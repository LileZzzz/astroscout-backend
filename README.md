# AstroScout Backend

Spring Boot REST API for AstroScout (observation logs, feed, auth, likes, comments).

## What this backend does

- Authentication and user profile APIs (JWT).
- Observation log CRUD + community feed + interactions.
- Observation planning APIs:
   - score breakdown
   - visible targets
   - best observing windows
- AI assistant API:
   - general astronomy Q&A (model knowledge)
   - observation planning answers with hydrated weather/score/context
- Image upload and static file serving for log cover images.
- Basic metrics via Actuator + Micrometer counters/timers.

## Core endpoints

- Auth: /api/auth/register, /api/auth/login, /api/auth/demo
- Profile: /api/users/me, /api/users/me/profile, /api/users/me/password
- Observe: /api/observe/score, /api/observe/celestial, /api/observe/best-window
- AI: /api/ai/chat
- Logs: /api/logs, /api/logs/{id}, /api/feed
- Upload: /api/uploads/image, /uploads/{filename}

## Run locally

1. Start PostgreSQL (e.g. Docker) with database `astroscout`.
2. Set datasource via environment variables (or edit `src/main/resources/application.yml`) for `spring.datasource.url`, `username`, `password`.
3. Run:

   ```bash
   mvn spring-boot:run
   ```

   Server listens on port 8081.

## Quick start (recommended local flow)

1. Start Postgres and Redis.
2. Start backend with Gemini env:

```bash
set -a
source .env.gemini.local
set +a
./mvnw spring-boot:run
```

3. Optional compile check:

```bash
./mvnw -q -DskipTests compile
```

## Reset and seed database

To wipe existing data and load sample users, logs, likes, and comments:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

Or with env:

```bash
export SPRING_PROFILES_ACTIVE=seed
mvn spring-boot:run
```

Seed users (password for all: `password`):

- CelesteVega, OrionNebula, LunaStargazer, SiriusObserver, AndromedaSky (USER)
- AstroAdmin (ADMIN) — email `admin@astroscout.example.com`

After seeding, run the app again without the `seed` profile for normal use.

### Seeding when using Docker Compose

If the full stack runs via `docker compose up`, the backend container holds port 8081. To seed into the **same** Postgres used by Docker:

1. Free the port: `docker compose stop backend` (from repo root).
2. From `astroscout-backend/`, run the seed with DB pointing at host Postgres (Docker exposes 5432):
   ```bash
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/astroscout \
   SPRING_DATASOURCE_USERNAME=postgres \
   SPRING_DATASOURCE_PASSWORD=postgres \
   mvn spring-boot:run -Dspring-boot.run.profiles=seed
   ```
3. Restart the backend container: `docker compose up backend -d`.

## AI configuration (Gemini only)

This backend uses Spring AI OpenAI-compatible integration with Gemini AI Studio.
Only the Gemini path is documented and supported here.

Required environment variables:

- `ASTROSCOUT_OPENAI_API_KEY` = your Gemini AI Studio API key
- `ASTROSCOUT_OPENAI_BASE_URL` = `https://generativelanguage.googleapis.com/v1beta/openai`
- `ASTROSCOUT_OPENAI_COMPLETIONS_PATH` = `/chat/completions`
- `ASTROSCOUT_OPENAI_MODEL` = `gemini-2.5-flash` (recommended)

Run with local env file:

```bash
set -a
source .env.gemini.local
set +a
./mvnw spring-boot:run
```

Quick diagnostics:

```bash
curl -sS https://generativelanguage.googleapis.com/v1beta/openai/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ASTROSCOUT_OPENAI_API_KEY" \
   -d '{"model":"gemini-2.5-flash","messages":[{"role":"user","content":"hello"}]}'
```

If you get `401`, verify AI Studio key validity and that the key is loaded in the current shell.
If you get `404`, verify endpoint/model name.
If you get `429`, your AI Studio project quota is exhausted for that model.

You can also use `src/main/resources/application-local.yml` as a local template (without committing real secrets).

## AI assistant request contract

The AI assistant answers astronomy and observation-planning questions using the model's general astronomy knowledge.
When planner context is available, the backend hydrates weather/score/moon/light-pollution/targets/windows and injects that context automatically.
It does not rely on a catalog RAG prompt anymore.

Endpoint:

- `POST /api/ai/chat`

Required field:

- `message`

Optional planner context fields (recommended for practical answers):

- `lat`
- `lng`
- `date`
- `score`
- `weatherSummary`
- `moonPhaseLabel`
- `bortleScale`
- `targetSummary`
- `bestWindowSummary`

Example minimal request:

```bash
curl -sS http://localhost:8081/api/ai/chat \
   -H "Content-Type: application/json" \
   -d '{"message":"What is Mars and why does it look red?"}'
```

Example context-rich request:

```bash
curl -sS http://localhost:8081/api/ai/chat \
   -H "Content-Type: application/json" \
   -d '{
      "message":"Should I observe tonight?",
      "lat":42.36,
      "lng":-71.06,
      "date":"2026-03-15",
      "score":78,
      "weatherSummary":"cloud=20%, visibility=15km, humidity=58%, wind=3.2m/s",
      "moonPhaseLabel":"Waxing Gibbous",
      "bortleScale":6,
      "targetSummary":"Jupiter (planet, alt 46°, mag -2.1); M31 (galaxy, alt 38°, mag 3.4)"
   }'
```

## Metrics and benchmark guidance

### Is cloud benchmark reasonable?

Yes, but do it in two layers:

- Layer A (local deterministic baseline): service + cache + DB behavior.
- Layer B (cloud realistic baseline): network/TLS/instance/storage effects.

Cloud-only benchmark without local baseline is hard to interpret.

### What usually brings improvement?

- Redis cache hit rate increase.
- API call reduction to weather/light services.
- Better DB indexing and query shape.
- Lower serialization and payload overhead.
- Better JVM/container sizing and warmup.

For /api/ai/chat specifically:

- prompt size control (smaller prompt => lower latency/cost)
- fewer upstream retries/timeouts
- model selection and quota strategy

### Gemini quota-limited metrics strategy (20 calls/day)

When model calls are scarce, split metrics into:

- Non-AI metrics (large sample): /api/observe/score, /api/feed, cache hit/miss.
- AI metrics (small sample): fixed 10-20 scenario set, not random load test.

Recommended AI test design:

1. Prepare fixed prompts:
- 5 general astronomy questions
- 5 planning questions with lat/lng/date
- 3 missing-context questions

2. Run each once per model/day and record:
- latency
- status code
- answer quality tag (pass/fail/manual)

3. Compare models by median latency + pass rate, not QPS.

This gives credible portfolio evidence even with low daily quota.

### Existing metrics endpoints

```bash
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/metrics/astroscout.weather.cache.hit
curl http://localhost:8081/actuator/metrics/astroscout.weather.cache.miss
curl http://localhost:8081/actuator/metrics/astroscout.weather.fetch
```

### Observe score benchmark script

From workspace root:

```bash
bash scripts/benchmark_observe_score.sh
```

## Phase-2 image upload and profile APIs

- `POST /api/uploads/image` (authenticated): multipart image upload, returns absolute URL.
- `GET /uploads/{filename}` (public): serve uploaded image files from local upload directory.
- `GET /api/users/me` (authenticated): fetch profile data.
- `PUT /api/users/me/profile` (authenticated): update username and avatar URL.
- `PUT /api/users/me/password` (authenticated): change password with current-password check.
