package org.example;

import org.example.db.Database;
import org.example.profile.Profile;
import org.example.profile.ProfileRepository;
import org.example.user.LoginUser;
import org.example.user.RegisterUser;
import org.example.user.User;
import org.example.user.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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

        try (Scanner scanner = new Scanner(System.in)) {
            UserRepository userRepo = new UserRepository();
            ProfileRepository profileRepo = new ProfileRepository();
            RegisterUser register = new RegisterUser(userRepo);
            LoginUser login = new LoginUser(userRepo);

            System.out.print("Очистить таблицы users и user_profiles перед заполнением? (y/N): ");
            if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                clearAllData();
                System.out.println("[DB] Таблицы очищены");
            }

            boolean running = true;
            while (running) {
                System.out.println("\nВыберите действие:");
                System.out.println("1) Добавить пользователя вручную и сохранить профиль");
                System.out.println("2) Создать демо-пользователя (test@example.com) и профиль");
                System.out.println("3) Показать все сохранённые профили");
                System.out.println("0) Выход");
                System.out.print("> ");

                String action = scanner.nextLine().trim();
                switch (action) {
                    case "1" -> createUserInteractive(scanner, register, profileRepo, login, userRepo);
                    case "2" -> seedDemoUser(register, profileRepo, login, userRepo);
                    case "3" -> profileRepo.findAll().forEach(System.out::println);
                    case "0" -> running = false;
                    default -> System.out.println("Неизвестная команда, попробуйте ещё раз.");
                }
            }
        }

        System.out.println("[DONE] Все операции завершены успешно");
    }

    private static void createUserInteractive(
            Scanner scanner,
            RegisterUser register,
            ProfileRepository profileRepo,
            LoginUser login,
            UserRepository userRepo
    ) {
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        while (email.isBlank()) {
            System.out.print("Email не может быть пустым, повторите: ");
            email = scanner.nextLine().trim();
        }

        System.out.print("Пароль: ");
        String password = scanner.nextLine().trim();
        while (password.isBlank()) {
            System.out.print("Пароль не может быть пустым, повторите: ");
            password = scanner.nextLine().trim();
        }

        System.out.print("Имя: ");
        String name = scanner.nextLine().trim();
        if (name.isBlank()) {
            name = "Без имени";
        }

        register.register(email, password, name);
        login.login(email, password);

        User user = userRepo.findByEmail(email).orElseThrow();

        System.out.print("Целевая роль: ");
        String role = scanner.nextLine().trim();
        if (role.isBlank()) {
            role = "Developer";
        }

        System.out.print("Опыт в годах (целое число): ");
        int experience = parseIntWithDefault(scanner.nextLine().trim(), 0);

        Map<String, Integer> skills = new HashMap<>();
        System.out.println("Ввод навыков. Формат: название=уровень. Пустая строка — завершить.");
        while (true) {
            System.out.print("Навык: ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) {
                break;
            }
            String[] parts = line.split("=");
            if (parts.length != 2) {
                System.out.println("Ожидается формат название=уровень. Пример: java=1");
                continue;
            }
            try {
                skills.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException e) {
                System.out.println("Уровень должен быть числом. Попробуйте ещё раз.");
            }
        }

        Profile profile = new Profile(user.getId(), role, skills, experience);
        profileRepo.save(profile);

        System.out.println("[PROFILE] Сохранено: " + profileRepo.findByUserId(user.getId()).orElse(profile));
    }

    private static void seedDemoUser(
            RegisterUser register,
            ProfileRepository profileRepo,
            LoginUser login,
            UserRepository userRepo
    ) {
        register.register("test@example.com", "12345", "Мария");
        login.login("test@example.com", "12345");

        User user = userRepo.findByEmail("test@example.com").orElseThrow();
        Profile profile = new Profile(
                user.getId(),
                "Java Developer",
                Map.of("java", 1, "sql", 1),
                2
        );
        profileRepo.save(profile);
        System.out.println("[PROFILE] Демо профиль обновлён");
    }

    private static int parseIntWithDefault(String raw, int def) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return def;
        }
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
}
