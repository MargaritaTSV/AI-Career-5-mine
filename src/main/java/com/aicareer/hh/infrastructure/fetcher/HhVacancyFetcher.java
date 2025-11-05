package com.aicareer.hh.infrastructure.fetcher;

import com.aicareer.hh.hhapi.HhVacancy;
import com.aicareer.hh.hhapi.HhVacancySearchResponse;
import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.ports.VacancyFetcher;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HhVacancyFetcher implements VacancyFetcher {
    private final HhApiClient client;
    private final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public HhVacancyFetcher(HhApiClient client) {
        this.client = client;
    }

    @Override
    public List<HhVacancy> fetch(String text,
                                 String area,
                                 int perPage,
                                 String employment,
                                 String schedule,
                                 Integer salaryFrom) {
        List<HhVacancy> all = new ArrayList<>();

        for (int page = 0; page < 3; page++) { // до 3 страниц для MVP
            StringBuilder url = new StringBuilder("https://api.hh.ru/vacancies?");
            url.append("text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8));
            if (area != null && !area.isBlank()) url.append("&area=").append(area);
            url.append("&per_page=").append(perPage);
            url.append("&page=").append(page);
            if (employment != null && !employment.isBlank()) url.append("&employment=").append(employment);
            if (schedule != null && !schedule.isBlank()) url.append("&schedule=").append(schedule);
            if (salaryFrom != null) url.append("&salary=").append(salaryFrom);
            url.append("&order_by=publication_time");

            try {
                String body = client.get(url.toString());
                HhVacancySearchResponse resp = om.readValue(body, HhVacancySearchResponse.class);
                if (resp.items != null) all.addAll(resp.items);
                if (page >= resp.pages - 1) break; // последняя страница
            } catch (Exception e) {
                throw new RuntimeException("Fetch error: " + e.getMessage(), e);
            }
        }
        return all;
    }
}
