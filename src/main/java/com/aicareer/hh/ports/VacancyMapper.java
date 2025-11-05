package com.aicareer.hh.ports;

import com.aicareer.hh.hhapi.HhVacancy;
import com.aicareer.hh.model.Vacancy;

public interface VacancyMapper {
    Vacancy mapFromRaw(HhVacancy raw);
}
