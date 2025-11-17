package com.aicareer.hh.infrastructure.mapper;

import com.aicareer.hh.model.HhVacancy;
import com.aicareer.hh.model.Vacancy;

public interface VacancyMapper {
    Vacancy mapFromRaw(HhVacancy raw);
}
