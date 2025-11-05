package com.aicareer.hh.ranking;

import com.aicareer.hh.model.Vacancy;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleRanker {

    // score 0..100 по доле совпавших скиллов
    public static int scoreBySkills(Vacancy v, List<String> wantedSkills) {
        if (wantedSkills == null || wantedSkills.isEmpty()) return 0;

        Set<String> textHits = new HashSet<>();
        String text = ((v.title != null ? v.title : "") + " " + (v.description != null ? v.description : ""))
                .toLowerCase(Locale.ROOT);

        // что считаем «попаданием»: либо есть в v.skills, либо встречается в title/description
        Set<String> have = v.skills != null
                ? v.skills.stream().filter(Objects::nonNull).map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet())
                : Collections.emptySet();

        long hits = wantedSkills.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .peek(s -> { if (text.contains(s)) textHits.add(s); })
                .filter(s -> have.contains(s) || text.contains(s))
                .count();

        return (int) Math.round(100.0 * hits / wantedSkills.size());
    }

    // вернуть топ-K вакансий по убыванию score
    public static List<Vacancy> topK(List<Vacancy> list, List<String> wantedSkills, int k) {
        return list.stream()
                .sorted((a, b) -> Integer.compare(
                        scoreBySkills(b, wantedSkills),
                        scoreBySkills(a, wantedSkills)))
                .limit(k)
                .collect(Collectors.toList());
    }
}
