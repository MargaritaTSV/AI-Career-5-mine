package com.aicareer.hh.service;

import com.aicareer.hh.model.Vacancy;
import java.util.Collection;
import java.util.List;

public interface SearchService {
    List<Vacancy> fetch(String text, String area, int perPage, String employment, String schedule, Integer salaryFrom);
    List<Vacancy> topBySkills(Collection<Vacancy> items, List<String> skills, int k);
    void saveAll(Collection<Vacancy> items, String fileName);
}
