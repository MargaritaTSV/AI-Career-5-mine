package com.aicareer.hh.hhapi;

import java.util.List;

public class HhVacancySearchResponse {
    public int found;
    public int pages;
    public int per_page;
    public int page;
    public List<HhVacancy> items;
}
