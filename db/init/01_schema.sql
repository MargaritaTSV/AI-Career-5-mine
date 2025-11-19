-- Создать базовые таблицы и индексы
create table if not exists employers (
  id bigserial primary key,
  name text not null unique,
  hh_id bigint,
  created_at timestamptz default now()
);

create table if not exists areas (
  id bigserial primary key,
  name text not null unique,
  hh_id bigint,
  created_at timestamptz default now()
);

create table if not exists skills (
  id bigserial primary key,
  name text not null unique
);

create table if not exists vacancies (
  id bigserial primary key,
  title text not null,
  employer_id bigint references employers(id),
  area_id bigint references areas(id),
  salary_from int,
  salary_to int,
  currency text,
  url text not null unique,
  score int default 0,
  published_at timestamptz,
  created_at timestamptz default now()
);

create table if not exists vacancy_skills (
  vacancy_id bigint not null references vacancies(id) on delete cascade,
  skill_id   bigint not null references skills(id) on delete cascade,
  primary key (vacancy_id, skill_id)
);

create table if not exists vacancy_raw (
  id bigserial primary key,
  vacancy_id bigint references vacancies(id) on delete cascade,
  payload jsonb not null,
  created_at timestamptz default now()
);

create index if not exists idx_vacancies_score on vacancies(score);
create index if not exists idx_vacancies_published on vacancies(published_at);
create index if not exists idx_vacancy_raw_payload on vacancy_raw using gin(payload);
