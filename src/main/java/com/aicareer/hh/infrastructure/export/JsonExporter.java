package com.aicareer.hh.infrastructure.export;

import com.aicareer.hh.model.OutVacancy;
import com.aicareer.hh.model.Vacancy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.Collection;
import java.util.List;

public final class JsonExporter {
    private final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public void writeJson(Collection<Vacancy> items, String fileName) {
        try {
            List<OutVacancy> out = items.stream()
                    .map(OutVacancy::from)
                    .toList();
            om.writeValue(new File(fileName), out);
            System.out.println("Сохранено: " + fileName);
        } catch (Exception e) {
            throw new RuntimeException("JSON export failed: " + e.getMessage(), e);
        }
    }
}
