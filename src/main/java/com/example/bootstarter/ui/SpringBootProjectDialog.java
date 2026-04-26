package com.example.bootstarter.ui;

import com.example.bootstarter.model.SpringBootProjectRequest;
import com.example.bootstarter.service.SpringInitializrMetadata;
import com.example.bootstarter.service.SpringInitializrMetadataService;
import com.example.bootstarter.util.NotificationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpringBootProjectDialog extends DialogWrapper {
    private static final String SERVER_DEFAULT_BOOT_LABEL = "(Server Default)";
    private static final Pattern JAVA_PACKAGE_PATTERN =
            Pattern.compile("^[a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*$");

    private static final List<String> FALLBACK_PACKAGING = List.of("jar", "war");
    private static final List<String> FALLBACK_JAVA = List.of("17", "21");
    private static final List<String> FALLBACK_BOOT = List.of(SERVER_DEFAULT_BOOT_LABEL, "3.5.0", "3.4.6");
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

    private final Map<String, String> dependencyLabelToId = new LinkedHashMap<>();
    private final JBList<String> dependencyList;
    private final Project project;
    private final SpringInitializrMetadataService metadataService = new SpringInitializrMetadataService();

    public SpringBootProjectDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("BootStarter - Initialize Your Spring Boot Project");

        applyFallbackOptions();

        dependencyList = new JBList<>(dependencyLabelToId.keySet().toArray(new String[0]));
        dependencyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dependencyList.setVisibleRowCount(6);

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

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel("Dependencies"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        panel.add(new JBScrollPane(dependencyList), gbc);

        panel.setPreferredSize(new Dimension(520, 420));
        return panel;
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
        req.setJavaVersion((String) javaVersionBox.getSelectedItem());
        req.setSpringBootVersion(normalizeBlank((String) bootVersionBox.getSelectedItem()));
        req.setType((String) projectTypeBox.getSelectedItem());
        req.setLanguage((String) languageBox.getSelectedItem());

        List<String> deps = dependencyList.getSelectedValuesList().stream()
                .map(dependencyLabelToId::get)
                .collect(Collectors.toList());
        req.setDependencies(deps);

        return req;
    }

    private void applyFallbackOptions() {
        setComboItems(packagingBox, FALLBACK_PACKAGING, "jar");
        setComboItems(javaVersionBox, FALLBACK_JAVA, "17");
        setComboItems(bootVersionBox, FALLBACK_BOOT, SERVER_DEFAULT_BOOT_LABEL);
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

    private void loadMetadataAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                SpringInitializrMetadata metadata = metadataService.fetchMetadata();
                ApplicationManager.getApplication().invokeLater(() -> applyMetadata(metadata));
            } catch (Exception ignored) {
                if (project != null) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            NotificationUtil.info(project, "Using built-in Spring Initializr defaults (metadata unavailable).")
                    );
                }
            }
        });
    }

    private void applyMetadata(SpringInitializrMetadata metadata) {
        setComboItems(packagingBox, fallbackIfEmpty(metadata.getPackagings(), FALLBACK_PACKAGING), "jar");
        setComboItems(javaVersionBox, fallbackIfEmpty(metadata.getJavaVersions(), FALLBACK_JAVA), "17");
        setComboItems(projectTypeBox, fallbackIfEmpty(metadata.getTypes(), FALLBACK_TYPES), "maven-project");
        setComboItems(languageBox, filterLanguage(fallbackIfEmpty(metadata.getLanguages(), FALLBACK_LANGUAGE)), "java");

        List<String> boot = new ArrayList<>();
        boot.add(SERVER_DEFAULT_BOOT_LABEL);
        boot.addAll(metadata.getBootVersions());
        setComboItems(bootVersionBox, fallbackIfEmpty(boot, FALLBACK_BOOT), SERVER_DEFAULT_BOOT_LABEL);

        if (!metadata.getDependencyLabelToId().isEmpty()) {
            dependencyLabelToId.clear();
            dependencyLabelToId.putAll(metadata.getDependencyLabelToId());
            dependencyList.setListData(dependencyLabelToId.keySet().toArray(new String[0]));
        }
    }

    private List<String> filterLanguage(List<String> values) {
        List<String> javaOnly = values.stream()
                .filter(v -> "java".equalsIgnoreCase(v))
                .collect(Collectors.toList());
        if (!javaOnly.isEmpty()) return javaOnly;
        return List.of("java");
    }

    private List<String> fallbackIfEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
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

    private String normalizeBlank(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (SERVER_DEFAULT_BOOT_LABEL.equals(trimmed)) return null;
        return trimmed.isEmpty() ? null : trimmed;
    }
}
