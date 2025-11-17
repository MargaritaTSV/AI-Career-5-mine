package com.aicareer.hh.ranking;

import com.aicareer.hh.model.Vacancy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SimpleRanker {

    public static int scoreBySkills(Vacancy v, List<String> wanted) {
        if (v.skills == null || v.skills.isEmpty() || wanted == null || wanted.isEmpty()) return 0;
        var wl = wanted.stream().map(s -> s.toLowerCase(Locale.ROOT)).toList();
        int score = 0;
        for (String s : v.skills) {
            if (s == null) continue;
            for (String w : wl) {
                if (s.contains(w)) { score++; break; }
            }
        }
        return score;
    }

    public static List<Vacancy> topK(List<Vacancy> items, List<String> wanted, int k) {
        return items.stream()
                .sorted(Comparator.comparingInt((Vacancy v) -> scoreBySkills(v, wanted)).reversed()
                        .thenComparing(v -> v.salaryTo == null ? 0 : v.salaryTo, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(k)
                .collect(Collectors.toList());
    }
}
