package com.aicareer.hh.service;

import com.aicareer.hh.model.HhVacancy;
import java.util.List;

public interface VacancySearchService {
    List<HhVacancy> find(String text, String area, int perPage, String employment, String schedule, Integer salaryFrom);
    List<HhVacancy> topBySkills(List<HhVacancy> items, List<String> skills, int k);
}
