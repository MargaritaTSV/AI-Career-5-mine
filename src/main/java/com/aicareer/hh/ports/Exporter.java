package com.aicareer.hh.ports;

import java.util.List;
import com.aicareer.hh.model.Vacancy;

public interface Exporter {
    void writeJson(List<Vacancy> list, String filePath);
}
