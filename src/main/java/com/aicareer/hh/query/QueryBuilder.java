package com.aicareer.hh.query;

import com.aicareer.hh.api.MatchRequest;

import java.util.*;
import java.util.stream.Collectors;

public class QueryBuilder {

    // Простейшие синонимы для MVP
    private static final Map<String, List<String>> SYN = Map.of(
            "java", List.of("java"),
            "spring", List.of("spring", "spring boot", "springboot"),
            "sql", List.of("sql", "postgresql", "postgres", "mysql")
    );

    public static HhQueryParams from(MatchRequest req) {
        HhQueryParams p = new HhQueryParams();

        // 1) text = роли + скиллы (+синонимы) + минус-слова
        List<String> tokens = new ArrayList<>();
        if (req.roles != null) tokens.addAll(req.roles);

        if (req.skills != null) {
            for (String s : req.skills) {
                if (s == null) continue;
                tokens.add(s);
                List<String> syn = SYN.get(s.toLowerCase(Locale.ROOT));
                if (syn != null) tokens.addAll(syn);
            }
        }

        String text = tokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .collect(Collectors.joining(" "));

        if (req.negatives != null) {
            for (String n : req.negatives) {
                if (n != null && !n.isBlank()) text += " -" + n.trim();
            }
        }
        p.text = text.isEmpty() ? "developer" : text;

        // 2) Прочие параметры: employment/schedule/salary/perPage/area
        p.employment = firstOrNull(req.employment);

        String sched = firstOrNull(req.schedule);
        if (sched == null && Boolean.TRUE.equals(req.remote)) sched = "remote";
        p.schedule = sched;

        boolean isRemote = "remote".equalsIgnoreCase(sched);
        p.area = isRemote
                ? null
                : (req.areaId != null && !req.areaId.isBlank() ? req.areaId : "1"); // Москва по умолчанию

        p.perPage = 50;
        p.salaryFrom = req.salaryMin;

        return p;
    }

    private static String firstOrNull(List<String> xs) {
        return (xs != null && !xs.isEmpty()) ? xs.get(0) : null;
    }
}
