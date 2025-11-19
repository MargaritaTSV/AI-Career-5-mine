package com.aicareer.hh.infrastructure.mapper;

import com.aicareer.hh.model.HhVacancy;
import com.aicareer.hh.model.Vacancy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class DefaultVacancyMapper implements VacancyMapper {

    @Override
    public Vacancy mapFromRaw(HhVacancy r) {
        Vacancy v = new Vacancy();

        // базовые
        v.setId( str(any(r, s("getId"), f("id"))) );
        v.setTitle( str(any(r, s("getName"), f("name"))) );
        Object employer = any(r, s("getEmployer"), f("employer"));
        v.setCompany( str(any(employer, s("getName"), f("name"))) );
        Object area = any(r, s("getArea"), f("area"));
        v.setCity( str(any(area, s("getName"), f("name"))) );

        // режим/формат
        Object exp = any(r, s("getExperience"), f("experience"));
        v.setExperience( str(any(exp, s("getId"), f("id"))) );
        Object emp = any(r, s("getEmployment"), f("employment"));
        v.setEmployment( str(any(emp, s("getId"), f("id"))) );
        Object sch = any(r, s("getSchedule"), f("schedule"));
        v.setSchedule( str(any(sch, s("getId"), f("id"))) );

        // зарплата
        Object salary = any(r, s("getSalary"), f("salary"));
        if (salary != null) {
            v.setSalaryFrom( intObj(any(salary, s("getFrom"), f("from"))) );
            v.setSalaryTo(   intObj(any(salary, s("getTo"),   f("to"))) );
            v.setCurrency(   str(any(salary, s("getCurrency"), f("currency"))) );
        }

        // навыки (List<String> или List<KeySkill{name}})
        Object ks = any(r, s("getKeySkills"), f("key_skills"), f("keySkills"));
        List<String> skills = new ArrayList<>();
        if (ks instanceof Collection<?> col) {
            for (Object it : col) {
                String one = str(any(it, s("getName"), f("name")));
                if (one != null && !one.isBlank()) skills.add(one);
            }
        }
        v.setSkills(skills.isEmpty() ? null : skills);

        // краткое описание
        Object snip = any(r, s("getSnippet"), f("snippet"));
        String req = str(any(snip, s("getRequirement"), f("requirement")));
        String resp = str(any(snip, s("getResponsibility"), f("responsibility")));
        String desc = join(" ", req, resp);
        if (desc == null) desc = str(any(r, s("getDescription"), f("description")));
        v.setDescription(desc);

        // ссылки/мета
        String alt = str(any(r, s("getAlternateUrl"), f("alternate_url"), f("alternateUrl")));
        String url = str(any(r, s("getUrl"), f("url")));
        v.setUrl(alt != null ? alt : url);
        v.setSource("hh");

        // publishedAt (разные варианты)
        Object pub = any(r, s("getPublishedAt"), s("getPublished_at"), f("published_at"), f("publishedAt"));
        v.setPublishedAt(pub == null ? null : String.valueOf(pub));

        return v;
    }

    // ---------- helpers ----------
    private static String join(String sep, String... parts) {
        var list = java.util.Arrays.stream(parts)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
        return list.isEmpty() ? null : String.join(sep, list);
    }

    private static record S(String name) {}
    private static record F(String name) {}
    private static S s(String n) { return new S(n); }
    private static F f(String n) { return new F(n); }

    private static Object any(Object o, Object... accessors) {
        if (o == null) return null;
        for (Object acc : accessors) {
            try {
                if (acc instanceof S m) {
                    Method mm = o.getClass().getMethod(m.name());
                    mm.setAccessible(true);
                    Object v = mm.invoke(o);
                    if (v != null) return v;
                } else if (acc instanceof F fld) {
                    Field ff = findField(o.getClass(), fld.name());
                    if (ff != null) {
                        ff.setAccessible(true);
                        Object v = ff.get(o);
                        if (v != null) return v;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer intObj(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }
}
