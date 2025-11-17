package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.infrastructure.fetcher.HhVacancyFetcher;
import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.infrastructure.mapper.DefaultVacancyMapper;
import com.aicareer.hh.infrastructure.ranking.SimpleRanker;
import com.aicareer.hh.model.HhVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.VacancyFetcher;

import java.util.List;

public class DemoFetch {
    private static final String USER_AGENT = "AI-Career/0.1 (https://github.com/yourrepo; email: you@mail.com)";
    private static final String TOKEN = System.getenv("HH_TOKEN"); // можно пустым

    public static void main(String[] args) {
        HhApiClient client = new HhApiClient(USER_AGENT, TOKEN);
        VacancyFetcher fetcher = new HhVacancyFetcher(client);
        var mapper = new DefaultVacancyMapper();
        var repo = new com.aicareer.hh.repository.FileVacancyRepository();

        String text = "java developer";
        String area = "1"; // Москва
        int perPage = 20;

        List<HhVacancy> raws = fetcher.fetch(text, area, perPage, null, null, null);
        List<Vacancy> items = raws.stream().map(mapper::mapFromRaw).toList();

        System.out.println("Всего найдено: " + items.size());

        var top5 = SimpleRanker.topK(items, List.of("java", "spring", "sql"), 5);
        System.out.println("Top-5 по скиллам:");
        top5.forEach(v -> System.out.printf(
                "score=%d | %s | %s | %s | %s-%s %s | %s%n",
                v.getScore(),
                nz(v.getTitle()),
                nz(v.getCompany()),
                nz(v.getCity()),
                nz(v.getSalaryFrom()),
                nz(v.getSalaryTo()),
                nz(v.getCurrency()),
                nz(v.getUrl())
        ));
        repo.saveAll(items, "vacancies_all.json");
        repo.saveAll(top5,  "vacancies_top5.json");
        System.out.println("Сохранено: vacancies_all.json");
        System.out.println("Сохранено: vacancies_top5.json");
    }

    private static String nz(Object o) { return o == null ? "null" : String.valueOf(o); }
}
