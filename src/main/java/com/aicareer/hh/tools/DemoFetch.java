package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.infrastructure.fetcher.HhVacancyFetcher;
import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.infrastructure.mapper.DefaultVacancyMapper;
import com.aicareer.hh.repository.JdbcVacancyRepository;
import com.aicareer.hh.repository.VacancyRepository;
import com.aicareer.hh.service.DefaultVacancySearchService;
import com.aicareer.hh.service.SearchService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DemoFetch {

    public static final String USER_AGENT =
            "AI-Career/0.1 (https://github.com/semended/AI-Career-5)";
    public static final String TOKEN = System.getenv("HH_TOKEN");

    private static String nz(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String safe(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public static void main(String[] args) throws Exception {
        var client   = new HhApiClient(USER_AGENT, TOKEN);
        var fetcher  = new HhVacancyFetcher(client);
        var mapper   = new DefaultVacancyMapper();
        var exporter = new JsonExporter();

        SearchService service = new DefaultVacancySearchService(fetcher, mapper, exporter);

        // БД
        var dbProvider = new DbConnectionProvider();
        VacancyRepository vacancyRepository = new JdbcVacancyRepository(dbProvider);

        // роли + навыки
        Map<String, List<String>> matrix = RoleMatrix.load("role_skill_matrix.json");

        String area = "1";  // Москва
        int perPage = 100;
        String employment = null, schedule = null;
        Integer salaryFrom = null;

        for (var e : matrix.entrySet()) {
            String role = e.getKey();
            List<String> skills = e.getValue();

            var items = service.fetch(role, area, perPage, employment, schedule, salaryFrom);
            var top25 = service.topBySkills(items, skills, 25);

            // пишем всё в БД
            vacancyRepository.saveAll(items);

            System.out.println("\n== " + role + " ==");
            top25.forEach(v -> System.out.printf(
                    "score=%s | %s | %s | %s | %s-%s %s | %s%n",
                    nz(v.getScore()), nz(v.getTitle()), nz(v.getCompany()), nz(v.getCity()),
                    nz(v.getSalaryFrom()), nz(v.getSalaryTo()), nz(v.getCurrency()), nz(v.getUrl())
            ));

            // JSON в resources/export
            exporter.writeJson(items,  "vacancies_all_"   + safe(role) + ".json");
            exporter.writeJson(top25,  "vacancies_top25_" + safe(role) + ".json");
        }

        System.out.println("\n✅ Готово: вакансии в БД, JSON в src/main/resources/export");
    }
}