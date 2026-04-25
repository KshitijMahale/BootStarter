package com.example.bootstarter.ui;

import com.example.bootstarter.model.SpringBootProjectRequest;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpringBootProjectDialog extends DialogWrapper {

    private static final Pattern JAVA_PACKAGE_PATTERN =
            Pattern.compile("^[a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*)*$");

    private final JTextField groupIdField = new JTextField("com.example");
    private final JTextField artifactIdField = new JTextField("demo");
    private final JTextField nameField = new JTextField("demo");
    private final JTextField packageNameField = new JTextField("com.example.demo");

    private final JComboBox<String> packagingBox = new JComboBox<>(new String[]{"jar", "war"});
    private final JComboBox<String> javaVersionBox = new JComboBox<>(new String[]{"17", "21"});
    private final JComboBox<String> bootVersionBox = new JComboBox<>(new String[]{"3.3.5", "3.2.10", "3.1.12"});
    private final JComboBox<String> projectTypeBox = new JComboBox<>(new String[]{"maven-project", "gradle-project"});
    private final JComboBox<String> languageBox = new JComboBox<>(new String[]{"java"});

    private final Map<String, String> dependencyLabelToId = new LinkedHashMap<>();
    private final JBList<String> dependencyList;

    public SpringBootProjectDialog(@Nullable Project project) {
        super(project);
        setTitle("Spring Boot Project (Community)");

        dependencyLabelToId.put("Spring Web", "web");
        dependencyLabelToId.put("Spring Data JPA", "data-jpa");
        dependencyLabelToId.put("Lombok", "lombok");
        dependencyLabelToId.put("Validation", "validation");
        dependencyLabelToId.put("Actuator", "actuator");
        dependencyLabelToId.put("Security", "security");
        dependencyLabelToId.put("DevTools", "devtools");

        dependencyList = new JBList<>(dependencyLabelToId.keySet().toArray(new String[0]));
        dependencyList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        dependencyList.setVisibleRowCount(6);

        init();
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
        req.setSpringBootVersion((String) bootVersionBox.getSelectedItem());
        req.setType((String) projectTypeBox.getSelectedItem());
        req.setLanguage((String) languageBox.getSelectedItem());

        List<String> deps = dependencyList.getSelectedValuesList().stream()
                .map(dependencyLabelToId::get)
                .collect(Collectors.toList());
        req.setDependencies(deps);

        return req;
    }
}

