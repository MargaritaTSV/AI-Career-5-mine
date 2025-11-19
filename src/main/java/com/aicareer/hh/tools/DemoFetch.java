package com.aicareer.hh.tools;

import com.aicareer.hh.infrastructure.fetcher.HhVacancyFetcher;
import com.aicareer.hh.infrastructure.http.HhApiClient;
import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.infrastructure.mapper.DefaultVacancyMapper;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.service.DefaultVacancySearchService;
import com.aicareer.hh.service.SearchService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DemoFetch {
    // подставь свои значения, у тебя они уже были
    public static final String USER_AGENT = "AI-Career/0.1 (https://github.com/yourrepo; email: youmail@gmail.com)";
    public static final String TOKEN = System.getenv("HH_TOKEN"); // либо твоя строка

    private static String nz(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String safe(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static void main(String[] args) throws Exception {
        var client   = new HhApiClient(USER_AGENT, TOKEN);
        var fetcher  = new HhVacancyFetcher(client);
        var mapper   = new DefaultVacancyMapper();
        var exporter = new JsonExporter();
        SearchService service = new DefaultVacancySearchService(fetcher, mapper, exporter);

        // 1) читаем все роли+скиллы из resources/role_skill_matrix.json
        Map<String, List<String>> matrix = RoleMatrix.load("role_skill_matrix.json");

        // 2) общие параметры поиска
        String area = "1";                 // Москва
        int perPage = 100;                 // побольше, чтобы точно хватило
        String employment = null, schedule = null;
        Integer salaryFrom = null;

        // 3) для каждой роли: ищем, ранжируем, берём топ-15, сохраняем
        for (var e : matrix.entrySet()) {
            String role = e.getKey();
            List<String> skills = e.getValue();

            // ищем по названию роли (это самый простой и стабильный вариант)
            var items = service.fetch(role, area, perPage, employment, schedule, salaryFrom);
            var top15 = service.topBySkills(items, skills, 15);

            System.out.println("\n== " + role + " ==");
            top15.forEach(v -> System.out.printf(
                    "score=%s | %s | %s | %s | %s-%s %s | %s%n",
                    nz(v.getScore()), nz(v.getTitle()), nz(v.getCompany()), nz(v.getCity()),
                    nz(v.getSalaryFrom()), nz(v.getSalaryTo()), nz(v.getCurrency()), nz(v.getUrl())
            ));

            // отдельные файлы по роли
            exporter.writeJson(items, "vacancies_all_"   + safe(role) + ".json");
            exporter.writeJson(top15, "vacancies_top15_" + safe(role) + ".json");
        }
    }
}
