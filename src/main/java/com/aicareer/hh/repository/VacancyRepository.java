package com.aicareer.hh.repository;

import com.aicareer.hh.model.Vacancy;

import java.util.Collection;
import java.util.List;

public interface VacancyRepository {

    void saveAll(Collection<Vacancy> vacancies);

    List<Vacancy> findBySource(String source);
}