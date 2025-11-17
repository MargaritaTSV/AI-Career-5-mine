package com.aicareer.hh.infrastructure.mapper;

import com.aicareer.hh.hhapi.HhVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.VacancyMapper;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultVacancyMapper implements VacancyMapper {

    // Набор часто встречающихся скиллов для быстрых эвристик из сниппета
    private static final List<String> DICT = List.of(
            "java","kotlin","spring","spring boot","hibernate","jpa",
            "maven","gradle","git","docker","kubernetes",
            "sql","postgres","mysql","oracle","clickhouse",
            "rest","kafka","rabbitmq","linux","aws","gcp","azure"
    );

    @Override
    public Vacancy mapFromRaw(HhVacancy v) {
        Vacancy x = new Vacancy();
        x.title   = v.name;
        x.company = v.employer != null ? v.employer.name : null;
        x.city    = v.area != null ? v.area.name : null;

        if (v.salary != null) {
            x.salaryFrom = v.salary.from;
            x.salaryTo   = v.salary.to;
            x.currency   = v.salary.currency;
        }
        x.url = v.url;

        // 1) key_skills если есть
        List<String> skills = v.key_skills == null ? new ArrayList<>()
                : v.key_skills.stream()
                .map(ks -> ks.name)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toCollection(ArrayList::new));

        // 2) Если пусто — берем из сниппета эвристически
        if (skills.isEmpty() && v.snippet != null) {
            String text = ((v.snippet.requirement == null ? "" : v.snippet.requirement) + " " +
                    (v.snippet.responsibility == null ? "" : v.snippet.responsibility))
                    .toLowerCase(Locale.ROOT);

            for (String kw : DICT) {
                if (text.contains(kw)) skills.add(kw);
            }
            // чуть нормализуем «spring boot» → и «spring»
            if (skills.contains("spring boot") && !skills.contains("spring")) skills.add("spring");
        }

        // удаляем дубли
        x.skills = skills.stream().distinct().toList();
        return x;
    }
}
