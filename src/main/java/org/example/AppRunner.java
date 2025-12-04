package org.example;

import com.aicareer.aitransform.AppDatabaseInitializer;
import com.aicareer.aitransform.Config;
import com.aicareer.aitransform.SkillsExtraction;
import com.aicareer.aitransform.UserInfoExporter;
import com.aicareer.comparison.Comparison;
import com.aicareer.comparison.Comparison.ComparisonResult;
import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.hh.model.Vacancy;
import com.aicareer.hh.repository.JdbcVacancyRepository;
import com.aicareer.hh.repository.VacancyRepository;
import com.aicareer.recommendation.DeepseekRoadmapClient;
import com.aicareer.recommendation.RoadmapPromptBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import org.example.db.AnalysisRepository;

public class AppRunner {

  // дефолтный пользователь для quickstart
  private static final String DEFAULT_TEST_EMAIL = "test@example.com";
  private static final String QUICKSTART_EMAIL = "quickstart@example.com";
  private static final String QUICKSTART_NAME = "Quick Start Demo";

  public static void main(String[] args) {
    DbConnectionProvider provider = new DbConnectionProvider();
    VacancyRepository vacancyRepository = new JdbcVacancyRepository(provider);
    AnalysisRepository analysisRepository = new AnalysisRepository(provider);

    System.out.println("=======================================");
    System.out.println("      AI-Career Navigator CLI");
    System.out.println("=======================================\n");

    try (Scanner in = new Scanner(System.in)) {
      System.out.print("Введите OpenAI API ключ: ");
      Config.setApiKey(readNonEmptyLine(in));

      System.out.println("[DB] applying schema and seeds...");
      new AppDatabaseInitializer(provider).applySchemaAndData();

      // старый режим: если есть аргументы — работаем как раньше, без диалогов
      if (args.length > 0) {
        runNonInteractive(provider, vacancyRepository, analysisRepository, args);
        return;
      }

      runInteractive(provider, vacancyRepository, analysisRepository, in);
    }
  }

  // ===== НЕИНТЕРАКТИВНЫЙ РЕЖИМ (как было) =====

  private static void runNonInteractive(
      DbConnectionProvider provider,
      VacancyRepository vacancyRepository,
      AnalysisRepository analysisRepository,
      String[] args
  ) {
    String email = args.length > 0 ? args[0] : DEFAULT_TEST_EMAIL;
    String roleOverride = args.length > 1 ? args[1] : null;

    UserInfoExporter exporter = new UserInfoExporter(provider);

    UserInfoExporter.ProfileSnapshot profile = exporter.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("User not found: " + email));

    String targetRole = roleOverride != null && !roleOverride.isBlank()
        ? roleOverride
        : profile.targetRole();

    if (targetRole == null || targetRole.isBlank()) {
      throw new IllegalStateException("Target role is empty for user: " + email);
    }

    System.out.println("[USER] loaded profile for: " + profile.name() + " (" + targetRole + ")");

