package org.example.roles;

public class Role {
    private final String id;
    private final String title;
    private final String description;
    private final String technologies;

    public Role(String id, String title, String description, String technologies) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.technologies = technologies;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTechnologies() { return technologies; }

    @Override
    public String toString() {
        return id + ". " + title + " — " + description + " [" + technologies + "]";
    }
}
