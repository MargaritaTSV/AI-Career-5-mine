# AI-Career-5 — Руководство по запуску

Кратко: этот проект извлекает матрицу навыков из JSON с вакансиями. Для извлечения по умолчанию используется локальная копия модели через сервис Ollama (HTTP API на порту 11434). По требованию Ollama должен запускаться через Docker — проект настроен так, чтобы разрешать подключение только к локальным адресам (localhost, 127.0.0.1, 0.0.0.0, host.docker.internal) и порту 11434.

Этот README объясняет, как запустить Ollama в Docker, как собрать и запустить приложение, и как контролировать поведение fallback (локальная стратегия извлечения навыков).

Содержание
- Требования
- Запуск Postgres и Ollama через Docker (docker-compose)
- Переменные окружения
- Сборка и запуск приложения
- Поведение при недоступном Ollama (fallback)
- Отладка и проверка
- Troubleshooting (частые ошибки и их причины)
- Рекомендации по безопасности
- Автоматическая загрузка моделей Ollama

---

Требования
- Java 17 (в проекте используется целевой релиз 17)
- Maven (3.x)
- Docker (для запуска Ollama и Postgres)

Запуск Postgres и Ollama через Docker (docker-compose)

1) Поднимите сервисы (Postgres и опционально Ollama):

```bash
cd /path/to/AI-Career-5-mine
# Поднимет Postgres. Если в docker-compose указан сервис ollama и вы настроили образ — он тоже поднимется
docker-compose up -d
```

2) Убедитесь, что Postgres готов к подключению (healthcheck в docker-compose):

```bash
docker ps --filter "name=aicareer-postgres" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# Просмотреть логи, чтобы убедиться в инициализации
docker logs aicareer-postgres --tail 50
```

3) Если вы не используете `docker-compose` для Ollama, запустите Ollama вручную (замените `<OLLAMA_IMAGE>`):

```bash
# пример запуска Ollama вручную (поменяйте на ваш образ/команду)
docker run --rm -p 11434:11434 --name ollama <OLLAMA_IMAGE>
```

Переменные окружения
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` — используются для подключения к Postgres. По умолчанию `DB_HOST=postgres` (под docker-compose) и `DB_PORT=5432`.
- `OLLAMA_HOST` — URL сервиса Ollama, по умолчанию `http://localhost:11434`.
- `OLLAMA_MODEL` — имя модели в Ollama (опционально).
- `ALLOW_LOCAL_FALLBACK` — если `true`, при недоступном Ollama используется локальный keyword-search; по умолчанию `false`.

Сборка и запуск приложения

1) Сборка:

```bash
mvn -DskipTests package
```

2) Запуск Demo (пример запуска `DemoMain`):

```bash
# Пример, если вы подняли Postgres через docker-compose и Ollama доступен на localhost
OLLAMA_HOST=http://localhost:11434 mvn -Dexec.mainClass=org.example.DemoMain exec:java

# Если хотите разрешить локальный fallback (при недоступном Ollama)
ALLOW_LOCAL_FALLBACK=true OLLAMA_HOST=http://localhost:11434 mvn -Dexec.mainClass=org.example.DemoMain exec:java
```

Если вы запускаете приложение локально без docker-compose, вы можете задать `DB_HOST=localhost`:

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=aicareer DB_USER=aicareer DB_PASSWORD=aicareer mvn -Dexec.mainClass=org.example.DemoMain exec:java
```

Поведение при недоступном Ollama (fallback)
- Если `ALLOW_LOCAL_FALLBACK=false` (по умолчанию): при ошибке подключения к Ollama приложение завершится с исключением и подсказкой.
- Если `ALLOW_LOCAL_FALLBACK=true`: приложение логирует предупреждение и возвращает локальную матрицу навыков (keyword-search).

Отладка и проверка

- Проверка Ollama:
```bash
curl -v http://localhost:11434/v1/models
```

- Проверка Postgres:
```bash
docker ps --filter "name=aicareer-postgres" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# или если запускаете локально
lsof -iTCP:5432 -sTCP:LISTEN
```

Troubleshooting
- Ошибка `UnknownHostException: postgres` означает, что среда не видит DNS-имя `postgres` — это нормально вне Docker. В таком случае либо запустите `docker-compose up -d` (чтобы хост `postgres` появился в сетевом пространстве Docker), либо при запуске на хосте укажите `DB_HOST=localhost`.

Рекомендации по безопасности
- Обновите зависимость PostgreSQL до версии, где исправлен известный CVE, прежде чем запускать в production.

Автоматическая загрузка моделей Ollama

Чтобы автоматически загрузить (pull) модель внутрь контейнера Ollama и сохранить её между перезапусками, в `docker-compose.yaml` добавлен одноразовый сервис `ollama-init` и bind-mount `./ollama-data`.

Процедура (рекомендуемая, без использования shell-скриптов):

```bash
cd /Users/smolevanataliia/AI-Career-5-mine
# 1) Поднять Postgres (если ещё не поднят)
docker-compose up -d postgres

# 2) Запустить init-контейнер одноразово, чтобы скачать модель в ./ollama-data
docker-compose run --rm ollama-init

# 3) После успешного завершения запустить основной сервис ollama
docker-compose up -d ollama

# 4) Проверить модели
curl -sS http://localhost:11434/v1/models | jq .
```

Если вы предпочитаете альтернативный подход, можно вручную выполнить команду pull, монтируя volume:

```bash
# скачивает модель внутрь ./ollama-data
docker run --rm -v "$(pwd)/ollama-data:/root/.ollama" ollama/ollama:latest /usr/bin/ollama pull deepseek-r1:8b
```

---
Автор: изменения по коду сделал помощник.
