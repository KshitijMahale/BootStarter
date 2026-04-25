package com.example.bootstarter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpringBootProjectRequest {
    private String groupId;
    private String artifactId;
    private String name;
    private String packageName;
    private String packaging;
    private String javaVersion;
    private String springBootVersion;
    private String type;
    private String language;
    private List<String> dependencies = new ArrayList<>();

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getPackaging() { return packaging; }
    public void setPackaging(String packaging) { this.packaging = packaging; }

    public String getJavaVersion() { return javaVersion; }
    public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

    public String getSpringBootVersion() { return springBootVersion; }
    public void setSpringBootVersion(String springBootVersion) { this.springBootVersion = springBootVersion; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) {
        this.dependencies = Objects.requireNonNullElseGet(dependencies, ArrayList::new);
    }
}

