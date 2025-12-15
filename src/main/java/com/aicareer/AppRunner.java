package com.aicareer;

import com.aicareer.aitransform.AppDatabaseInitializer;
import com.aicareer.aitransform.Config;
import com.aicareer.aitransform.SkillsExtraction;
import com.aicareer.aitransform.UserInfoExporter;
import com.aicareer.comparison.Comparison;
import com.aicareer.comparison.Comparison.ComparisonResult;
import com.aicareer.hh.infrastructure.db.DbConnectionProvider;
import com.aicareer.recommendation.DeepseekRoadmapClient;
import com.aicareer.recommendation.RoadmapPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.stream.Stream;

public class AppRunner {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  private static final Path USER_MATRIX_PATH = Path.of("src/main/resources/matrices/user_skill_matrix.json");
  private static final Path ROLE_MATRIX_PATH = Path.of("src/main/resources/matrices/desired_role_matrix.json");
  private static final Path STATUSES_PATH = Path.of("src/main/resources/matrices/skill_comparison.json");
  private static final Path SUMMARY_PATH = Path.of("src/main/resources/matrices/summary.json");

  // дефолтный пользователь для quickstart
  private static final String DEFAULT_TEST_EMAIL = "test@example.com";
  private static final String QUICKSTART_EMAIL = "quickstart@example.com";
  private static final String QUICKSTART_NAME = "Quick Start Demo";

  public static void main(String[] args) {
    DbConnectionProvider provider = new DbConnectionProvider();

    System.out.println("=======================================");
    System.out.println("      AI-Career Navigator CLI");
    System.out.println("=======================================\n");

    try (Scanner in = new Scanner(System.in)) {
      Config.ensureApiKeyFromInput(in);

      System.out.println("\n[DB] applying schema and seeds...");
      new AppDatabaseInitializer(provider).applySchemaAndData();

      // старый режим: если есть аргументы — работаем как раньше, без диалогов
      if (args.length > 0) {
        runNonInteractive(provider, args);
        return;
      }

      // новый режим: диалог с пользователем в терминале
      runInteractive(provider, in);
    }
  }

  // ===== НЕИНТЕРАКТИВНЫЙ РЕЖИМ (как было) =====

  private static void runNonInteractive(DbConnectionProvider provider, String[] args) {
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

    runPipeline(exporter, profile, targetRole);
  }

  // ===== ИНТЕРАКТИВНЫЙ РЕЖИМ =====

  private static void runInteractive(DbConnectionProvider provider, Scanner in) {
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
      runPipeline(exporter, quickstart, targetRole);
      return;
    }

    if (mode == 3) {
      System.out.println("[MODE] Ручной ввод с сохранением в БД");
      UserInfoExporter.ProfileSnapshot newProfile = createUserInteractive(provider, exporter, in);
      System.out.println("\n[PIPELINE] Старт анализа для роли: " + newProfile.targetRole());
      runPipeline(exporter, newProfile, newProfile.targetRole());
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
    runPipeline(exporter, profile, targetRole);
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

  private static List<UserRow> loadUsers(DbConnectionProvider provider) {
    String sql = "SELECT email, name FROM app_users ORDER BY created_at DESC, email";
    List<UserRow> users = new ArrayList<>();

    try (Connection connection = provider.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        users.add(new UserRow(rs.getString("email"), rs.getString("name")));
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Не удалось получить список пользователей", e);
    }

    return users;
  }

  private static UserInfoExporter.ProfileSnapshot createUserInteractive(
      DbConnectionProvider provider,
      UserInfoExporter exporter,
      Scanner in) {

    System.out.print("Email: ");
    String email = in.nextLine().trim();
    while (email.isBlank()) {
      System.out.print("Email не может быть пустым, попробуйте ещё раз: ");
      email = in.nextLine().trim();
    }

    System.out.print("Имя: ");
    String name = in.nextLine().trim();
    if (name.isBlank()) {
      name = "Новый пользователь";
    }

    String role = pickTargetRole(in, "Java Backend Developer");

    System.out.print("Опыт в годах (0-50): ");
    int experience = readInt(in, 0, 50);

    Map<String, Integer> skills = new LinkedHashMap<>();
    System.out.println("\nОцените навыки: вводите 1 для тех, которыми владеете, и 0 для остальных.");
    System.out.println("Навыки будут выводиться по одному:");
    for (String skill : SkillsExtraction.skillList()) {
      System.out.print(skill + " (1/0): ");
      skills.put(skill, readInt(in, 0, 1));
    }

    String passwordHash = "manual-" + email.hashCode();

    try (Connection connection = provider.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String userId = upsertUser(connection, email, passwordHash, name);
        upsertProfile(connection, userId, role, experience);
        upsertSkills(connection, userId, skills);
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Не удалось сохранить данные пользователя", e);
    }

    return exporter.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("Не удалось найти пользователя после сохранения"));
  }

