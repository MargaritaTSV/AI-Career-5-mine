TRUNCATE TABLE app_skills, app_profiles, app_users RESTART IDENTITY CASCADE;

-- Два идеальных кандидата
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'alex.perfect@example.com', 'hash', 'Александр Идеальный', NOW())
ON CONFLICT (id) DO NOTHING;
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'olga.perfect@example.com', 'hash', 'Ольга Идеальная', NOW())
ON CONFLICT (id) DO NOTHING;

-- Три опытных, но с пробелами
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('33333333-3333-3333-3333-333333333333', 'maria.solid@example.com', 'hash', 'Мария Опытная', NOW())
ON CONFLICT (id) DO NOTHING;
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('44444444-4444-4444-4444-444444444444', 'pavel.solid@example.com', 'hash', 'Павел Уверенный', NOW())
ON CONFLICT (id) DO NOTHING;
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('55555555-5555-5555-5555-555555555555', 'sergey.solid@example.com', 'hash', 'Сергей Развивающийся', NOW())
ON CONFLICT (id) DO NOTHING;

-- Два новичка
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('66666666-6666-6666-6666-666666666666', 'irina.junior@example.com', 'hash', 'Ирина Новичок', NOW())
ON CONFLICT (id) DO NOTHING;
INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('77777777-7777-7777-7777-777777777777', 'nikita.junior@example.com', 'hash', 'Никита Начинающий', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Java Backend Developer', 6, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'Java Backend Developer', 7, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('33333333-3333-3333-3333-333333333333', 'Java Backend Developer', 4, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('44444444-4444-4444-4444-444444444444', 'Java Backend Developer', 5, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('55555555-5555-5555-5555-555555555555', 'Java Backend Developer', 3, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('66666666-6666-6666-6666-666666666666', 'Java Backend Developer', 1, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;
INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('77777777-7777-7777-7777-777777777777', 'Java Backend Developer', 0, NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;

-- Идеальные кандидаты
INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('11111111-1111-1111-1111-111111111111', 'java', 1),
       ('11111111-1111-1111-1111-111111111111', 'spring', 1),
       ('11111111-1111-1111-1111-111111111111', 'sql', 1),
       ('11111111-1111-1111-1111-111111111111', 'docker', 1),
       ('11111111-1111-1111-1111-111111111111', 'kafka', 1),
       ('11111111-1111-1111-1111-111111111111', 'microservices', 1),
       ('11111111-1111-1111-1111-111111111111', 'testing', 1),
       ('11111111-1111-1111-1111-111111111111', 'cloud', 1),
       ('22222222-2222-2222-2222-222222222222', 'java', 1),
       ('22222222-2222-2222-2222-222222222222', 'spring', 1),
       ('22222222-2222-2222-2222-222222222222', 'sql', 1),
       ('22222222-2222-2222-2222-222222222222', 'docker', 1),
       ('22222222-2222-2222-2222-222222222222', 'kafka', 1),
       ('22222222-2222-2222-2222-222222222222', 'microservices', 1),
       ('22222222-2222-2222-2222-222222222222', 'testing', 1),
       ('22222222-2222-2222-2222-222222222222', 'cloud', 1)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;

-- Опытные с пробелами
INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('33333333-3333-3333-3333-333333333333', 'java', 1),
       ('33333333-3333-3333-3333-333333333333', 'spring', 1),
       ('33333333-3333-3333-3333-333333333333', 'sql', 1),
       ('33333333-3333-3333-3333-333333333333', 'docker', 1),
       ('33333333-3333-3333-3333-333333333333', 'kafka', 0),
       ('33333333-3333-3333-3333-333333333333', 'microservices', 1),
       ('33333333-3333-3333-3333-333333333333', 'testing', 1),
       ('33333333-3333-3333-3333-333333333333', 'cloud', 0),
       ('44444444-4444-4444-4444-444444444444', 'java', 1),
       ('44444444-4444-4444-4444-444444444444', 'spring', 1),
       ('44444444-4444-4444-4444-444444444444', 'sql', 1),
       ('44444444-4444-4444-4444-444444444444', 'docker', 1),
       ('44444444-4444-4444-4444-444444444444', 'kafka', 1),
       ('44444444-4444-4444-4444-444444444444', 'microservices', 1),
       ('44444444-4444-4444-4444-444444444444', 'testing', 1),
       ('44444444-4444-4444-4444-444444444444', 'cloud', 0),
       ('55555555-5555-5555-5555-555555555555', 'java', 1),
       ('55555555-5555-5555-5555-555555555555', 'spring', 1),
       ('55555555-5555-5555-5555-555555555555', 'sql', 1),
       ('55555555-5555-5555-5555-555555555555', 'docker', 0),
       ('55555555-5555-5555-5555-555555555555', 'kafka', 1),
       ('55555555-5555-5555-5555-555555555555', 'microservices', 1),
       ('55555555-5555-5555-5555-555555555555', 'testing', 1),
       ('55555555-5555-5555-5555-555555555555', 'cloud', 0)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;

-- Новички
INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('66666666-6666-6666-6666-666666666666', 'python', 1),
       ('66666666-6666-6666-6666-666666666666', 'git', 0),
       ('77777777-7777-7777-7777-777777777777', 'python', 1),
       ('77777777-7777-7777-7777-777777777777', 'linux', 0)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;
