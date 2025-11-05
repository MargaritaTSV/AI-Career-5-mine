package com.aicareer.hh;

import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.infrastructure.fetcher.HhVacancyFetcher;
import com.aicareer.hh.infrastructure.mapper.DefaultVacancyMapper;
import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ranking.SimpleRanker;

import java.util.List;

public class DemoFetch {
    public static void main(String[] args) throws Exception {
        String USER_AGENT = "AI-Career/0.1 (+https://github.com/yourrepo; email: yourmail@gmail.com)";
        String TOKEN = null; // если есть — вставь

        String text = "java developer";
        String area = "1";
        int perPage = 20;
        String employment = null;
        String schedule = null;
        Integer salaryFrom = null;

        var client = new HhApiClient(USER_AGENT, TOKEN);
        var fetcher = new HhVacancyFetcher(client);
        var mapper = new DefaultVacancyMapper();

        var raws = fetcher.fetch(text, area, perPage, employment, schedule, salaryFrom);
        List<Vacancy> items = raws.stream().map(mapper::mapFromRaw).toList();

        System.out.println("Всего найдено: " + items.size());

        // что считаем целевыми скиллами (пока хардкод для демонстрации)
        List<String> wanted = List.of("java", "spring", "sql");

        // ТОП-5 по скиллам
        List<Vacancy> top5 = SimpleRanker.topK(items, wanted, 5);

        System.out.println("\nTOP-5 (по попаданиям скиллов):");
        for (Vacancy v : top5) {
            int score = SimpleRanker.scoreBySkills(v, wanted);
            System.out.println(score + "% | " +
                    (v.company != null ? v.company : "—") + " | " +
                    v.title + " | " +
                    (v.city != null ? v.city : "—") + " | " +
                    (v.salaryFrom != null ? v.salaryFrom : "—") + " " +
                    (v.currency != null ? v.currency : "") + " | " +
                    v.url);
        }

        // экспорт
        var exporter = new JsonExporter();
        exporter.writeJson(items, "vacancies_all.json");
        exporter.writeJson(top5,  "vacancies_top5.json");
        System.out.println("\nСохранено: vacancies_all.json и vacancies_top5.json");
    }
}
