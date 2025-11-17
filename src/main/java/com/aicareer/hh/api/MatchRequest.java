package com.aicareer.hh.api;

import java.util.List;

public class MatchRequest {
    // от профиля/анкеты (всё опционально)
    public List<String> roles;        // ["java_developer","backend"]
    public List<String> skills;       // ["java","spring","sql"]
    public String experience;         // "noExperience" | "between1And3" | "between3And6" | "moreThan6" (пока не используем)
    public String areaId;             // "1" = Москва
    public Boolean remote;            // true -> schedule = "remote"
    public List<String> employment;   // ["full"] и т.п.
    public List<String> schedule;     // ["remote"] и т.п.
    public Integer salaryMin;         // минималка
    public List<String> negatives;    // слова-минусы для поиска: ["outstaff","ночные"]
    public Integer limit;             // сколько вернуть (пока используем 5)
}
