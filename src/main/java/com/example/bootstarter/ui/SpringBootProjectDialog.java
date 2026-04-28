package com.example.bootstarter.ui;

import com.example.bootstarter.model.SpringBootProjectRequest;
import com.example.bootstarter.service.SpringInitializrMetadata;
import com.example.bootstarter.service.SpringInitializrMetadataService;
import com.example.bootstarter.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpringBootProjectDialog extends DialogWrapper {
    private static final String SERVER_DEFAULT_LABEL = "(Server Default)";
    private static final Pattern JAVA_PACKAGE_PATTERN =
            Pattern.compile("^[a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*$");

    private static final List<String> FALLBACK_PACKAGING = List.of("jar", "war");
    private static final List<String> FALLBACK_JAVA = List.of(
            SERVER_DEFAULT_LABEL,
            "26",
            "25",
            "21",
            "17"
    );
    private static final List<String> FALLBACK_BOOT = List.of(
            SERVER_DEFAULT_LABEL,
            "4.1.0.BUILD-SNAPSHOT",
            "4.1.0.RC1",
            "4.0.6.RELEASE",
            "3.5.14.RELEASE"
    );
    private static final List<String> FALLBACK_TYPES = List.of("maven-project", "gradle-project");
    private static final List<String> FALLBACK_LANGUAGE = List.of("java");

    private final JTextField groupIdField = new JTextField("com.example");
    private final JTextField artifactIdField = new JTextField("demo");
    private final JTextField nameField = new JTextField("demo");
    private final JTextField packageNameField = new JTextField("com.example.demo");

    private final JComboBox<String> packagingBox = new JComboBox<>();
    private final JComboBox<String> javaVersionBox = new JComboBox<>();
    private final JComboBox<String> bootVersionBox = new JComboBox<>();
    private final JComboBox<String> projectTypeBox = new JComboBox<>();
    private final JComboBox<String> languageBox = new JComboBox<>();

    private final JTextField dependencySearchField = new JTextField();
    private final JTextField customDependencyIdsField = new JTextField();
    private final JPanel dependencyCheckboxPanel = new JPanel();

    private final Map<String, String> dependencyLabelToId = new LinkedHashMap<>();
    private final Set<String> selectedDependencyIds = new LinkedHashSet<>();
    private final Project project;
    private final SpringInitializrMetadataService metadataService = new SpringInitializrMetadataService();

    public SpringBootProjectDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("BootStarter - Initialize Your Spring Boot Project");

        applyFallbackOptions();
        setupDependencyUi();

        init();
        loadMetadataAsync();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int y = 0;
        addRow(panel, gbc, y++, "Group ID", groupIdField);
        addRow(panel, gbc, y++, "Artifact ID", artifactIdField);
        addRow(panel, gbc, y++, "Name", nameField);
        addRow(panel, gbc, y++, "Package Name", packageNameField);
        addRow(panel, gbc, y++, "Packaging", packagingBox);
        addRow(panel, gbc, y++, "Java Version", javaVersionBox);
        addRow(panel, gbc, y++, "Spring Boot Version", bootVersionBox);
        addRow(panel, gbc, y++, "Project Type", projectTypeBox);
        addRow(panel, gbc, y++, "Language", languageBox);
        addRow(panel, gbc, y++, "Find Dependency", dependencySearchField);

        JPanel dependencyActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton selectAllButton = new JButton("Select All");
        JButton clearButton = new JButton("Clear");
        selectAllButton.addActionListener(e -> {
            selectedDependencyIds.clear();
            selectedDependencyIds.addAll(dependencyLabelToId.values());
            renderDependencyCheckboxes();
        });
        clearButton.addActionListener(e -> {
            selectedDependencyIds.clear();
            renderDependencyCheckboxes();
        });
        dependencyActions.add(selectAllButton);
        dependencyActions.add(clearButton);
        addRow(panel, gbc, y++, "Dependency Actions", dependencyActions);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel("Dependencies"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JBScrollPane(dependencyCheckboxPanel), gbc);

        addRow(panel, gbc, ++y, "Custom IDs", customIdsFieldWithHint(customDependencyIdsField));

        panel.setPreferredSize(new Dimension(620, 520));
        return panel;
    }

    private JComponent customIdsFieldWithHint(JTextField textField) {
        String hint = "Comma-separated ids, e.g. redis,amqp,mysql";
        textField.setToolTipText(hint);
        JPanel wrapper = new JPanel(new BorderLayout(0, 4));
        wrapper.add(textField, BorderLayout.CENTER);
        JLabel note = new JLabel(hint);
        note.setForeground(UIManager.getColor("Label.disabledForeground"));
        wrapper.add(note, BorderLayout.SOUTH);
        return wrapper;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(comp, gbc);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (groupIdField.getText().isBlank()) return new ValidationInfo("Group ID is required.", groupIdField);
        if (artifactIdField.getText().isBlank()) return new ValidationInfo("Artifact ID is required.", artifactIdField);
        if (nameField.getText().isBlank()) return new ValidationInfo("Name is required.", nameField);
        String pkg = packageNameField.getText().trim();
        if (pkg.isEmpty()) return new ValidationInfo("Package Name is required.", packageNameField);
        if (!JAVA_PACKAGE_PATTERN.matcher(pkg).matches()) {
            return new ValidationInfo("Invalid Java package format.", packageNameField);
        }
        return null;
    }

    public SpringBootProjectRequest toRequest() {
        SpringBootProjectRequest req = new SpringBootProjectRequest();
        req.setGroupId(groupIdField.getText().trim());
        req.setArtifactId(artifactIdField.getText().trim());
        req.setName(nameField.getText().trim());
        req.setPackageName(packageNameField.getText().trim());
        req.setPackaging((String) packagingBox.getSelectedItem());
        req.setJavaVersion(normalizeServerDefault((String) javaVersionBox.getSelectedItem()));
        req.setSpringBootVersion(normalizeServerDefault((String) bootVersionBox.getSelectedItem()));
        req.setType((String) projectTypeBox.getSelectedItem());
        req.setLanguage((String) languageBox.getSelectedItem());

        Set<String> deps = new LinkedHashSet<>(selectedDependencyIds);
        deps.addAll(parseCustomDependencyIds(customDependencyIdsField.getText()));
        req.setDependencies(new ArrayList<>(deps));

        return req;
    }

    private void applyFallbackOptions() {
        setComboItems(packagingBox, FALLBACK_PACKAGING, "jar");
        setComboItems(javaVersionBox, FALLBACK_JAVA, SERVER_DEFAULT_LABEL);
        setComboItems(bootVersionBox, FALLBACK_BOOT, SERVER_DEFAULT_LABEL);
        setComboItems(projectTypeBox, FALLBACK_TYPES, "maven-project");
        setComboItems(languageBox, FALLBACK_LANGUAGE, "java");

        dependencyLabelToId.clear();
        dependencyLabelToId.put("Spring Web", "web");
        dependencyLabelToId.put("Spring Data JPA", "data-jpa");
        dependencyLabelToId.put("Lombok", "lombok");
        dependencyLabelToId.put("Validation", "validation");
        dependencyLabelToId.put("Actuator", "actuator");
        dependencyLabelToId.put("Security", "security");
        dependencyLabelToId.put("DevTools", "devtools");
    }

    private void setupDependencyUi() {
        dependencyCheckboxPanel.setLayout(new BoxLayout(dependencyCheckboxPanel, BoxLayout.Y_AXIS));
        dependencySearchField.setToolTipText("Search by dependency name or id");
        dependencySearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { renderDependencyCheckboxes(); }

            @Override
            public void removeUpdate(DocumentEvent e) { renderDependencyCheckboxes(); }

            @Override
            public void changedUpdate(DocumentEvent e) { renderDependencyCheckboxes(); }
        });
        renderDependencyCheckboxes();
    }

    private void renderDependencyCheckboxes() {
        String filter = dependencySearchField.getText() == null
                ? ""
                : dependencySearchField.getText().trim().toLowerCase();

        dependencyCheckboxPanel.removeAll();
        int count = 0;
        for (Map.Entry<String, String> entry : dependencyLabelToId.entrySet()) {
            String label = entry.getKey();
            String id = entry.getValue();
            if (!matchesFilter(filter, label, id)) {
                continue;
            }

            JCheckBox checkBox = new JCheckBox(label);
            checkBox.setToolTipText("Dependency id: " + id);
            checkBox.setSelected(selectedDependencyIds.contains(id));
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {
                    selectedDependencyIds.add(id);
                } else {
                    selectedDependencyIds.remove(id);
                }
            });
            dependencyCheckboxPanel.add(checkBox);
            count++;
        }

        if (count == 0) {
            JLabel empty = new JLabel("No dependencies found for current filter.");
            empty.setForeground(UIManager.getColor("Label.disabledForeground"));
            dependencyCheckboxPanel.add(empty);
        }

        dependencyCheckboxPanel.revalidate();
        dependencyCheckboxPanel.repaint();
    }

    private boolean matchesFilter(String filter, String label, String id) {
        if (filter.isEmpty()) return true;
        return label.toLowerCase().contains(filter) || id.toLowerCase().contains(filter);
    }

    private void loadMetadataAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (isLifecycleDisposed()) {
                    return;
                }
                SpringInitializrMetadata metadata = metadataService.fetchMetadata();
                if (isLifecycleDisposed()) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (isLifecycleDisposed()) return;
                    if (metadata.getJavaVersions().isEmpty() || metadata.getBootVersions().isEmpty()) {
                        NotificationUtil.error(project,
                                "Spring Initializr metadata did not include Java/Spring Boot versions. " +
                                        "Using fallback versions.");
                    }
                    applyMetadata(metadata);
                });
            } catch (ProcessCanceledException ignored) {
                // Ignore cancellation during IDE/project shutdown.
            } catch (Exception e) {
                if (!isLifecycleDisposed()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            NotificationUtil.error(project,
                                    "Failed to load Spring Initializr metadata: " +
                                            (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()))
                    );
                }
            }
        });
    }

    private boolean isLifecycleDisposed() {
        var app = ApplicationManager.getApplication();
        return app == null || app.isDisposed() || isDisposed() || (project != null && project.isDisposed());
    }

    private void applyMetadata(SpringInitializrMetadata metadata) {
        String currentPackaging = (String) packagingBox.getSelectedItem();
        String currentJavaVersion = (String) javaVersionBox.getSelectedItem();
        String currentProjectType = (String) projectTypeBox.getSelectedItem();
        String currentLanguage = (String) languageBox.getSelectedItem();
        String currentBootVersion = (String) bootVersionBox.getSelectedItem();
        Set<String> previousSelectedDependencyIds = new LinkedHashSet<>(selectedDependencyIds);

        setComboItems(packagingBox, fallbackIfEmpty(metadata.getPackagings(), FALLBACK_PACKAGING),
                firstNonBlank(currentPackaging, metadata.getDefaultPackaging(), "jar"));

        List<String> javaOptions = withServerDefaultPrefix(metadata.getJavaVersions(), FALLBACK_JAVA);
        setComboItems(javaVersionBox, javaOptions,
                firstNonBlank(nonServerDefault(currentJavaVersion), metadata.getDefaultJavaVersion(), SERVER_DEFAULT_LABEL));

        setComboItems(projectTypeBox, fallbackIfEmpty(metadata.getTypes(), FALLBACK_TYPES),
                firstNonBlank(currentProjectType, metadata.getDefaultType(), "maven-project"));
        setComboItems(languageBox, fallbackIfEmpty(metadata.getLanguages(), FALLBACK_LANGUAGE),
                firstNonBlank(currentLanguage, metadata.getDefaultLanguage(), "java"));

        List<String> boot = withServerDefaultPrefix(metadata.getBootVersions(), FALLBACK_BOOT);
        setComboItems(bootVersionBox, fallbackIfEmpty(boot, FALLBACK_BOOT),
                firstNonBlank(nonServerDefault(currentBootVersion), metadata.getDefaultBootVersion(), SERVER_DEFAULT_LABEL));

        if (!metadata.getDependencyLabelToId().isEmpty()) {
            dependencyLabelToId.clear();
            dependencyLabelToId.putAll(metadata.getDependencyLabelToId());
            selectedDependencyIds.clear();
            Set<String> availableIds = new LinkedHashSet<>(dependencyLabelToId.values());
            for (String id : previousSelectedDependencyIds) {
                if (availableIds.contains(id)) {
                    selectedDependencyIds.add(id);
                }
            }
            renderDependencyCheckboxes();
        }
    }

    private List<String> parseCustomDependencyIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private List<String> fallbackIfEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private List<String> withServerDefaultPrefix(List<String> values, List<String> fallback) {
        List<String> normalized = fallbackIfEmpty(values, fallback);
        List<String> result = new ArrayList<>();
        result.add(SERVER_DEFAULT_LABEL);
        for (String value : normalized) {
            if (value == null || value.isBlank() || SERVER_DEFAULT_LABEL.equals(value)) {
                continue;
            }
            result.add(value);
        }
        return result;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void setComboItems(JComboBox<String> comboBox, List<String> values, String preferredValue) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(values.toArray(new String[0]));
        comboBox.setModel(model);
        if (preferredValue != null && values.contains(preferredValue)) {
            comboBox.setSelectedItem(preferredValue);
        } else if (!values.isEmpty()) {
            comboBox.setSelectedIndex(0);
        }
    }

    private String normalizeServerDefault(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (SERVER_DEFAULT_LABEL.equals(trimmed)) return null;
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nonServerDefault(String value) {
        if (value == null) {
            return null;
        }
        return SERVER_DEFAULT_LABEL.equals(value.trim()) ? null : value;
    }
}
