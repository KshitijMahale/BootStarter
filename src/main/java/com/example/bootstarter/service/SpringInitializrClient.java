package com.example.bootstarter.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class SpringInitializrClient {
    private static final String INITIALIZR_BASE_URL = "https://start.spring.io";
    private static final String METADATA_URL = "https://start.spring.io/metadata/client";
    private static final String METADATA_V21_FALLBACK_URL = "https://start.spring.io/metadata/client?format=v2.1";
    private static final String DEPENDENCIES_URL = "https://start.spring.io/dependencies";
    private static final String INITIALIZR_METADATA_ACCEPT = "application/vnd.initializr.v2.1+json";
    private static final String JSON_ACCEPT = "application/json";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public byte[] downloadStarterZip(String requestUrl) throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(requestUrl, null);
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new SpringInitializrHttpException(response.statusCode(), body);
        }
        return response.body();
    }

    public String fetchMetadataJson() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(METADATA_URL, INITIALIZR_METADATA_ACCEPT);
        if (response.statusCode() != 200) {
            return fetchMetadataJsonFallback();
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    public String fetchMetadataJsonFallback() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(METADATA_V21_FALLBACK_URL, JSON_ACCEPT);
        if (response.statusCode() != 200) {
            response = sendForBytes(METADATA_URL, JSON_ACCEPT);
        }
        if (response.statusCode() != 200) {
            response = sendForBytes(INITIALIZR_BASE_URL, JSON_ACCEPT);
        }
        if (response.statusCode() != 200) {
            response = sendForBytes(INITIALIZR_BASE_URL, INITIALIZR_METADATA_ACCEPT);
            if (response.statusCode() != 200) {
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new SpringInitializrHttpException(response.statusCode(), body);
            }
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    public String fetchDependenciesJson() throws IOException, InterruptedException {
        HttpResponse<byte[]> response = sendForBytes(DEPENDENCIES_URL, JSON_ACCEPT);
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new SpringInitializrHttpException(response.statusCode(), body);
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private HttpResponse<byte[]> sendForBytes(String url, String acceptHeader) throws IOException, InterruptedException {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "BootStarter-IntelliJ")
                .header("Accept-Encoding", "identity")
                .GET();
        if (acceptHeader != null && !acceptHeader.isBlank()) {
            reqBuilder.header("Accept", acceptHeader);
        }
        HttpRequest req = reqBuilder.build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }
}
