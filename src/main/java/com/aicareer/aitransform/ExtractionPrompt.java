package com.aicareer.aitransform;

import java.util.List;
import java.util.stream.Collectors;

public final class ExtractionPrompt {
    private ExtractionPrompt() {
    }

    public static String build() {
        return build(SkillsExtraction.skillList());
    }

    public static String build(List<String> skills) {
        String skillsList = skills.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", ", "[", "]"));

        String skillsObject = skills.stream()
                .map(s -> "  \"" + s + "\": 1 or 0")
                .collect(Collectors.joining(",\n", "{\n", "\n}"));

        return "You extract required programming skills for a specific job role.\n\n"
                + "You are given a JSON object containing many vacancies for the same role.\n"
                + "Each vacancy includes fields like title, description, skills, responsibilities,\n"
                + "requirements, and other text.\n\n"
                + "You also have a fixed list of skills:\n" + skillsList + "\n\n"
                + "Your task:\n"
                + "Analyze ALL vacancy descriptions together and determine what skills are typically required for this role.\n"
                + "That means:\n"
                + "- You should NOT output skills from a single vacancy.\n"
                + "- You should infer the average, typical, or common skill requirements across the whole group.\n"
                + "- If a skill appears clearly required or strongly relevant in at least some vacancies (not necessarily all), mark it as 1.\n"
                + "- If a skill almost never appears or is irrelevant to the role, mark it as 0.\n"
                + "- If you see very few skills in the descriptions for this role, put 1 for every skill that appears in the text "
                + "(when the amount of data is low, use every opportunity to fill the table).\n"
                + "- If the vacancy title itself contains a skill name, immediately put 1 for that skill.\n\n"
                + "Return ONLY valid JSON in the following format:\n\n"
                + skillsObject + "\n\n"
                + "Important rules:\n"
                + "- Use integers 1 or 0 for each skill.\n"
                + "- Include ALL skills from the list. If a skill is not required, output 0 for it.\n"
                + "- Do NOT add any additional fields.\n"
                + "- Do NOT add explanations, comments, reasoning, or text outside the JSON.\n"
                + "Output ONLY the JSON table above.";
    }
}