  private static String upsertUser(Connection connection, String email, String passwordHash, String name)
      throws SQLException {

    String sql = """
        INSERT INTO app_users (id, email, password_hash, name, created_at)
        VALUES (?, ?, ?, ?, NOW())
        ON CONFLICT (email) DO UPDATE
        SET password_hash = EXCLUDED.password_hash,
            name = EXCLUDED.name
        RETURNING id
        """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, UUID.randomUUID().toString());
      ps.setString(2, email);
      ps.setString(3, passwordHash);
      ps.setString(4, name);

      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getString("id");
      }
    }
  }

  private static void upsertProfile(Connection connection, String userId, String role, int experience)
      throws SQLException {

    String sql = """
        INSERT INTO app_profiles (user_id, target_role, experience_years, updated_at)
        VALUES (?, ?, ?, NOW())
        ON CONFLICT (user_id) DO UPDATE
        SET target_role = EXCLUDED.target_role,
            experience_years = EXCLUDED.experience_years,
            updated_at = EXCLUDED.updated_at
        """;

    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, userId);
      ps.setString(2, role);
      ps.setInt(3, experience);
      ps.executeUpdate();
    }
  }

  private static void upsertSkills(Connection connection, String userId, Map<String, Integer> skills)
      throws SQLException {

    if (skills.isEmpty()) {
      return;
    }

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

  // выбор желаемой роли
  private static String chooseTargetRole(Scanner in, String profileRole) {
    System.out.println("\nВыберите целевую роль:");
    System.out.println("1) Использовать роль из анкеты: " + profileRole);
    System.out.println("2) Выбрать из списка по вакансиям (введите цифру из списка)");
    System.out.println("3) Ввести название роли вручную");
    System.out.print("> ");

    int choice = readInt(in, 1, 3);

    switch (choice) {
      case 1 -> {
        return profileRole;
      }
      case 2 -> {
        return pickTargetRole(in, profileRole);
      }
      case 3 -> {
        System.out.print("Введите название роли (например, Java Developer): ");
        String role = in.nextLine().trim();
        while (role.isBlank()) {
          System.out.print("Роль не может быть пустой, попробуйте ещё раз: ");
          role = in.nextLine().trim();
        }
        return role;
      }
      default -> throw new IllegalStateException("Unexpected value: " + choice);
    }
  }

  private static String pickTargetRole(Scanner in, String fallbackRole) {
    List<String> roles = listAvailableRoles();
    if (roles.isEmpty()) {
      System.out.print("Доступных ролей в списке нет. Введите название роли: ");
      String manual = in.nextLine().trim();
      return manual.isBlank() ? fallbackRole : manual;
    }

    System.out.println("\nДоступные роли по данным о вакансиях:");
    for (int i = 0; i < roles.size(); i++) {
      System.out.println((i + 1) + ") " + roles.get(i));
    }
    System.out.print("Введите номер роли (0 — оставить текущую: " + fallbackRole + "): ");

    int idx = readInt(in, 0, roles.size());
    if (idx == 0) {
      return fallbackRole;
    }
    return roles.get(idx - 1);
  }

  // поиск всех файлов vacancies_top25_*.json и превращение их в человекочитаемые названия ролей
  private static List<String> listAvailableRoles() {
    Path exportDir = Path.of("src/main/resources/export");
    if (!Files.isDirectory(exportDir)) {
      return List.of();
    }

    try (Stream<Path> stream = Files.list(exportDir)) {
      return stream
          .filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .filter(name -> name.startsWith("vacancies_top25_") && name.endsWith(".json"))
          .sorted()
          .map(name -> name.substring(
              "vacancies_top25_".length(),
              name.length() - ".json".length()
          ))
          .map(safe -> safe.replace('_', ' ')) // "middle_java_developer" -> "middle java developer"
          .toList();
    } catch (IOException e) {
      System.err.println("[ROLE] Не удалось прочитать список файлов в " + exportDir + ": " + e.getMessage());
      return List.of();
    }
  }

  // ===== ОБЩИЙ ПАЙПЛАЙН (как был, но вынесен в отдельный метод) =====

  private static void runPipeline(UserInfoExporter exporter,
      UserInfoExporter.ProfileSnapshot profile,
      String targetRole) {

    if (targetRole == null || targetRole.isBlank()) {
      throw new IllegalStateException("Target role is empty for user: " + profile.name());
    }

    exporter.writeUserSkillMatrix(profile.skills(), USER_MATRIX_PATH);

    String vacanciesResource = resolveVacanciesResource(targetRole);
    System.out.println("[ROLE] using vacancies resource: " + vacanciesResource);

    Map<String, Integer> roleMatrix = SkillsExtraction.fromResource(vacanciesResource);
    writeJson(ROLE_MATRIX_PATH, roleMatrix);

    ComparisonResult comparison = Comparison.calculate(roleMatrix, profile.skills());
    Comparison.writeOutputs(comparison, STATUSES_PATH, SUMMARY_PATH);

    List<String> masteredSkills = profile.skills().entrySet().stream()
        .filter(entry -> entry.getValue() != null && entry.getValue() == 1)
        .map(Map.Entry::getKey)
        .toList();

    List<String> requiredSkills = roleMatrix.entrySet().stream()
        .filter(entry -> entry.getValue() != null && entry.getValue() == 1)
        .map(Map.Entry::getKey)
        .toList();

    System.out.println("\n[SKILLS] Освоенные: " + String.join(", ", masteredSkills));
    System.out.println("[SKILLS] Требуются для роли: " + String.join(", ", requiredSkills));
    List<String> missingSkills = requiredSkills.stream()
        .filter(skill -> !masteredSkills.contains(skill))
        .toList();
    System.out.println("[SKILLS] Необходимо освоить: " + (missingSkills.isEmpty()
        ? "нет"
        : String.join(", ", missingSkills)));

    Path skillGraphImage = runVisualizationScript();

    String prompt = RoadmapPromptBuilder.build(
        vacanciesResource,
        "matrices/user_skill_matrix.json",
        "matrices/desired_role_matrix.json",
        "graphs/skills-graph.json",
        masteredSkills,
        requiredSkills,
        missingSkills
    );

    try {
      String roadmap = DeepseekRoadmapClient.generateRoadmap(prompt, skillGraphImage);
      System.out.println("\n[AI RESPONSE]\n" + roadmap);
    } catch (Exception e) {
      System.err.println("[AI] Failed to get roadmap from model: " + e.getMessage());
    }
  }

  private static Path runVisualizationScript() {
    Path scriptPath = Path.of("Visualisation/Visualisation/plotskillsgraph.py");
    Path expectedImage = Path.of("Visualisation/target/visualisations/skills_graph.png");

    if (!Files.exists(scriptPath)) {
      if (Files.exists(expectedImage)) {
        return expectedImage;
      }
      System.err.println("[VIS] Visualization script not found at " + scriptPath.toAbsolutePath());
      return null;
    }

    String pythonExecutable = resolvePythonExecutable();

    if (pythonExecutable == null) {
      if (Files.exists(expectedImage)) {
        return expectedImage;
      }
      System.err.println(
          "[VIS] Python interpreter not found. Install Python 3 or add it to PATH to render the graph.");
      return null;
    }

    ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, scriptPath.toString());
    processBuilder.inheritIO();

    try {
      Process process = processBuilder.start();
      int exitCode = process.waitFor();
      if (exitCode == 0) {
        if (Files.exists(expectedImage)) {
          return expectedImage;
        }
        System.err.println("[VIS] Visualization finished, but image not found at " + expectedImage.toAbsolutePath());
      } else {
        System.err.println("[VIS] Visualization script exited with code " + exitCode);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to start visualization script", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("[VIS] Visualization script was interrupted");
    }
    return null;
  }

  private static String resolvePythonExecutable() {
    List<String> candidates = List.of("python3", "python", "py");

    for (String candidate : candidates) {
      ProcessBuilder check = new ProcessBuilder(candidate, "--version");
      try {
        Process process = check.start();
        int exit = process.waitFor();
        if (exit == 0) {
          return candidate;
        }
      } catch (IOException e) {
        // ignore and try next candidate
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.err.println("[VIS] Interrupted while searching for python interpreter");
        return null;
      }
    }

    return null;
  }

  // ===== СТАРЫЕ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

  private static String resolveVacanciesResource(String roleName) {
    String safe = roleName.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "");
    String resource = "export/vacancies_top25_" + safe + ".json";

    if (resourceExists(resource)) {
      return resource;
    }
    throw new IllegalArgumentException("No vacancies_top25 resource found for role: " + roleName);
  }

  private static boolean resourceExists(String resource) {
    return Thread.currentThread()
        .getContextClassLoader()
        .getResource(resource) != null;
  }

  private static void writeJson(Path path, Object payload) {
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      MAPPER.writeValue(path.toFile(), payload);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write JSON to " + path, e);
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

  private record UserRow(String email, String name) {
  }
}
