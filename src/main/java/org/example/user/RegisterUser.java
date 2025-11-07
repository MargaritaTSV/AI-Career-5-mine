package org.example.user;

public class RegisterUser {
    private final UserRepository userRepo;

    public RegisterUser(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public void register(String email, String password, String name) {
        if (userRepo.findByEmail(email).isPresent()) {
            System.out.println("Пользователь с таким email уже существует!");
            return;
        }

        String hash = Integer.toHexString(password.hashCode());
        User user = new User(email, hash, name);
        userRepo.save(user);
        System.out.println("Пользователь зарегистрирован: " + email);
    }
}


