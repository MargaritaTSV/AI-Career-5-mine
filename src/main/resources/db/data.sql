INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111',
        'test@example.com',
        'hash',
        'Test User',
        NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('22222222-2222-2222-2222-222222222222',
        'ivan.petrov@example.com',
        'hash2',
        'Иван Петров',
        NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('33333333-3333-3333-3333-333333333333',
        'anna.sidorova@example.com',
        'hash3',
        'Анна Сидорова',
        NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_users (id, email, password_hash, name, created_at)
VALUES ('44444444-4444-4444-4444-444444444444',
        'maksim.ivanov@example.com',
        'hash4',
        'Максим Иванов',
        NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111',
        'Java Backend Developer',
        1,
        NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222',
        'Frontend JavaScript Developer',
        2,
        NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('33333333-3333-3333-3333-333333333333',
        'Data Scientist',
        3,
        NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;

INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
VALUES ('44444444-4444-4444-4444-444444444444',
        'DevOps Engineer',
        4,
        NOW())
ON CONFLICT (user_id) DO UPDATE SET target_role = EXCLUDED.target_role, experience_years = EXCLUDED.experience_years, updated_at = EXCLUDED.updated_at;

INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('11111111-1111-1111-1111-111111111111', 'java',   1),
       ('11111111-1111-1111-1111-111111111111', 'spring', 1),
       ('11111111-1111-1111-1111-111111111111', 'sql',    1)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;

INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('22222222-2222-2222-2222-222222222222', 'javascript', 1),
       ('22222222-2222-2222-2222-222222222222', 'react',      1),
       ('22222222-2222-2222-2222-222222222222', 'css',        1),
       ('22222222-2222-2222-2222-222222222222', 'html',       1),
       ('22222222-2222-2222-2222-222222222222', 'nodejs',     0)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;

INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('33333333-3333-3333-3333-333333333333', 'python',      1),
       ('33333333-3333-3333-3333-333333333333', 'pandas',      1),
       ('33333333-3333-3333-3333-333333333333', 'machine_learning', 1),
       ('33333333-3333-3333-3333-333333333333', 'sql',         1),
       ('33333333-3333-3333-3333-333333333333', 'deep_learning', 0)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;

INSERT INTO app_skills (user_id, skill_name, level)
VALUES ('44444444-4444-4444-4444-444444444444', 'linux',      1),
       ('44444444-4444-4444-4444-444444444444', 'docker',     1),
       ('44444444-4444-4444-4444-444444444444', 'kubernetes', 0),
       ('44444444-4444-4444-4444-444444444444', 'ci_cd',      1),
       ('44444444-4444-4444-4444-444444444444', 'terraform',  0)
ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level;
