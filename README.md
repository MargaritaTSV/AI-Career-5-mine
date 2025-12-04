# AI Career Navigator

AI Career Navigator — монолитное Java-приложение для построения индивидуальных карьерных траекторий IT-специалистов на основе:

- анкеты пользователя (опыт и текущие навыки);
- реальных вакансий с job-сайтов;
- графа зависимостей между навыками;
- анализа требований к целевым ролям с помощью модели **gpt-4o-mini** (OpenAI API).

Проект реализуется в рамках программы «Высшая школа программной инженерии» МФТИ при поддержке компании ООО MWS.

---

## Требования

- **Java 21 (JDK)**
- **Maven 3.9+**
- **PostgreSQL 18+**
- Доступ в интернет
- Аккаунт OpenAI и API-ключ с доступом к модели **gpt-4o-mini**

---

## TL;DR

| Цель                              | Команда / действие                                                                     |
|-----------------------------------|---------------------------------------------------------------------------------------|
| Создать БД и пользователя         | `psql -U postgres -f db/init.sql` (или выполнить SQL вручную)                         |
| Применить схему                   | `psql -U aicareer -d aicareer -f db/schema.sql`                                       |
| Собрать проект                    | `mvn clean package -DskipTests`                                                       |
| Запустить `AppRunner` через Maven | `mvn exec:java -Dexec.mainClass="com.aicareer.AppRunner"`                           |
| Запустить через JAR               | `java -jar target/AI-Career-5-1.0-SNAPSHOT.jar`                                      |
| Проверить OpenAI API              | `curl https://api.openai.com/v1/models -H "Authorization: Bearer $OPENAI_API_KEY"`  |

---

## Подготовка базы данных

### 1. Создать пользователя и БД

```sql
CREATE USER aicareer WITH PASSWORD 'aicareer';
CREATE DATABASE aicareer OWNER aicareer;
GRANT ALL PRIVILEGES ON DATABASE aicareer TO aicareer;
```

### 2. Применить схему

```bash
psql -U aicareer -d aicareer -f db/schema.sql
```

Пути к файлам (например, `db/schema.sql`, `db/test_users.sql`) могут отличаться в зависимости от структуры репозитория — при необходимости скорректируйте их.

---

## Настройка подключения

### Параметры PostgreSQL

По умолчанию используются параметры вида:

```
DB_URL=jdbc:postgresql://localhost:5432/aicareer
DB_USER=aicareer
DB_PASSWORD=aicareer
```

Их можно переопределить через переменные окружения или конфигурационные файлы (например, `application.properties` или конфиг-класс).

### Настройка OpenAI API

Модуль AI использует класс `OpenAIClient`:

```java
private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
private static final String DEFAULT_MODEL = System.getenv().getOrDefault(
        "OPENAI_MODEL",
        "gpt-4o-mini"
);
```

Ключ берётся из `Config.API_KEY`, поэтому важно, чтобы:

- `Config.API_KEY` читался из переменной окружения; или
- ключ был явно прописан (для продакшена так делать не рекомендуется).

Рекомендуемый вариант — завести переменные окружения:

```
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o-mini
```

и внутри `Config` читать их, например:

```java
public class Config {
    public static final String API_KEY = System.getenv("OPENAI_API_KEY");
}
```

---

## Установка и проверка OpenAI

1. **Получить API-ключ.** Зарегистрируйтесь в OpenAI, сохраните ключ в переменной окружения:

    ```bash
    export OPENAI_API_KEY="sk-ваш-ключ"
    export OPENAI_MODEL="gpt-4o-mini"
    ```

    (На Windows через `set` или в настройках IDE.)

2. **Проверить доступ к API.**

    ```bash
    curl https://api.openai.com/v1/models \
      -H "Authorization: Bearer $OPENAI_API_KEY"
    ```

    Если пришёл JSON-ответ со списком моделей — всё настроено корректно.

---

## Сборка и запуск проекта

1. **Сборка**

    ```bash
    mvn clean package -DskipTests
    ```

2. **Запуск через Maven**

    ```bash
    mvn exec:java -Dexec.mainClass="com.aicareer.AppRunner"
    ```

3. **Запуск через JAR**

    ```bash
    java -jar target/AI-Career-5-1.0-SNAPSHOT.jar
    ```

`AppRunner` — главный класс, который инициализирует конфигурацию, подключается к БД и запускает пайплайн (обновление вакансий, построение графа навыков, перерасчёт матриц, генерация рекомендаций).

---

## Как работает AI-модуль

