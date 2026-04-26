package com.example.bootstarter.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SpringInitializrClient {
    private static final String METADATA_URL = "https://start.spring.io/metadata/client";
    private static final String DEPENDENCIES_URL = "https://start.spring.io/dependencies";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public byte[] downloadStarterZip(String requestUrl) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(requestUrl);
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new SpringInitializrHttpException(response.statusCode(), body);
        }
        return response.body();
    }

    public String fetchMetadataJson() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(METADATA_URL);
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new SpringInitializrHttpException(response.statusCode(), body);
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    public String fetchDependenciesJson() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(DEPENDENCIES_URL);
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new SpringInitializrHttpException(response.statusCode(), body);
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private HttpResponse<byte[]> sendForBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }
}
