# PortfolioSync API

[![CI](https://github.com/AlmeidaSNicolas/portfoliosync-api/actions/workflows/ci.yml/badge.svg)](https://github.com/AlmeidaSNicolas/portfoliosync-api/actions/workflows/ci.yml)

Backend that keeps the projects section of my portfolio up to date on its own. When I push to a GitHub repository that has the `portfolio` topic and a `portfolio.json` file, a webhook tells this API, the API reads the repository's metadata and stores the project, and the portfolio site lists it in Spanish or English.

> 🚧 **Work in progress.** Sprints 0 to 2 are done: `GET /api/projects` lists the visible projects in Spanish or English from seed data, and a tested GitHub client reads and validates `portfolio.json`. The client is not connected to the API yet: the webhook and the sync arrive in Sprint 3. See the [roadmap](#roadmap).

## How it works

```mermaid
sequenceDiagram
    participant Dev as Developer (git push)
    participant GH as GitHub
    participant API as PortfolioSync API
    participant DB as PostgreSQL
    participant Site as Portfolio (Next.js)
    Dev->>GH: push to a repo with the "portfolio" topic
    GH->>API: POST /webhooks/github (HMAC-signed)
    API->>API: verify signature + idempotency check
    API->>GH: GET portfolio.json + repo metadata
    API->>DB: upsert project
    Site->>API: GET /api/projects (ISR, every hour)
    API-->>Site: projects in ES/EN
```

## Repository convention

A repository shows up on the portfolio when it has both:

1. The `portfolio` **topic** on GitHub.
2. A `portfolio.json` file at its root:

```json
{
  "title": { "es": "Bot de curaduría geopolítica", "en": "Geopolitical news curation bot" },
  "description": { "es": "Java puro, sin frameworks…", "en": "Plain Java, no frameworks…" },
  "stack": ["Java", "PostgreSQL", "Docker"],
  "demoUrl": "https://t.me/geopoliticaNews",
  "featured": true,
  "order": 1
}
```

`title` and `description` are required in both languages; the other fields are optional.

## Data model

```mermaid
erDiagram
    projects |o..o{ webhook_deliveries : "logical reference by github_repo_id (no FK)"

    projects {
        bigint id PK
        bigint github_repo_id UK "stable GitHub id, survives renames"
        varchar repo_name UK
        varchar html_url
        jsonb title "es + en required (CHECK)"
        jsonb description "es + en required (CHECK)"
        jsonb stack "array, defaults to empty"
        varchar demo_url "nullable"
        boolean featured "default false"
        int display_order "nullable, nulls sort last"
        boolean visible "default true"
        timestamptz pushed_at
        timestamptz synced_at
        text last_error "nullable"
        timestamptz created_at
    }
    webhook_deliveries {
        bigint id PK
        uuid delivery_id UK "X-GitHub-Delivery header"
        varchar event "push | ping"
        bigint github_repo_id "nullable"
        varchar repo_name "nullable"
        varchar status "RECEIVED | PROCESSED | IGNORED | FAILED"
        text error_message "nullable"
        timestamptz received_at
        timestamptz processed_at "nullable"
    }
```

Key decisions:

- **`github_repo_id` as the upsert key**, not `repo_name`: the name changes when a repository is renamed, the GitHub id never does.
- **`jsonb` for translated text and stack**: these values are always read together with the project, so there is no join, one row carries both languages, and adding a language needs no migration. `CHECK` constraints enforce that both `es` and `en` exist.
- **Hide instead of delete**: a repository that loses the topic gets `visible = false`, so it comes back with its history if the topic returns.
- **Idempotent webhooks**: each delivery is inserted first; the `UNIQUE` constraint on `delivery_id` rejects GitHub retries atomically, then the delivery is processed and its status updated.
- **`webhook_deliveries` has no foreign key**: it is a log, and it also records deliveries for repositories that never became projects.

## API

### `GET /api/projects`

Lists the visible projects, with the translated fields in one language.

| Query parameter | Values | Default |
| --- | --- | --- |
| `lang` | `es`, `en` | `es` |

```bash
curl "http://localhost:8080/api/projects?lang=en"
```

```json
[
  {
    "repoName": "geopolitica-news-bot",
    "title": "Geopolitical news curation bot",
    "description": "Plain Java, no frameworks…",
    "stack": ["Java", "PostgreSQL"],
    "htmlUrl": "https://github.com/AlmeidaSNicolas/geopolitica-news-bot",
    "demoUrl": null,
    "featured": true
  }
]
```

The response is a DTO (`ProjectResponse`), never the JPA entity: internal fields such as `lastError` or `syncedAt` stay inside the API, and the JSON does not change when the table does.

An unsupported language returns `400 Bad Request` as an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem detail, produced by a global `@RestControllerAdvice`:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Unsupported language: pt",
  "instance": "/api/projects"
}
```

Interactive docs (springdoc): <http://localhost:8080/swagger-ui.html>

## GitHub client

`GithubClient` takes a repository name and returns its `portfolio.json` as a `PortfolioFile` record:

1. It calls the [repository contents endpoint](https://docs.github.com/en/rest/repos/contents) with Spring's `RestClient`.
2. It decodes the `content` field of the response, which GitHub sends in Base64.
3. It maps the decoded JSON to `PortfolioFile` with Jackson.

Key decisions:

- **Contents API instead of `download_url`**: raw file URLs are cached for a few minutes after a push, so a webhook could read a stale file.
- **Own exception for a 404**: `PortfolioFileNotFoundException` replaces Spring's `HttpClientErrorException.NotFound` and keeps it as the cause. GitHub answers 404 both for a missing repository and for a missing file, so the name covers both.
- **Validation is a separate step**: the client only fetches. `PortfolioValidator` requires `title` and `description` in both `es` and `en`, stops at the first problem and throws `InvalidPortfolioFileException`. The sync service will call it in Sprint 3 and store the error in `last_error`.
- **Base URL in configuration**: it comes from `github.api.base-url`, so tests can point the client at a local server.

`GithubClientTest` uses [WireMock](https://wiremock.org/) to run a real HTTP server on a random port in place of GitHub. It covers a successful read and a 404, and needs neither Docker nor internet access.

## Project structure

Packages are split by feature first, then by layer inside each feature, so every feature keeps its own boundary and the layers stay visible. A feature talks to another one only through its service.

```
io.github.almeidasnicolas.portfoliosync
├── project/
│   ├── controller/   ProjectController (GET /api/projects)
│   ├── service/      ProjectService (entity → DTO, language selection)
│   ├── repository/   ProjectRepository (Spring Data JPA)
│   ├── dto/          ProjectResponse
│   ├── domain/       Project (entity), LocalizedText (es/en value)
│   └── seed/         ProjectSeeder (sample data, "dev" profile only)
├── github/           GithubClient, PortfolioValidator, PortfolioFile, ContentDTO, exceptions
└── shared/
    └── exception/    GlobalExceptionHandler, UnsupportedLanguageException
```

## Tech stack

Java 21 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA (Hibernate) · PostgreSQL 17 · Flyway · springdoc-openapi · Actuator · Lombok · JUnit 5 · Testcontainers · WireMock · Docker Compose · Maven · GitHub Actions (CI)

Planned: Bean Validation, Spring Security (admin endpoints), CD with GitHub Actions, deploy to Render + Neon.

## Database migrations

The schema is owned by Flyway. Migrations live in `src/main/resources/db/migration` and run when the app starts; Hibernate runs with `ddl-auto=validate`, so it only checks that the entities match the tables and never changes the schema. A migration that has already been applied is never edited: any change goes in a new `V<n>__` file.

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `./mvnw verify` on every push to `main` and every pull request targeting `main`, using Temurin 21 with the Maven dependency cache. The integration tests start PostgreSQL 17 through Testcontainers, so CI needs no database configuration.

## Running locally

Requirements: Java 21 and Docker.

```bash
docker compose up -d   # starts PostgreSQL 17 on localhost:5432
./mvnw spring-boot:run # runs Flyway migrations and starts the API on :8080
```

Health check: <http://localhost:8080/actuator/health>

To start with sample projects, run with the `dev` profile. `ProjectSeeder` inserts three projects when the table is empty and never runs in other environments:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Configuration comes from environment variables, with local defaults (the database ones match `compose.yaml`):

| Variable | Local default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/portfoliosync` |
| `DB_USERNAME` | `portfoliosync` |
| `DB_PASSWORD` | `portfoliosync` |
| `GITHUB_API_URL` | `https://api.github.com` |

Run the tests (Testcontainers starts its own PostgreSQL, so Docker must be running):

```bash
./mvnw verify
```

## Roadmap

- [x] **Sprint 0 — Setup:** project skeleton, Docker Compose, Flyway, README with data model, CI running `./mvnw verify`
- [x] **Sprint 1 — Projects:** entity, `GET /api/projects` with seed data, per-language DTOs, error handling
- [x] **Sprint 2 — GitHub client:** read `portfolio.json`, JSON validation, WireMock tests
- [ ] **Sprint 3 — Webhook and sync:** HMAC signature check, idempotency by delivery id, read repository topics, store projects and `last_error`
- [ ] **Sprint 4 — Admin and ordering:** token-protected manual resync, listing order
- [ ] **Sprint 5 — Deploy:** Dockerfile, Render + Neon, CD with GitHub Actions
- [ ] **Sprint 6 — Portfolio:** Next.js site consuming the API with ISR and a fallback
