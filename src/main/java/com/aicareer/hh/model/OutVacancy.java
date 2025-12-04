package com.aicareer.hh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
        "id", "title", "company", "city",
        "experience", "employment", "schedule",
        "salaryFrom", "salaryTo", "currency",
        "skills", "description",
        "url", "source", "publishedAt"
})
public class OutVacancy {
    public String  id;
    public String  title;
    public String  company;
    public String  city;

    public String  experience;
    public String  employment;
    public String  schedule;

    public Integer salaryFrom;
    public Integer salaryTo;
    public String  currency;

    public List<String> skills;
    public String  description;

    public String  url;
    public String  source;
    public String  publishedAt;

    public Integer score;

    public static OutVacancy from(Vacancy v) {
        OutVacancy o = new OutVacancy();
        o.id          = v.getId();
        o.title       = v.getTitle();
        o.company     = v.getCompany();
        o.city        = v.getCity();

        o.experience  = v.getExperience();
        o.employment  = v.getEmployment();
        o.schedule    = v.getSchedule();

        o.salaryFrom  = v.getSalaryFrom();
        o.salaryTo    = v.getSalaryTo();
        o.currency    = v.getCurrency();

        o.skills      = v.getSkills() == null ? new ArrayList<>() : v.getSkills();
        o.description = v.getDescription();

        o.url         = v.getUrl();
        o.source      = v.getSource();
        o.publishedAt = v.getPublishedAt();
        return o;
    }
}
