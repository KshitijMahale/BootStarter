package com.example.bootstarter.service;

import java.io.IOException;

public class SpringInitializrHttpException extends IOException {
    private final int statusCode;
    private final String responseBody;

    public SpringInitializrHttpException(int statusCode, String responseBody) {
        super("Spring Initializr request failed: HTTP " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}

