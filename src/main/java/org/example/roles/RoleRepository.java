package org.example.roles;

import java.util.List;

public class RoleRepository {
    public static List<Role> getAllRoles() {
        return List.of(
                new Role("1", "Java Developer", "Разработка серверной логики", "Java, Spring, SQL"),
                new Role("2", "Data Scientist", "Машинное обучение и анализ данных", "Python, Pandas, scikit-learn"),
                new Role("3", "DevOps Engineer", "Инфраструктура и CI/CD", "Docker, Kubernetes, Linux")
        );
    }
}

