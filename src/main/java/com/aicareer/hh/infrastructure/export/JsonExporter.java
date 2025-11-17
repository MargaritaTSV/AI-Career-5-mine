package com.aicareer.hh.infrastructure.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;

public class JsonExporter {
    private final ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void writeJson(Object data, String fileName) {
        try {
            om.writeValue(new File(fileName), data);
        } catch (Exception e) {
            throw new RuntimeException("JSON export failed: " + e.getMessage(), e);
        }
    }
}
