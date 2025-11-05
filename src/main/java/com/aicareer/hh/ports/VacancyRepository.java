package com.aicareer.hh.ports;

import java.util.List;
import com.aicareer.hh.model.Vacancy;

public interface VacancyRepository {
    void saveAll(List<Vacancy> list);
    List<Vacancy> findAll();
}
