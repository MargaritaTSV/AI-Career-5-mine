package com.aicareer.hh.infrastructure.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HhApiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String userAgent;   // обязателен для HH
    private final String token;       // опционально

    public HhApiClient(String userAgent, String token) {
        this.userAgent = userAgent;
        this.token = token;
    }

    public String get(String url) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .header("HH-User-Agent", userAgent);

        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> resp =
                http.send(b.build(), HttpResponse.BodyHandlers.ofString());

        int code = resp.statusCode();
        if (code >= 400) {
            throw new RuntimeException("HH " + code + ": " + resp.body());
        }
        return resp.body();
    }
}
