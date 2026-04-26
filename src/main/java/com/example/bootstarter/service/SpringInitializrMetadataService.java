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
        String json = client.fetchMetadataJson();
        return parseMetadata(json);
    }

    static SpringInitializrMetadata parseMetadata(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        List<String> types = extractOptionIds(root, "type");
        List<String> languages = extractOptionIds(root, "language");
        List<String> bootVersions = extractOptionIds(root, "bootVersion");
        List<String> javaVersions = extractOptionIds(root, "javaVersion");
        List<String> packagings = extractOptionIds(root, "packaging");
        Map<String, String> dependencies = extractDependencies(root);

        return new SpringInitializrMetadata(types, languages, bootVersions, javaVersions, packagings, dependencies);
    }

    private static List<String> extractOptionIds(JsonObject root, String sectionName) {
        List<String> result = new ArrayList<>();
        JsonObject section = getObject(root, sectionName);
        if (section == null) return result;

        JsonArray values = getValuesArray(section);
        if (values == null) return result;

        for (JsonElement el : values) {
            JsonObject option = el.getAsJsonObject();
            String id = getString(option, "id");
            if (id != null && !id.isBlank()) {
                result.add(id);
            }
        }
        return result;
    }

    private static Map<String, String> extractDependencies(JsonObject root) {
        Map<String, String> result = new LinkedHashMap<>();
        JsonObject section = getObject(root, "dependencies");
        if (section == null) return result;

        JsonArray groups = getValuesArray(section);
        if (groups == null) return result;

        for (JsonElement groupEl : groups) {
            JsonObject group = groupEl.getAsJsonObject();
            JsonArray dependencies = getValuesArray(group);
            if (dependencies == null) continue;

            for (JsonElement depEl : dependencies) {
                JsonObject dep = depEl.getAsJsonObject();
                String id = getString(dep, "id");
                String name = getString(dep, "name");
                if (id == null || id.isBlank()) continue;

                String label = (name == null || name.isBlank()) ? id : name;
                if (result.containsKey(label)) {
                    label = label + " (" + id + ")";
                }
                result.put(label, id);
            }
        }

        return result;
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
