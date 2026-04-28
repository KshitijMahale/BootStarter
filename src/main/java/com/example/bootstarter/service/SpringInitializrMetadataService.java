package com.example.bootstarter.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpringInitializrMetadataService {

    private final SpringInitializrClient client;

    public SpringInitializrMetadataService() {
        this(new SpringInitializrClient());
    }

    SpringInitializrMetadataService(SpringInitializrClient client) {
        this.client = client;
    }

    public SpringInitializrMetadata fetchMetadata() throws IOException, InterruptedException {
        SpringInitializrMetadata metadata = null;
        try {
            metadata = fetchBestMetadata();
            if (!metadata.getDependencyLabelToId().isEmpty()) {
                return metadata;
            }
            try {
                Map<String, String> extraDependencies = parseDependencyCatalog(client.fetchDependenciesJson());
                return withDependencies(metadata, extraDependencies);
            } catch (Exception dependencyCatalogError) {
                // Keep version/type metadata even if dependency catalog endpoint is unavailable.
                return metadata;
            }
        } catch (InterruptedException interruptedException) {
            throw interruptedException;
        } catch (Exception metadataError) {
            if (metadata != null) {
                return metadata;
            }
            Map<String, String> dependencies = parseDependencyCatalog(client.fetchDependenciesJson());
            return new SpringInitializrMetadata(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    dependencies,
                    new SpringInitializrMetadata.Defaults(null, null, null, null, null)
            );
        }
    }

    private SpringInitializrMetadata fetchBestMetadata() throws IOException, InterruptedException {
        SpringInitializrMetadata best = null;
        Exception lastParseError = null;

        try {
            SpringInitializrMetadata primary = parseMetadata(client.fetchMetadataJson());
            best = pickMoreComplete(best, primary);
            if (hasRequiredVersionOptions(primary)) {
                return primary;
            }
        } catch (Exception e) {
            lastParseError = e;
        }

        try {
            SpringInitializrMetadata fallback = parseMetadata(client.fetchMetadataJsonFallback());
            best = pickMoreComplete(best, fallback);
            if (hasRequiredVersionOptions(fallback)) {
                return fallback;
            }
        } catch (Exception e) {
            lastParseError = e;
        }

        if (best != null) {
            return best;
        }

        if (lastParseError instanceof IOException) {
            throw (IOException) lastParseError;
        }
        if (lastParseError instanceof InterruptedException) {
            throw (InterruptedException) lastParseError;
        }
        throw new IOException("Unable to parse Spring Initializr metadata.", lastParseError);
    }

    private SpringInitializrMetadata pickMoreComplete(SpringInitializrMetadata current,
                                                      SpringInitializrMetadata candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null) {
            return candidate;
        }
        int currentScore = completenessScore(current);
        int candidateScore = completenessScore(candidate);
        return candidateScore > currentScore ? candidate : current;
    }

    private int completenessScore(SpringInitializrMetadata metadata) {
        if (metadata == null) {
            return 0;
        }
        int score = 0;
        if (!metadata.getJavaVersions().isEmpty()) score += 2;
        if (!metadata.getBootVersions().isEmpty()) score += 2;
        if (!metadata.getTypes().isEmpty()) score += 1;
        if (!metadata.getPackagings().isEmpty()) score += 1;
        if (!metadata.getLanguages().isEmpty()) score += 1;
        if (!metadata.getDependencyLabelToId().isEmpty()) score += 1;
        return score;
    }

    private boolean hasRequiredVersionOptions(SpringInitializrMetadata metadata) {
        return metadata != null
                && !metadata.getJavaVersions().isEmpty()
                && !metadata.getBootVersions().isEmpty();
    }

    static SpringInitializrMetadata parseMetadata(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        List<String> types = extractOptionIds(root, "type");
        List<String> languages = extractOptionIds(root, "language");
        List<String> bootVersions = extractOptionIds(root, "bootVersion");
        List<String> javaVersions = extractOptionIds(root, "javaVersion");
        List<String> packagings = extractOptionIds(root, "packaging");
        Map<String, String> dependencies = extractDependencies(root);
        String defaultType = extractDefaultOptionId(root, "type");
        String defaultLanguage = extractDefaultOptionId(root, "language");
        String defaultBootVersion = extractDefaultOptionId(root, "bootVersion");
        String defaultJavaVersion = extractDefaultOptionId(root, "javaVersion");
        String defaultPackaging = extractDefaultOptionId(root, "packaging");

        SpringInitializrMetadata.Defaults defaults = new SpringInitializrMetadata.Defaults(
                defaultType,
                defaultLanguage,
                defaultBootVersion,
                defaultJavaVersion,
                defaultPackaging
        );

        return new SpringInitializrMetadata(
                types,
                languages,
                bootVersions,
                javaVersions,
                packagings,
                dependencies,
                defaults
        );
    }

    private static List<String> extractOptionIds(JsonObject root, String sectionName) {
        List<String> result = new ArrayList<>();
        JsonObject section = getObject(root, sectionName);
        if (section == null) return result;

        JsonArray values = getValuesArray(section);
        if (values == null) return result;

        for (JsonElement el : values) {
            if (el == null || el.isJsonNull()) {
                continue;
            }

            if (el.isJsonPrimitive()) {
                String id = el.getAsString();
                if (id != null && !id.isBlank()) {
                    result.add(id);
                }
                continue;
            }

            if (el.isJsonObject()) {
                JsonObject option = el.getAsJsonObject();
                String id = getString(option, "id");
                if (id != null && !id.isBlank()) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private static Map<String, String> extractDependencies(JsonObject root) {
        Map<String, String> result = new LinkedHashMap<>();
        JsonObject section = getObject(root, "dependencies");
        if (section == null) return result;

        JsonArray groups = getValuesArray(section);
        if (groups != null) {
            for (JsonElement groupEl : groups) {
                if (groupEl == null || !groupEl.isJsonObject()) {
                    continue;
                }
                JsonObject group = groupEl.getAsJsonObject();
                JsonArray dependencies = getValuesArray(group);
                if (dependencies == null) continue;

                for (JsonElement depEl : dependencies) {
                    if (depEl == null || !depEl.isJsonObject()) {
                        continue;
                    }
                    addDependency(result, depEl.getAsJsonObject(), null);
                }
            }
            return result;
        }

        // /dependencies endpoint: "dependencies" is a flat object keyed by id.
        for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            addDependency(result, entry.getValue().getAsJsonObject(), entry.getKey());
        }

        return result;
    }

    static Map<String, String> parseDependencyCatalog(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return extractDependencies(root);
    }

    private static SpringInitializrMetadata withDependencies(SpringInitializrMetadata metadata,
                                                             Map<String, String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return metadata;
        }
        return new SpringInitializrMetadata(
                metadata.getTypes(),
                metadata.getLanguages(),
                metadata.getBootVersions(),
                metadata.getJavaVersions(),
                metadata.getPackagings(),
                dependencies,
                new SpringInitializrMetadata.Defaults(
                        metadata.getDefaultType(),
                        metadata.getDefaultLanguage(),
                        metadata.getDefaultBootVersion(),
                        metadata.getDefaultJavaVersion(),
                        metadata.getDefaultPackaging()
                )
        );
    }

    private static String extractDefaultOptionId(JsonObject root, String sectionName) {
        JsonObject section = getObject(root, sectionName);
        if (section == null) return null;

        String sectionDefault = getString(section, "default");
        if (sectionDefault != null && !sectionDefault.isBlank()) {
            return sectionDefault;
        }

        JsonArray values = getValuesArray(section);
        if (values == null) return null;

        for (JsonElement element : values) {
            if (!element.isJsonObject()) continue;
            JsonObject option = element.getAsJsonObject();
            if (!isDefaultOption(option)) continue;
            String id = getString(option, "id");
            if (id != null && !id.isBlank()) {
                return id;
            }
        }
        return null;
    }

    private static boolean isDefaultOption(JsonObject option) {
        if (option == null || !option.has("default") || option.get("default").isJsonNull()) {
            return false;
        }
        return option.get("default").getAsBoolean();
    }

    private static void addDependency(Map<String, String> result, JsonObject dep, String fallbackId) {
        String id = getString(dep, "id");
        if (id == null || id.isBlank()) {
            id = fallbackId;
        }
        String name = getString(dep, "name");
        if (id == null || id.isBlank()) return;

        String label = (name == null || name.isBlank()) ? id : name;
        if (result.containsKey(label)) {
            label = label + " (" + id + ")";
        }
        result.put(label, id);
    }

    private static JsonObject getObject(JsonObject parent, String name) {
        if (parent == null || !parent.has(name) || !parent.get(name).isJsonObject()) {
            return null;
        }
        return parent.getAsJsonObject(name);
    }

    private static JsonArray getValuesArray(JsonObject parent) {
        if (parent == null || !parent.has("values") || !parent.get("values").isJsonArray()) {
            return null;
        }
        return parent.getAsJsonArray("values");
    }

    private static String getString(JsonObject parent, String name) {
        if (parent == null || !parent.has(name) || parent.get(name).isJsonNull()) {
            return null;
        }
        return parent.get(name).getAsString();
    }
}
