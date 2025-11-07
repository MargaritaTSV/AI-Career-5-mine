package org.example.user;

public class Session {
    private static User currentUser;

    public static void login(User user) {
        currentUser = user;
        System.out.println("Авторизован как: " + user.getName() + " (" + user.getEmail() + ")");
    }

    public static void logout() {
        if (currentUser != null) {
            System.out.println("Выход: " + currentUser.getEmail());
            currentUser = null;
        } else {
            System.out.println("Вы не вошли в систему.");
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}

