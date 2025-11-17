package com.aicareer.hh.hhapi;

import java.util.List;

public class HhVacancy {
    public String id;
    public String name;
    public Area area;
    public Employer employer;
    public Salary salary;
    public List<KeySkill> key_skills;
    public Schedule schedule;
    public Employment employment;
    public Snippet snippet;
    public String url;

    public static class Area { public String name; }
    public static class Employer { public String name; }
    public static class Salary { public Integer from; public Integer to; public String currency; public Boolean gross; }
    public static class KeySkill { public String name; }
    public static class Schedule { public String name; }
    public static class Employment { public String name; }
    public static class Snippet { public String requirement; public String responsibility; }
}
