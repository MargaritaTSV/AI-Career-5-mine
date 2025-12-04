package com.aicareer.hh.service;

import com.aicareer.hh.infrastructure.mapper.VacancyMapper;
import com.aicareer.hh.infrastructure.ranking.SimpleRanker; // <-- ВАЖНО: правильный пакет
import com.aicareer.hh.model.HhVacancy;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.VacancyFetcher;
import com.aicareer.hh.repository.VacancyRepository;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class DefaultVacancySearchService implements SearchService {
    private final VacancyFetcher fetcher;
    private final VacancyMapper mapper;
    private final VacancyRepository vacancyRepository;

    public DefaultVacancySearchService(VacancyFetcher fetcher,
                                       VacancyMapper mapper,
                                       VacancyRepository vacancyRepository) {
        this.fetcher = fetcher;
        this.mapper = mapper;
        this.vacancyRepository = vacancyRepository;
    }

    @Override
    public List<Vacancy> fetch(String text,
                               String area,
                               int perPage,
                               String employment,
                               String schedule,
                               Integer salaryFrom) {
        List<HhVacancy> raws = fetcher.fetch(text, area, perPage, employment, schedule, salaryFrom);
        return raws.stream().map(mapper::mapFromRaw).collect(Collectors.toList());
    }

    @Override
    public List<Vacancy> topBySkills(Collection<Vacancy> items, List<String> skills, int k) {
        return items.stream()
                .peek(v -> v.setScore(SimpleRanker.scoreBySkills(v, skills))) // статический вызов
                .sorted(Comparator.comparingInt(Vacancy::getScore).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }

    @Override
    public void saveAll(Collection<Vacancy> items, String fileName) {
        vacancyRepository.saveAll(items);
    }
}
