//  Вспомогательный класс для демонстрации функционала регистрации, входа и анкет
package org.example;

import org.example.db.Database;
import org.example.user.*;
import org.example.profile.*;
import java.util.Map;


public class DemoMain {
    public static void main(String[] args) {

        Database.init(); // создание таблиц users и profiles
        Database.smokeTest();
        System.out.println("Схема готова. Файл БД: ./data/aicareer.mv.db");

        UserRepository userRepo = new UserRepository();
        ProfileRepository profileRepo = new ProfileRepository();

        RegisterUser register = new RegisterUser(userRepo);
        register.register("test@example.com", "12345", "Мария");

        register.register("test@example.com", "12345", "Мария");

        LoginUser login = new LoginUser(userRepo);
        login.login("test@example.com", "12345"); // успешный вход
        login.login("wrong@example.com", "12345"); // неверный email
        login.login("test@example.com", "wrongpass"); // неверный пароль

        Profile profile = new Profile(
                userRepo.findByEmail("test@example.com").orElseThrow().getId(),
                "Java Developer",
                Map.of(
                        "java", 1,
                        "python", 0,
                        "javascript", 1,
                        "c++", 0,
                        "c#", 1
                ),
                2
        );
        profileRepo.save(profile);

        EditUser editUser = new EditUser(userRepo);
        editUser.editName("test@example.com", "Мария Тестовая");

        System.out.println("Профиль пользователя:");
        System.out.println(profileRepo.findByUserId(profile.getUserId()).orElseThrow());
    }
}


