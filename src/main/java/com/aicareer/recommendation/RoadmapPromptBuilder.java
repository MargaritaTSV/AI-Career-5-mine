package com.aicareer.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RoadmapPromptBuilder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RoadmapPromptBuilder() {
    }

    public static String build(List<?> vacancies,
                               Map<String, Integer> userMatrix,
                               Map<String, Integer> desiredMatrix,
                               Map<String, List<String>> summary) {
        List<String> userSkills = flaggedSkills(userMatrix);
        List<String> targetSkills = flaggedSkills(desiredMatrix);
        List<String> missingSkills = targetSkills.stream()
                .filter(skill -> !userSkills.contains(skill))
                .toList();

        return String.join("\n",
                "Ты — карьерный консультант и планировщик обучения. Используй модель deepseek-r1:8b.",
                "Исходные данные:",
                "- Матрица навыков пользователя (1 = владеет): " + formatJson(userMatrix),
                "- Требования роли: " + formatJson(desiredMatrix),
                "- Баланс навыков: " + formatJson(summary),
                "- Вакансии для анализа стеков и узких тем: " + formatJson(vacancies),
                "Извлеки ключевые технологии и темы из вакансий, опираясь на уже освоенные навыки: " + String.join(", ", userSkills),
                "Навыки, которых не хватает: " + (missingSkills.isEmpty() ? "нет" : String.join(", ", missingSkills)),
                "Сформируй учебный маршрут из 3–7 шагов. Каждый шаг выводи на новой строке в формате 'Шаг N: ...',",
                "в шагах можно упоминать библиотеки, алгоритмы, паттерны или технические акценты, на которые стоит обратить внимание.",
                "Сначала закрывай пробелы по весомым зависимостям, затем усиливай стек роли.",
                "Не используй формулировку 'вы будете уметь' и не завершай ей ответ; финальную строку оформи как 'Итог: <краткий результат без этой фразы>'.");
    }

    private static List<String> flaggedSkills(Map<String, Integer> matrix) {
        return matrix.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static String formatJson(Object payload) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to format JSON payload", e);
        }
    }
}
