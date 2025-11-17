package org.example.user;

import java.util.Optional;

public class EditUser {
    private final UserRepository repo;

    public EditUser(UserRepository repo) {
        this.repo = repo;
    }

    public void editName(String email, String newName) {
        Optional<User> userOpt = repo.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setName(newName);
            repo.update(user);
            System.out.println("Имя пользователя успешно обновлено: " + newName);
        } else {
            System.out.println("Пользователь с email " + email + " не найден");
        }
    }
}