1. **Данные пользователя → матрица навыков.**

    Анкета (JSON вида):

    ```json
    {
      "id": "test-user-1",
      "name": "Иван Петров",
      "skills": ["Java", "Spring Boot", "PostgreSQL", "Git", "Docker"],
      "desiredRoles": ["Java Developer", "Backend Developer"]
    }
    ```

    Конвертируется в бинарную матрицу (список навыков фиксирован, например, `skills.json` в ресурсах):

    ```json
    {
      "java": 1,
      "c++": 0,
      "python": 0,
      "javascript": 0,
      "sql": 1,
      "docker": 1,
      "c#": 0,
      "php": 0,
      "spring": 1,
      "machine_learning": 0
    }
    ```

2. **Вакансии по роли → матрица навыков роли.**

    - Из БД выбираются ~10 вакансий по каждой целевой роли.
    - Формируется промпт и отправляется в `OpenAIClient`:

      ```java
      OpenAIClient client = new OpenAIClient();
      String resultJson = client.generate(null, promptWithVacancies);
      ```

    - Модель `gpt-4o-mini` анализирует вакансии и возвращает только JSON с усреднёнными требованиями к навыкам роли (пример как выше).

3. **Глобальный граф навыков.**

    Строится на основе всех спарсенных вакансий:

    - узлы — навыки (например, `java`, `python`, `docker`);
    - рёбра — направленные связи между навыками с весами (частота совместной встречаемости, сила зависимости).

    Пример узлов:

    ```json
    {
      "nodes": [
        { "id": "java", "vacancies": 132 },
        { "id": "c++", "vacancies": 3 },
        { "id": "python", "vacancies": 380 },
        { "id": "javascript", "vacancies": 177 },
        { "id": "sql", "vacancies": 185 },
        { "id": "docker", "vacancies": 95 }
      ]
    }
    ```

    Пример рёбер:

    ```json
    {
      "edges": [
        { "from": "python", "to": "machine_learning", "weight": 0.82 },
        { "from": "java", "to": "spring", "weight": 0.90 },
        { "from": "linux", "to": "docker", "weight": 0.76 }
      ]
    }
    ```

    **Хранение графа в БД.**

    ```sql
    CREATE TABLE skill_graph (
        id         SERIAL PRIMARY KEY,
        graph_json JSONB      NOT NULL,
        created_at TIMESTAMP  NOT NULL DEFAULT now()
    );
    ```

4. **Сравнение пользователя и роли.**

    Сравниваются две матрицы: пользователя и целевой роли. Результат: совпадающие навыки, избыточные, недостающие и процент соответствия.

5. **Генерация маршрута развития (Learning Path).**

    Недостающие навыки и подграф из `skill_graph` передаются в модель `gpt-4o-mini`. Модель учитывает существующие навыки пользователя, граф зависимостей и примеры вакансий, возвращая структурированные рекомендации по веткам и порядку изучения навыков.

6. **Визуализация.**

    Backend формирует JSON для фронтенда:

    ```json
    {
      "nodes": [...],
      "edges": [...],
      "userSkills": ["java", "spring"],
      "missingSkills": ["docker", "machine_learning"]
    }
    ```

    Фронтенд может подсветить:

    - зелёным — навыки пользователя;
    - красным — недостающие навыки для роли;
    - серым — остальные.

---

## Тесты

```bash
mvn test
```

---

## Частые проблемы

1. **Model call failed: HTTP 401** — неверный или отсутствующий `OPENAI_API_KEY`. Проверьте переменные окружения и класс `Config`.
2. **Model call failed: HTTP 429 / 500** — лимиты OpenAI или временные ошибки. В `OpenAIClient` есть простой ретрай при коде 419, при необходимости добавьте обработку 429.
3. **Null / пустой ответ от модели** — модель вернула не JSON. Убедитесь, что промпт требует: «Return ONLY valid JSON…», и корректно обрабатывается `choices[0].message.content` в `OpenAIClient`.
4. **Ошибки подключения к БД** — проверьте `DB_URL`, `DB_USER`, `DB_PASSWORD`; убедитесь, что PostgreSQL запущен и слушает нужный порт.

---

## Полезные команды

```bash
# Создать БД и схему
psql -U postgres -f db/init.sql
psql -U aicareer -d aicareer -f db/schema.sql

# Запуск проекта
mvn exec:java -Dexec.mainClass="com.aicareer.AppRunner"

# Проверка OpenAI
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

---

## Команда проекта

Блок для заполнения позже

- **Ментор:** (ФИО, Telegram)
- **Команда:** (участники проекта)

---

## Лицензия

Тип лицензии определяется заказчиком проекта (ООО MWS / МФТИ).

---

Если нужно, можно дополнительно оформить README для модуля `aitransform`, добавить пример конкретного промпта или вынести подробное описание AI-модуля в `/docs/AI-module.md`.
