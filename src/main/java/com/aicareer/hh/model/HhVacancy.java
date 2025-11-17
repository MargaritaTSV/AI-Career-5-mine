package com.aicareer.hh.model;

public class HhVacancy {
    public String name;

    public Employer employer;
    public static class Employer { public String name; }

    public Area area;
    public static class Area { public String name; }

    public Salary salary;
    public static class Salary { public Integer from; public Integer to; public String currency; }

    public Snippet snippet;
    public static class Snippet { public String requirement; public String responsibility; }

    public String alternate_url;
    public String url;
}
