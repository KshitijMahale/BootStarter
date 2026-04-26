package com.example.bootstarter.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SpringInitializrClient {
    private static final String METADATA_URL = "https://start.spring.io/metadata/client";

    private final HttpClient client = HttpClient.newHttpClient();

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

    private HttpResponse<byte[]> sendForBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }
}
