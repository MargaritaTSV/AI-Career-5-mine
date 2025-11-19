-- Справочники
insert into employers(name) values ('Консалт Плюс') on conflict do nothing;
insert into employers(name) values ('Ранг') on conflict do nothing;
insert into employers(name) values ('РЖД') on conflict do nothing;

insert into areas(name) values ('Москва') on conflict do nothing;

insert into skills(name) values ('java') on conflict do nothing;
insert into skills(name) values ('spring') on conflict do nothing;
insert into skills(name) values ('sql') on conflict do nothing;

-- Примерные вакансии
insert into vacancies(title, employer_id, area_id, salary_from, salary_to, currency, url, score, published_at)
values
('Java / Kotlin разработчик (Software Engineer)',
 (select id from employers where name='Консалт Плюс'),
 (select id from areas where name='Москва'),
 null, 450000, 'RUR', 'https://hh.ru/vacancy/107650257', 0, now())
on conflict (url) do nothing;

insert into vacancies(title, employer_id, area_id, salary_from, salary_to, currency, url, score, published_at)
values
('Frontend × Vue × Nuxt разработчик',
 (select id from employers where name='Ранг'),
 (select id from areas where name='Москва'),
 130000, 170000, 'RUR', 'https://hh.ru/vacancy/127295887', 0, now())
on conflict (url) do nothing;

-- Привязка навыков
insert into vacancy_skills(vacancy_id, skill_id)
select v.id, s.id
from vacancies v
join skills s on s.name in ('java','spring','sql')
where v.url = 'https://hh.ru/vacancy/107650257'
on conflict do nothing;
