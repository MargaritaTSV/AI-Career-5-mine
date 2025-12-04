package org.example;

import org.example.db.Database;
import org.example.profile.Profile;
import org.example.profile.ProfileRepository;
import org.example.user.RegisterUser;
import org.example.user.User;
import org.example.user.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DemoMain {
    public static void main(String[] args) {
        System.out.println("[DB] Инициализация схемы...");
        Database.init();

        System.out.println("[DB] Проверка подключения...");
        try {
            Database.get();
            System.out.println("[DB] Подключение успешно");
        } catch (Exception e) {
            System.out.println("[DB] Ошибка подключения");
            e.printStackTrace();
            return;
        }

        UserRepository userRepo = new UserRepository();
        ProfileRepository profileRepo = new ProfileRepository();
        RegisterUser register = new RegisterUser(userRepo);

        clearAllData();
        System.out.println("[DB] Таблицы users и user_profiles очищены");

        List<SeedUser> samples = buildSamples();
        for (SeedUser sample : samples) {
            register.register(sample.email(), sample.password(), sample.name());
            User user = userRepo.findByEmail(sample.email()).orElseThrow();
            Profile profile = new Profile(user.getId(), sample.targetRole(), sample.skills(), sample.experience());
            profileRepo.save(profile);
            System.out.println("[PROFILE] Добавлен тестовый пользователь: " + sample.email());
        }

        System.out.println("[DONE] Загружено " + samples.size() + " тестовых пользователей без участия оператора");
    }

    private static List<SeedUser> buildSamples() {
        List<SeedUser> users = new ArrayList<>();

        Map<String, Integer> idealSkills = Map.of(
                "java", 1,
                "spring", 1,
                "sql", 1,
                "docker", 1,
                "kafka", 1,
                "microservices", 1,
                "testing", 1,
                "cloud", 1
        );

        Map<String, Integer> strongButMissingKafka = Map.of(
                "java", 1,
                "spring", 1,
                "sql", 1,
                "docker", 1,
                "kafka", 0,
                "microservices", 1,
                "testing", 1,
                "cloud", 0
        );

        Map<String, Integer> strongButMissingCloud = Map.of(
                "java", 1,
                "spring", 1,
                "sql", 1,
                "docker", 1,
                "kafka", 1,
                "microservices", 1,
                "testing", 0,
                "cloud", 0
        );

        Map<String, Integer> strongButMissingDocker = Map.of(
                "java", 1,
                "spring", 1,
                "sql", 1,
                "docker", 0,
                "kafka", 1,
                "microservices", 1,
                "testing", 1,
                "cloud", 0
        );

        Map<String, Integer> pythonOnly = Map.of(
                "python", 1,
                "git", 0
        );

        Map<String, Integer> basicsOnly = Map.of(
                "python", 1,
                "linux", 0
        );

        users.add(new SeedUser("alex.perfect@example.com", "12345", "Александр Идеальный", 6, "Java Backend Developer", idealSkills));
        users.add(new SeedUser("olga.perfect@example.com", "12345", "Ольга Идеальная", 7, "Java Backend Developer", idealSkills));
        users.add(new SeedUser("maria.solid@example.com", "12345", "Мария Опытная", 4, "Java Backend Developer", strongButMissingKafka));
        users.add(new SeedUser("pavel.solid@example.com", "12345", "Павел Уверенный", 5, "Java Backend Developer", strongButMissingCloud));
        users.add(new SeedUser("sergey.solid@example.com", "12345", "Сергей Развивающийся", 3, "Java Backend Developer", strongButMissingDocker));
        users.add(new SeedUser("irina.junior@example.com", "12345", "Ирина Новичок", 1, "Java Backend Developer", pythonOnly));
        users.add(new SeedUser("nikita.junior@example.com", "12345", "Никита Начинающий", 0, "Java Backend Developer", basicsOnly));

        return users;
    }

    private static void clearAllData() {
        try (Connection c = Database.get()) {
            try (PreparedStatement ps1 = c.prepareStatement("DELETE FROM user_profiles");
                 PreparedStatement ps2 = c.prepareStatement("DELETE FROM users")) {
                ps1.executeUpdate();
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось очистить таблицы", e);
        }
    }

    private record SeedUser(
            String email,
            String password,
            String name,
            int experience,
            String targetRole,
            Map<String, Integer> skills
    ) {
    }
}
