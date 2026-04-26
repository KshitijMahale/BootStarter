package com.example.bootstarter.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpringInitializrMetadata {
    private final List<String> types;
    private final List<String> languages;
    private final List<String> bootVersions;
    private final List<String> javaVersions;
    private final List<String> packagings;
    private final Map<String, String> dependencyLabelToId;

    public SpringInitializrMetadata(List<String> types,
                                    List<String> languages,
                                    List<String> bootVersions,
                                    List<String> javaVersions,
                                    List<String> packagings,
                                    Map<String, String> dependencyLabelToId) {
        this.types = new ArrayList<>(types);
        this.languages = new ArrayList<>(languages);
        this.bootVersions = new ArrayList<>(bootVersions);
        this.javaVersions = new ArrayList<>(javaVersions);
        this.packagings = new ArrayList<>(packagings);
        this.dependencyLabelToId = new LinkedHashMap<>(dependencyLabelToId);
    }

    public List<String> getTypes() {
        return List.copyOf(types);
    }

    public List<String> getLanguages() {
        return List.copyOf(languages);
    }

    public List<String> getBootVersions() {
        return List.copyOf(bootVersions);
    }

    public List<String> getJavaVersions() {
        return List.copyOf(javaVersions);
    }

    public List<String> getPackagings() {
        return List.copyOf(packagings);
    }

    public Map<String, String> getDependencyLabelToId() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(dependencyLabelToId));
    }
}