    runPipeline(exporter, vacancyRepository, analysisRepository, profile, targetRole);
  }

  // ===== ИНТЕРАКТИВНЫЙ РЕЖИМ =====

  private static void runInteractive(DbConnectionProvider provider,
                                     VacancyRepository vacancyRepository,
                                     AnalysisRepository analysisRepository,
                                     Scanner in) {
    UserInfoExporter exporter = new UserInfoExporter(provider);

    System.out.println("Как будем получать данные о пользователе?");
    System.out.println("1) QuickStart: использовать тестовый профиль со средними навыками");
    System.out.println("2) Выбрать готового пользователя из БД");
    System.out.println("3) Ввести свои данные и сохранить в БД");
    System.out.print("> ");

    int mode = readInt(in, 1, 3);

    if (mode == 1) {
      System.out.println("[MODE] QuickStart");
      UserInfoExporter.ProfileSnapshot quickstart = prepareQuickstartProfile(provider, exporter);
      String targetRole = quickstart.targetRole();
      System.out.println("[ROLE] Используем целевую роль QuickStart без вопросов: " + targetRole);
      System.out.println("\n[PIPELINE] Старт анализа для роли: " + targetRole);
      runPipeline(exporter, vacancyRepository, analysisRepository, quickstart, targetRole);
      return;
    }

    if (mode == 3) {
      System.out.println("[MODE] Ручной ввод с сохранением в БД");
      UserInfoExporter.ProfileSnapshot newProfile = createUserInteractive(provider, exporter, in);
      System.out.println("\n[PIPELINE] Старт анализа для роли: " + newProfile.targetRole());
      runPipeline(exporter, vacancyRepository, analysisRepository, newProfile, newProfile.targetRole());
      return;
    }

    final String userEmail = chooseExistingUserEmail(provider, in);

    UserInfoExporter.ProfileSnapshot profile = exporter.findByEmail(userEmail)
        .orElseThrow(() -> new IllegalStateException("User not found: " + userEmail));

    System.out.println("\n[USER] Загружен профиль: " + profile.name());
    System.out.println("      Email: " + userEmail);
    System.out.println("      Целевая роль из анкеты: " + profile.targetRole());

    String targetRole = chooseTargetRole(in, profile.targetRole());

    System.out.println("\n[PIPELINE] Старт анализа для роли: " + targetRole);
    runPipeline(exporter, vacancyRepository, analysisRepository, profile, targetRole);
  }

  private static UserInfoExporter.ProfileSnapshot prepareQuickstartProfile(
      DbConnectionProvider provider,
      UserInfoExporter exporter
  ) {
    Map<String, Integer> skills = Map.ofEntries(
        Map.entry("java", 1),
        Map.entry("spring", 1),
        Map.entry("sql", 1),
        Map.entry("docker", 1),
        Map.entry("kafka", 0),
        Map.entry("testing", 1),
        Map.entry("cloud", 0),
        Map.entry("git", 1)
    );

    try (Connection connection = provider.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String userId = upsertUser(connection, QUICKSTART_EMAIL, "quickstart-hash", QUICKSTART_NAME);
        upsertProfile(connection, userId, "Java Backend Developer", 3);
        upsertSkills(connection, userId, skills);
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw new IllegalStateException("Не удалось подготовить QuickStart пользователя", e);
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Ошибка при сохранении QuickStart профиля", e);
    }

    return exporter.findByEmail(QUICKSTART_EMAIL)
        .orElseThrow(() -> new IllegalStateException("QuickStart профиль не найден после сохранения"));
  }

  private static String chooseExistingUserEmail(DbConnectionProvider provider, Scanner in) {

    List<UserRow> users = loadUsers(provider);
    if (users.isEmpty()) {
      throw new IllegalStateException("В базе нет пользователей. Создайте профиль через пункт 2.");
    }

    System.out.println("\nДоступные пользователи:");
    for (int i = 0; i < users.size(); i++) {
      UserRow row = users.get(i);
      System.out.println((i + 1) + ") " + row.name() + " — " + row.email());
    }
    System.out.print("Введите номер пользователя (0 — ввести email вручную): ");

    int idx = readInt(in, 0, users.size());
    if (idx == 0) {
      System.out.print("Введите email пользователя: ");
      String email = in.nextLine().trim();
      while (email.isBlank()) {
        System.out.print("Email не может быть пустым, попробуйте ещё раз: ");
        email = in.nextLine().trim();
      }
      return email;
    }

    return users.get(idx - 1).email();
  }

  private static String chooseTargetRole(Scanner in, String fallbackRole) {
    if (fallbackRole != null && !fallbackRole.isBlank()) {
      System.out.print("Использовать целевую роль из анкеты (" + fallbackRole + ")? [Y/n]: ");
      String answer = in.nextLine().trim().toLowerCase(Locale.ROOT);
      if (answer.isEmpty() || answer.startsWith("y")) {
        return fallbackRole;
      }
    }

    System.out.print("Введите желаемую роль: ");
    String role = in.nextLine().trim();
    while (role.isBlank()) {
      System.out.print("Роль не может быть пустой, попробуйте ещё раз: ");
      role = in.nextLine().trim();
    }
    return role;
  }

  // ===== ОБЩИЙ ПАЙПЛАЙН (как был, но вынесен в отдельный метод) =====

  private static void runPipeline(UserInfoExporter exporter,
      VacancyRepository vacancyRepository,
      AnalysisRepository analysisRepository,
      UserInfoExporter.ProfileSnapshot profile,
      String targetRole) {

    if (targetRole == null || targetRole.isBlank()) {
      throw new IllegalStateException("Target role is empty for user: " + profile.name());
    }

    List<Vacancy> vacancies = vacancyRepository.findByRole(targetRole, 50);
    if (vacancies.isEmpty()) {
      throw new IllegalStateException(
          "Вакансии для роли '" + targetRole + "' не найдены в БД. Сначала загрузите их.");
    }

    Map<String, Integer> roleMatrix = SkillsExtraction.fromVacancies(vacancies);

    ComparisonResult comparison = Comparison.calculate(roleMatrix, profile.skills());

    UUID runId = analysisRepository.saveRun(profile.id(), targetRole, vacancies,
        profile.skills(), roleMatrix, comparison.statuses(), comparison.summary());

    System.out.println("[COMPARE] Strong sides: " +
        comparison.summary().getOrDefault("лучше ожидаемого", List.of()));
    System.out.println("[COMPARE] Weak sides:   " +
        comparison.summary().getOrDefault("требует улучшения", List.of()));

    String prompt = RoadmapPromptBuilder.build(vacancies, profile.skills(), roleMatrix, comparison.summary());

    try {
      String roadmap = DeepseekRoadmapClient.generateRoadmap(prompt);
      System.out.println("\n[AI RESPONSE]\n" + roadmap);
      analysisRepository.updateRoadmap(runId, roadmap);
    } catch (Exception e) {
      System.err.println("[AI] Failed to get roadmap from model: " + e.getMessage());
    }
  }

  // ===== УТИЛИТА ДЛЯ ЧТЕНИЯ ЦЕЛЫХ ЧИСЕЛ ИЗ КОНСОЛИ =====

  private static int readInt(Scanner in, int min, int max) {
    while (true) {
      String line = in.nextLine().trim();
      try {
        int value = Integer.parseInt(line);
        if (value < min || value > max) {
          System.out.print("Введите число от " + min + " до " + max + ": ");
          continue;
        }
        return value;
      } catch (NumberFormatException e) {
        System.out.print("Некорректный ввод, введите число от " + min + " до " + max + ": ");
      }
    }
  }

  private static String readNonEmptyLine(Scanner in) {
    String value = in.nextLine().trim();
    while (value.isBlank()) {
      System.out.print("Значение не может быть пустым, попробуйте ещё раз: ");
      value = in.nextLine().trim();
    }
    return value;
  }

  private static List<UserRow> loadUsers(DbConnectionProvider provider) {
    List<UserRow> users = new ArrayList<>();

    try (Connection connection = provider.getConnection();
         PreparedStatement ps = connection.prepareStatement(
             "SELECT email, name FROM app_users ORDER BY created_at DESC")) {

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          users.add(new UserRow(rs.getString("email"), rs.getString("name")));
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to load users", e);
    }
    return users;
  }

  private static String upsertUser(Connection connection, String email, String passwordHash, String name)
      throws SQLException {
    String selectSql = "SELECT id FROM app_users WHERE email = ?";
    try (PreparedStatement select = connection.prepareStatement(selectSql)) {
      select.setString(1, email);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        }
      }
    }

    String insertSql = "INSERT INTO app_users (id, email, password_hash, name, created_at) VALUES (?, ?, ?, ?, NOW())";
    String userId = UUID.randomUUID().toString();
    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
      insert.setString(1, userId);
      insert.setString(2, email);
      insert.setString(3, passwordHash);
      insert.setString(4, name);
      insert.executeUpdate();
      return userId;
    }
  }

  private static void upsertProfile(Connection connection, String userId, String targetRole, Integer experienceYears)
      throws SQLException {
    String sql = """
            INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
            VALUES (?, ?, ?, NOW())
            ON CONFLICT (user_id) DO UPDATE SET
                target_role = EXCLUDED.target_role,
                experience_years = EXCLUDED.experience_years,
                updated_at = EXCLUDED.updated_at
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, targetRole);
      if (experienceYears != null) {
        ps.setInt(3, experienceYears);
      } else {
        ps.setNull(3, java.sql.Types.INTEGER);
      }
      ps.executeUpdate();
    }
  }

  private static void upsertSkills(Connection connection, String userId, Map<String, Integer> skills)
      throws SQLException {
    String sql = """
            INSERT INTO app_skills (user_id, skill_name, level)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, skill_name) DO UPDATE SET level = EXCLUDED.level
            """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      for (Map.Entry<String, Integer> entry : skills.entrySet()) {
        ps.setString(1, userId);
        ps.setString(2, entry.getKey());
        ps.setInt(3, entry.getValue());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private static UserInfoExporter.ProfileSnapshot createUserInteractive(
      DbConnectionProvider provider,
      UserInfoExporter exporter,
      Scanner in) {

    System.out.print("Введите email: ");
    String email = readNonEmptyLine(in);

    System.out.print("Введите имя: ");
    String name = readNonEmptyLine(in);

    System.out.print("Введите целевую роль (например, 'Java Backend Developer'): ");
    String targetRole = readNonEmptyLine(in);

    System.out.print("Введите опыт (лет): ");
    int experience = readInt(in, 0, 60);

    Map<String, Integer> skills = new LinkedHashMap<>();
    System.out.println("Введите навыки в формате 'skill=1/0'. Пустая строка — закончить.");
    while (true) {
      System.out.print("skill=level > ");
      String line = in.nextLine().trim();
      if (line.isBlank()) {
        break;
      }
      if (!line.contains("=")) {
        System.out.println("Нужно указать через '='. Пример: java=1");
        continue;
      }
      String[] parts = line.split("=", 2);
      String skill = parts[0].trim();
      String levelStr = parts[1].trim();
      if (skill.isEmpty()) {
        System.out.println("Навык не может быть пустым");
        continue;
      }
      int level;
      try {
        level = Integer.parseInt(levelStr);
      } catch (NumberFormatException e) {
        System.out.println("Уровень должен быть 0 или 1");
        continue;
      }
      if (level != 0 && level != 1) {
        System.out.println("Уровень должен быть 0 или 1");
        continue;
      }
      skills.put(skill, level);
    }

    try (Connection connection = provider.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String userId = upsertUser(connection, email, "hash", name);
        upsertProfile(connection, userId, targetRole, experience);
        upsertSkills(connection, userId, skills);
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw new IllegalStateException("Не удалось сохранить профиль", e);
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Ошибка при сохранении данных пользователя", e);
    }

    return exporter.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Не удалось найти сохранённого пользователя"));
  }

  private record UserRow(String email, String name) {
  }
}
