package com.aicareer.hh.repository;

import com.aicareer.hh.infrastructure.export.JsonExporter;
import com.aicareer.hh.model.Vacancy;
import java.util.Collection;

public final class FileVacancyRepository implements VacancyRepository {
    private final JsonExporter exporter = new JsonExporter();
    @Override public void saveAll(Collection<Vacancy> items, String fileName) {
        exporter.writeJson(items, fileName);
    }
}
