package com.aicareer.hh.ports;

import java.util.List;
import com.aicareer.hh.hhapi.HhVacancy;

public interface VacancyFetcher {
    List<HhVacancy> fetch(
            String text,
            String area,
            int perPage,
            String employment,
            String schedule,
            Integer salaryFrom
    );
}
