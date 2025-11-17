package com.aicareer.hh.hhapi;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class HhApiClient {
    private static final String BASE = "https://api.hh.ru";

    private final HttpClient http;
    private final String userAgent;
    private final String token;

    public HhApiClient(String userAgent, String token) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        this.userAgent = (userAgent == null || userAgent.isBlank())
                ? "ai-career-bot/1.0 (+contact@example.com)"
                : userAgent;
        this.token = token;
    }

    public HttpResponse<String> get(String path, Map<String, String> qs) throws Exception {
        URI uri = URI.create(BASE + path + buildQuery(qs));
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .GET();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpRequest req = b.build();

        for (int attempt = 0; attempt < 2; attempt++) {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            logRateHeaders(resp);
            int sc = resp.statusCode();
            if (sc == 200) return resp;

            if (sc == 429 && attempt == 0) {
                long wait = parseRetryAfter(resp).orElse(2L);
                Thread.sleep(wait * 1000L);
                continue;
            }
            if (sc == 401 && attempt == 0) {
                throw new RuntimeException("Unauthorized (401). Access token is invalid/expired.");
            }
            throw new RuntimeException("HH GET " + uri + " failed: " + sc + " body=" + resp.body());
        }
        throw new RuntimeException("Unexpected retry loop exit");
    }

    public HttpResponse<String> getVacancies(Map<String, String> qs) throws Exception {
        return get("/vacancies", qs);
    }

    private static String buildQuery(Map<String, String> qs) {
        if (qs == null || qs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (var e : qs.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private void logRateHeaders(HttpResponse<?> resp) {
        resp.headers().map().forEach((k, v) -> {
            String lk = k.toLowerCase();
            if (lk.contains("limit") || lk.contains("retry-after") || lk.contains("ratelimit")) {
                System.out.println("[HDR] " + k + " = " + v);
            }
        });
    }

    private Optional<Long> parseRetryAfter(HttpResponse<?> resp) {
        try {
            return resp.headers().firstValue("Retry-After").map(Long::parseLong);
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }
}
