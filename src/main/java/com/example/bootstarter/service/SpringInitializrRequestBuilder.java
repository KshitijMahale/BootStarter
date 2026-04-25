package com.example.bootstarter.service;

import com.example.bootstarter.model.SpringBootProjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SpringInitializrRequestBuilder {
    private static final String BASE_URL = "https://start.spring.io/starter.zip";

    public String buildUrl(SpringBootProjectRequest req) {
        List<String> params = new ArrayList<>();
        add(params, "type", req.getType());
        add(params, "language", req.getLanguage());
        add(params, "bootVersion", req.getSpringBootVersion());
        add(params, "groupId", req.getGroupId());
        add(params, "artifactId", req.getArtifactId());
        add(params, "name", req.getName());
        add(params, "packageName", req.getPackageName());
        add(params, "packaging", req.getPackaging());
        add(params, "javaVersion", req.getJavaVersion());
        if (req.getDependencies() != null && !req.getDependencies().isEmpty()) {
            add(params, "dependencies", String.join(",", req.getDependencies()));
        }
        return BASE_URL + "?" + String.join("&", params);
    }

    private void add(List<String> params, String key, String value) {
        if (value == null || value.isBlank()) return;
        params.add(encode(key) + "=" + encode(value));
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

