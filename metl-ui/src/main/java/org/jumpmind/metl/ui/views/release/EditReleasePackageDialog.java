package org.jumpmind.metl.ui.views.release;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.ReleasePackageProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditReleasePackageDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;
    
    ReleasePackage releasePackage;
    
    ApplicationContext context;
    
    IReleasePackageListener listener;
    
    IConfigurationService configurationService;
    
    TextField nameField;
    
    TextField versionLabelField;
    
    DateField releaseDateField;
    
    List<CheckBox> projectCheckboxes = new ArrayList<CheckBox>();
    
    Map<String, OptionGroup> projectVersionOptionGroups = new HashMap<String, OptionGroup>();

    public EditReleasePackageDialog(ReleasePackage releasePackage, ApplicationContext context, IReleasePackageListener listener) {
        super(releasePackage == null ? "New Release Package" : "Edit Release Package");
        this.releasePackage = releasePackage;
        this.listener = listener;
        this.context = context;
        this.configurationService = context.getConfigurationService();
        
        if (this.releasePackage == null) {
            this.releasePackage = new ReleasePackage();
            this.releasePackage.setId(UUID.randomUUID().toString());
        }
        
        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);
        
        VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSizeFull();
        vLayout.setMargin(true);
        Panel formPanel = new Panel();
        formPanel.setSizeFull();
        formPanel.setContent(buildEntryForm());
        vLayout.addComponent(formPanel);
        
        Panel projectPanel = new Panel();
        projectPanel.setSizeFull();
        projectPanel.setContent(buildProjectsAndVersions(this.releasePackage.getId()));
        vLayout.addComponent(projectPanel);        
        addComponent(vLayout,1);        
        addComponent(buildButtonBar());
        vLayout.setExpandRatio(formPanel,  0.3f);
        vLayout.setExpandRatio(projectPanel, 0.7f);
    }

    protected FormLayout buildEntryForm() {
        FormLayout form = new FormLayout();
        form.setMargin(true);
        nameField = new TextField("Name");
        nameField.setValue(releasePackage.getName() != null ? releasePackage.getName() : "");
        form.addComponent(nameField);
        versionLabelField = new TextField("Version");
        versionLabelField.setValue(releasePackage.getVersionLabel() != null ? releasePackage.getVersionLabel() : "");
        form.addComponent(versionLabelField);
        releaseDateField = new DateField("Release Date");
        releaseDateField.setValue(releasePackage.getReleaseDate() != null ? releasePackage.getReleaseDate() : null);
        form.addComponent(releaseDateField);
        return form;
    }

    protected Panel buildProjectsAndVersions(String releasePackageId) {

        Panel projectsAndVersionsPanel = new Panel("Projects and Branches");
        projectsAndVersionsPanel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        projectsAndVersionsPanel.setSizeFull();

        VerticalLayout projectLayout = new VerticalLayout();
        projectLayout.setMargin(true);
        projectCheckboxes.clear();
        projectVersionOptionGroups.clear();
        List<Project> projects = configurationService.findProjects();
        for (Project project : projects) {
            //first allow the user to select or unselect a project
            CheckBox checkBox = new CheckBox(project.getName());
            checkBox.setData(project.getId());
            projectLayout.addComponent(checkBox);
            //now put the project version options for each project            
            OptionGroup optionGroup = new OptionGroup();            
            List<ReleasePackageProjectVersion> rppvs = configurationService.findReleasePackageProjectVersions(releasePackageId);
            Set<String> projectVersionsInReleasePackage = getListOfProjectVersionsInReleasePackages(rppvs);
            for (ProjectVersion projectVersion : project.getProjectVersions()) {
                if (!projectVersion.getVersionType().equalsIgnoreCase(ProjectVersion.VersionType.RELEASE.toString())) {
                    optionGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
                    optionGroup.addItem(projectVersion.getId());
                    optionGroup.setEnabled(false);
                    optionGroup.setItemCaption(projectVersion.getId(),projectVersion.getName());
                    if (projectVersionsInReleasePackage.contains(projectVersion.getId())) {
                        checkBox.setValue(true);
                        optionGroup.select(projectVersion.getId());
                        optionGroup.setEnabled(true);
                    }
                    projectVersionOptionGroups.put(project.getId(), optionGroup);
                    projectLayout.addComponent(optionGroup);
                }
            }
            projectCheckboxes.add(checkBox);
            checkBox.addValueChangeListener(e -> projectSelectionListener(e));
        }
        
        projectsAndVersionsPanel.setContent(projectLayout);
        return projectsAndVersionsPanel;
    }
    
    protected Set<String> getListOfProjectVersionsInReleasePackages(List<ReleasePackageProjectVersion> rppvs) {
        Set<String> projectVersionsInRelease = new HashSet<String>();
        for (ReleasePackageProjectVersion rppv : rppvs) {
            projectVersionsInRelease.add(rppv.getProjectVersionId());
        }
        return projectVersionsInRelease;
    }
    
    protected void projectSelectionListener(ValueChangeEvent event) {
        CheckBox checkbox = (CheckBox) event.getProperty();
        String projectId = (String) checkbox.getData();
        OptionGroup optionGroup = projectVersionOptionGroups.get(projectId);
        if (checkbox.getValue() == false) {
            optionGroup.clear();
            optionGroup.setEnabled(false);
        } else {
            @SuppressWarnings("unchecked")
            Collection<String> projectVersionIds = (Collection<String>) optionGroup.getItemIds();
            Iterator<String> itr = projectVersionIds.iterator();
            optionGroup.select(itr.next());
            optionGroup.setEnabled(true);
        }
    }
    
    protected HorizontalLayout buildButtonBar() {
        Button cancelButton = new Button("Cancel", e->cancel());
        Button saveButton = new Button("Save", e->save());
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        saveButton.setClickShortcut(KeyCode.ENTER);
        return buildButtonFooter(cancelButton, saveButton);        
    }
        
    protected void save() {
        releasePackage.setName(nameField.getValue());
        releasePackage.setVersionLabel(versionLabelField.getValue());
        releasePackage.setReleaseDate(releaseDateField.getValue());
        configurationService.save(releasePackage);
        
        configurationService.deleteReleasePackageProjectVersionsForReleasePackage(releasePackage.getId());
        for (CheckBox projectCheckbox : projectCheckboxes) {
            if (projectCheckbox.getValue() == true) {
                String projectId = (String) projectCheckbox.getData();
                OptionGroup optionGroup = projectVersionOptionGroups.get(projectId);
                String projectVersionId = (String) optionGroup.getValue();
                ReleasePackageProjectVersion rppv = new ReleasePackageProjectVersion(releasePackage.getId(), projectVersionId);
                configurationService.save(rppv);
            }            
        }
        
        listener.updated(releasePackage);
        close();
    }
    
    protected void cancel() {
        close();
    }
}
