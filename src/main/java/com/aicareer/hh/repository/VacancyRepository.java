package com.aicareer.hh.repository;

import com.aicareer.hh.model.Vacancy;
import java.util.Collection;

public interface VacancyRepository {
    void saveAll(Collection<Vacancy> items, String fileName);
}
