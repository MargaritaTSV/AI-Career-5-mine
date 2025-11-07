package org.example.user;

import java.util.Optional;

public class LoginUser {
    private final UserRepository repo;

    public LoginUser(UserRepository repo) {
        this.repo = repo;
    }

    public void login(String email, String password) {
        String passwordHash = Integer.toHexString(password.hashCode());
        Optional<User> user = repo.findByEmailAndPassword(email, passwordHash);

        if (user.isPresent()) {
            System.out.println("Вход выполнен успешно: " + user.get().getName());
        } else {
            System.out.println("Неверный email или пароль");
        }
    }
}

