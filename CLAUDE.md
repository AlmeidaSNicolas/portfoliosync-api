# PortfolioSync API

Spring Boot API that keeps my portfolio's projects in sync with GitHub through webhooks. Full detail (data model, API, decisions) is in `README.md`: read it before asking anything about the project.

## Study mode (read first)

- Nicolas writes the code. Guide and review; do not write or paste implementation into his files unless he explicitly asks ("show me", "write it", "give me the code").
- Hints in levels: concept, then where to look, then the structure. When reviewing, point at the problem with a question before giving the answer.
- Before asking about the project, read `README.md`, `pom.xml`, `git log` and the tests. Only ask what they do not answer.
- Explain the why of each decision (annotation, layer, constraint) as if preparing him for an interview.

## Stack and commands

Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, PostgreSQL 17, Flyway, Lombok, JUnit 5, Mockito, Testcontainers, WireMock, Maven.

```bash
docker compose up -d                                           # PostgreSQL on :5432
GITHUB_WEBHOOK_SECRET=dev-secret-local ./mvnw spring-boot:run  # API on :8080 (add SPRING_PROFILES_ACTIVE=dev for seed data)
./mvnw verify                                                  # tests; Docker must be running (Testcontainers)
```

`GITHUB_WEBHOOK_SECRET` has no default on purpose: the app must not start without it.

## Structure and rules

Package by feature, then by layer: `project`, `github`, `webhook`, `sync`, `shared` under `io.github.almeidasnicolas.portfoliosync`. A feature talks to another only through its service (`SyncService` never touches `ProjectRepository`).

- Controllers return DTOs, never JPA entities.
- Errors go through `GlobalExceptionHandler` as RFC 9457 problem details; use the project's own exceptions, not Spring's.
- Schema is owned by Flyway (`src/main/resources/db/migration`); Hibernate stays on `ddl-auto=validate`.
- Tests: WireMock for GitHub calls, Testcontainers for database tests, Mockito for collaborators. Cover the failure path, not only the happy one.
- Commits follow Conventional Commits in English (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`); work goes through PRs into `main`, and CI runs `./mvnw verify`.

## Decisions already made (do not reopen without a reason)

- Upsert key is `github_repo_id`, not `repo_name` (renames).
- Translated text and stack are `jsonb`, with `CHECK` constraints for `es` and `en`.
- Projects that lose the topic are hidden (`visible = false`), never deleted.
- Webhooks are idempotent: insert first, `UNIQUE` on `delivery_id` rejects retries.
- HMAC is computed over the raw body (`byte[]`) and compared in constant time; a missing signature is a 401.
- `portfolio.json` is read through the Contents API, not `download_url` (cache).

## Current state (update at the end of each session)

Updated 2026-11-11 (end of the evening session). Working branch: `sprint-3-webhook` (not `main`), PR #3 still a draft. Sprints 0 to 2 done; Sprint 3 has one item left.

Done and committed (last commits `d7157e4`, `09af4ed`): HMAC check, `POST /webhooks/github`, idempotency by delivery id, repository metadata and topics, sync create/update, hide on lost topic, hide + `last_error` + rethrow on a missing or invalid `portfolio.json`, tests for all of that (`SyncServiceTest`, 7 tests), README and PR checklist.

Last item of Sprint 3, in progress and uncommitted:

- Created: `webhook/dto/GithubPushBody(repository)`, `webhook/dto/GithubPushRepository(id, name)`, `webhook/exception/InvalidWebhookPayloadFormatException`, `webhook/service/ExtractDataFromGithubPush` (reads the `byte[]` body with `JsonMapper`, rejects a missing `repository.name`) and a 400 handler for the new exception in `GlobalExceptionHandler`. No tests yet; names in `ExtractDataFromGithubPush` (class and method `dataRetur`) are still placeholders he may rename.
- Still to do: wire it into `WebhookController` after the HMAC check (only `push` runs the sync; `ping` does not), handle a repeated delivery (`registerIfNew` returns `false`; do not sync again), call `SyncService.sync`, set the delivery status (`PROCESSED`, `FAILED` when the sync throws, `IGNORED`), add methods to `WebhookDelivery` for status, `repoName`, `githubRepoId`, `processedAt`, `errorMessage`, tests (failure paths too), README roadmap.
- Next session starts here: Nicolas writes the ordered steps of `receiveDelivery` in words (repeated delivery, `ping`, `push`) and Claude reviews the order before any code.

Known loose ends: CI runs for PR #3 stuck in `queued` for 2h+ (GitHub queue, not the code; re-run if still stuck); `.DS_Store` was committed by mistake in `d7157e4` (remove with `git rm --cached` and add to `.gitignore`); a malformed `portfolio.json` makes Jackson throw an exception that `SyncService` does not catch; `GlobalExceptionHandler` in `shared` now imports a `webhook` exception (interview question: what happens when it imports one per feature?).

Next after Sprint 3: Sprint 4 (token-protected manual resync, listing order).
