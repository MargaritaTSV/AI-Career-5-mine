package com.aicareer.hh.infrastructure.ranking;

import com.aicareer.hh.model.Vacancy;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SimpleRanker {

    public static int scoreBySkills(Vacancy v, List<String> wanted) {
        String text =
                ((v.getTitle() == null ? "" : v.getTitle()) + " " +
                        (v.getCompany() == null ? "" : v.getCompany()) + " " +
                        (v.getCity() == null ? "" : v.getCity()))
                        .toLowerCase(Locale.ROOT);

        int score = 0;
        if (wanted != null) {
            for (String w : wanted) {
                if (w == null || w.isBlank()) continue;
                if (text.contains(w.toLowerCase(Locale.ROOT))) score++;
            }
        }
        return score;
    }

    public static List<Vacancy> topK(List<Vacancy> items, List<String> wanted, int k) {
        return items.stream()
                .peek(v -> v.setScore(scoreBySkills(v, wanted)))
                .sorted(
                        Comparator.comparingInt(Vacancy::getScore).reversed()
                                .thenComparing(Vacancy::getSalaryTo,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .limit(k)
                .toList();
    }

    private SimpleRanker() {}
}
