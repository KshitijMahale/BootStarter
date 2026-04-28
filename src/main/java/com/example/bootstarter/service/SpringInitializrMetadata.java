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
    private final Defaults defaults;

    public SpringInitializrMetadata(List<String> types,
                                    List<String> languages,
                                    List<String> bootVersions,
                                    List<String> javaVersions,
                                    List<String> packagings,
                                    Map<String, String> dependencyLabelToId,
                                    Defaults defaults) {
        this.types = new ArrayList<>(types);
        this.languages = new ArrayList<>(languages);
        this.bootVersions = new ArrayList<>(bootVersions);
        this.javaVersions = new ArrayList<>(javaVersions);
        this.packagings = new ArrayList<>(packagings);
        this.dependencyLabelToId = new LinkedHashMap<>(dependencyLabelToId);
        this.defaults = defaults == null ? new Defaults(null, null, null, null, null) : defaults;
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

    public String getDefaultType() {
        return defaults.type;
    }

    public String getDefaultLanguage() {
        return defaults.language;
    }

    public String getDefaultBootVersion() {
        return defaults.bootVersion;
    }

    public String getDefaultJavaVersion() {
        return defaults.javaVersion;
    }

    public String getDefaultPackaging() {
        return defaults.packaging;
    }

    public static final class Defaults {
        private final String type;
        private final String language;
        private final String bootVersion;
        private final String javaVersion;
        private final String packaging;

        public Defaults(String type,
                        String language,
                        String bootVersion,
                        String javaVersion,
                        String packaging) {
            this.type = type;
            this.language = language;
            this.bootVersion = bootVersion;
            this.javaVersion = javaVersion;
            this.packaging = packaging;
        }
    }
}
