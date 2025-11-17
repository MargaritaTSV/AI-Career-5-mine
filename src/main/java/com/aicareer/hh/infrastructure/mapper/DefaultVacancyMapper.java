package com.aicareer.hh.infrastructure.mapper;

import com.aicareer.hh.model.HhVacancy;
import com.aicareer.hh.model.Vacancy;

public class DefaultVacancyMapper implements VacancyMapper {
    @Override
    public Vacancy mapFromRaw(HhVacancy raw) {
        Vacancy v = new Vacancy();
        v.setTitle(raw.name);
        v.setCompany(raw.employer != null ? raw.employer.name : null);
        v.setCity(raw.area != null ? raw.area.name : null);
        if (raw.salary != null) {
            v.setSalaryFrom(raw.salary.from);
            v.setSalaryTo(raw.salary.to);
            v.setCurrency(raw.salary.currency);
        }
        v.setSnippet(raw.snippet != null ? raw.snippet.requirement : null);
        v.setUrl(raw.alternate_url != null ? raw.alternate_url : raw.url);
        v.setScore(0);
        return v;
    }
}
