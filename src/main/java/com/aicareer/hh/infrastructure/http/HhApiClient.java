package com.aicareer.hh.infrastructure.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HhApiClient {
    private final String userAgent;
    private final String token;
    private final HttpClient http = HttpClient.newHttpClient();

    public HhApiClient(String userAgent, String token) {
        this.userAgent = userAgent;
        this.token = token;
    }

    public String get(String url) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("User-Agent", userAgent)
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + r.statusCode() + " GET " + url);
        }
        return r.body();
    }
}
