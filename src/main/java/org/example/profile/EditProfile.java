package org.example.profile;

import java.util.*;

public class EditProfile {
    private final ProfileRepository repo;

    public EditProfile(ProfileRepository repo) {
        this.repo = repo;
    }

    public void run(String userId) {
        Scanner sc = new Scanner(System.in);

        System.out.println("\n=== Заполнение анкеты ===");
        System.out.print("Целевая роль: ");
        String role = sc.nextLine();


        List<String> allSkills = Arrays.asList("java", "python", "javascript", "c++", "c#");

        System.out.println("\nУкажите владение навыками (1 — да, 0 — нет):");
        Map<String, Integer> skills = new HashMap<>();

        for (String skill : allSkills) {
            System.out.print(skill + ": ");
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                skills.put(skill, value == 1 ? 1 : 0);
            } catch (Exception e) {
                skills.put(skill, 0);
            }
        }

        System.out.print("\nОпыт (в годах): ");
        int years = Integer.parseInt(sc.nextLine());

        Profile profile = new Profile(userId, role, skills, years);
        repo.save(profile);

        System.out.println("\n Анкета сохранена!");
        System.out.println("Роль: " + role);
        System.out.println("Навыки: " + skills);
        System.out.println("Опыт: " + years + " лет");
    }
}

