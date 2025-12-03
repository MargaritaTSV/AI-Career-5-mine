INSERT INTO users (id, email, password_hash, name, created_at) VALUES
  ('11111111-1111-1111-1111-111111111111', 'java@example.com', '2ca0033', 'Мария Бэкенд', NOW()),
  ('22222222-2222-2222-2222-222222222222', 'datasci@example.com', '4889ba9b', 'Илья Дата', NOW()),
  ('33333333-3333-3333-3333-333333333333', 'frontend@example.com', '4103158', 'Анна Фронтенд', NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_profiles (user_id, target_role, experience_years, skills, updated_at) VALUES
  ('11111111-1111-1111-1111-111111111111', 'Java Backend Developer', 2,
   '{"java":1,"spring":1,"sql":1,"docker":1,"kafka":0,"aws":0}', NOW()),
  ('22222222-2222-2222-2222-222222222222', 'Data Scientist', 3,
   '{"python":1,"pandas":1,"numpy":1,"machine_learning":1,"sql":1,"spark":0,"aws":0}', NOW()),
  ('33333333-3333-3333-3333-333333333333', 'Frontend JavaScript Developer', 4,
   '{"javascript":1,"react":1,"typescript":1,"html":1,"css":1,"docker":0}', NOW())
ON CONFLICT (user_id) DO UPDATE SET
    target_role = EXCLUDED.target_role,
    experience_years = EXCLUDED.experience_years,
    skills = EXCLUDED.skills,
    updated_at = EXCLUDED.updated_at;
