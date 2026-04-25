package com.example.bootstarter.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class SpringInitializrClient {
    private final HttpClient client = HttpClient.newHttpClient();

    public byte[] downloadStarterZip(String requestUrl) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            String body = new String(response.body(), StandardCharsets.UTF_8);
            throw new IOException("Spring Initializr request failed: HTTP " + response.statusCode() + " | " + body);
        }
        return response.body();
    }
}

