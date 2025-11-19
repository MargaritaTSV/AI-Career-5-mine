package com.aicareer.hh.model;

import java.util.List;

public class Vacancy {
    private String id;
    private String title;
    private String company;
    private String city;

    private String experience;
    private String employment;
    private String schedule;

    private Integer salaryFrom;
    private Integer salaryTo;
    private String currency;

    private List<String> skills;
    private String description;

    private String url;
    private String source;
    private String publishedAt;

    private int score;

    // getters/setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getEmployment() { return employment; }
    public void setEmployment(String employment) { this.employment = employment; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public Integer getSalaryFrom() { return salaryFrom; }
    public void setSalaryFrom(Integer salaryFrom) { this.salaryFrom = salaryFrom; }

    public Integer getSalaryTo() { return salaryTo; }
    public void setSalaryTo(Integer salaryTo) { this.salaryTo = salaryTo; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
