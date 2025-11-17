package com.aicareer.hh.ports;

import com.aicareer.hh.hhapi.HhVacancy;
import java.util.List;

public interface VacancyFetcher {
    List<HhVacancy> fetch(String text,
                          String area,
                          int perPage,
                          String employment,
                          String schedule,
                          Integer salaryFrom);
}
