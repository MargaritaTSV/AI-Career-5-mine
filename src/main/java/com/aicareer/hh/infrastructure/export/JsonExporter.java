package com.aicareer.hh.infrastructure.export;

import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.ports.Exporter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.List;

public class JsonExporter implements Exporter {
    private final ObjectMapper om = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Override
    public void writeJson(List<Vacancy> list, String filePath) {
        try {
            om.writeValue(new File(filePath), list);
        } catch (Exception e) {
            throw new RuntimeException("Export error: " + e.getMessage(), e);
        }
    }
}
