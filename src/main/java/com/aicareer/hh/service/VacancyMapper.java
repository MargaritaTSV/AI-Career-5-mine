package com.aicareer.hh.service;

import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;

/** Maps vacancy DTOs from export files into domain entities. */
public final class VacancyMapper {

    private VacancyMapper() {
    }

    public static Vacancy fromOutVacancy(OutVacancy o) {
        if (o == null) {
            return null;
        }

        Vacancy v = new Vacancy();
        v.setId(o.id);
        v.setTitle(o.title);
        v.setCompany(o.company);
        v.setCity(o.city);
        v.setExperience(o.experience);
        v.setEmployment(o.employment);
        v.setSchedule(o.schedule);
        v.setSalaryFrom(o.salaryFrom);
        v.setSalaryTo(o.salaryTo);
        v.setCurrency(o.currency);
        v.setDescription(o.description);
        v.setUrl(o.url);
        v.setSource(o.source);
        v.setPublishedAt(o.publishedAt);
        v.setSkills(o.skills);
        v.setScore(o.score != null ? o.score : 0);
        return v;
    }
}
