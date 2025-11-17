package com.aicareer.hh;

import com.aicareer.hh.hhapi.HhVacancy;
import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.infrastructure.fetcher.HhVacancyFetcher;
import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.infrastructure.mapper.DefaultVacancyMapper;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.VacancyMapper;
import com.aicareer.hh.ranking.SimpleRanker;

import java.util.List;

public class DemoFetch {
    private static final String USER_AGENT = "AI-Career/0.1 (https://github.com/yourrepo; email: yourmail@gmail.com)";
    private static final String TOKEN = System.getenv("HH_TOKEN"); // или null

    public static void main(String[] args) throws Exception {
        var client = new HhApiClient(USER_AGENT, TOKEN);
        var fetcher = new HhVacancyFetcher(client);
        VacancyMapper mapper = new DefaultVacancyMapper();

        String text = "java developer";
        String area = "1";         // Москва (пример)
        int perPage = 20;
        String employment = null;  // full, part, etc.
        String schedule = null;    // remote, flexible, etc.
        Integer salaryFrom = null;

        List<HhVacancy> raws = fetcher.fetch(text, area, perPage, employment, schedule, salaryFrom);
        List<Vacancy> items = raws.stream().map(mapper::mapFromRaw).toList();

        System.out.println("Всего найдено: " + items.size());

        List<String> wanted = List.of("java", "spring", "sql");
        var tops = SimpleRanker.topK(items, wanted, 5);

        System.out.println("\nTop-5 по скиллам:");
        for (Vacancy v : tops) {
            int score = SimpleRanker.scoreBySkills(v, wanted);
            System.out.printf("score=%d | %s | %s | %s | %s-%s %s | %s%n",
                    score,
                    v.title,
                    v.company,
                    v.city,
                    v.salaryFrom, v.salaryTo, v.currency,
                    v.url);
        }

        var exporter = new JsonExporter();
        exporter.writeJson(items, "vacancies_all.json");
        exporter.writeJson(tops, "vacancies_top5.json");
    }
}
