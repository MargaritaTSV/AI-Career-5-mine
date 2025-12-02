package org.example;

import org.example.db.Database;
import org.example.user.UserRepository;
import org.example.user.RegisterUser;
import org.example.user.LoginUser;
import org.example.profile.ProfileRepository;
import org.example.profile.Profile;

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

        System.out.println("[AUTH] Регистрация пользователя...");
        register.register("test@example.com", "12345", "Мария");

        System.out.println("[AUTH] Логин...");
        LoginUser login = new LoginUser(userRepo);
        login.login("test@example.com", "12345");

        System.out.println("[PROFILE] Сохранение профиля...");
        Profile profile = new Profile(
                userRepo.findByEmail("test@example.com").orElseThrow().getId(),
                "Java Developer",
                Map.of("java", 1, "sql", 1),
                2
        );
        profileRepo.save(profile);

        System.out.println("[PROFILE] Извлекаем из БД...");
        System.out.println(profileRepo.findByUserId(profile.getUserId()).orElseThrow());

        System.out.println("[DONE] Все операции завершены успешно");
    }
}
