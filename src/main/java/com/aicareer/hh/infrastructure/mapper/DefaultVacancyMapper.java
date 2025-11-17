package com.aicareer.hh.infrastructure.mapper;

import com.aicareer.hh.hhapi.HhKeySkill;
import com.aicareer.hh.hhapi.HhVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.VacancyMapper;

import java.util.ArrayList;
import java.util.List;

public class DefaultVacancyMapper implements VacancyMapper {
    @Override
    public Vacancy mapFromRaw(HhVacancy r) {
        Vacancy v = new Vacancy();

        v.id = r.id;
        v.title = r.name;
        v.company = (r.employer != null) ? r.employer.name : null;
        v.city = (r.area != null) ? r.area.name : null;

        v.experience = (r.experience != null) ? r.experience.id : null;
        v.employment = (r.employment != null) ? r.employment.id : null;
        v.schedule = (r.schedule != null) ? r.schedule.id : null;

        if (r.salary != null) {
            v.salaryFrom = r.salary.from;
            v.salaryTo = r.salary.to;
            v.currency = r.salary.currency;
        }

        List<String> skills = new ArrayList<>();
        if (r.key_skills != null) {
            for (HhKeySkill k : r.key_skills) {
                if (k != null && k.name != null) skills.add(k.name.toLowerCase());
            }
        }
        v.skills = skills;

        v.description = (r.snippet != null)
                ? (r.snippet.requirement != null ? r.snippet.requirement : r.snippet.responsibility)
                : null;

        v.url = r.alternate_url;
        v.publishedAt = r.published_at;
        v.source = "hh";

        return v;
    }
}
