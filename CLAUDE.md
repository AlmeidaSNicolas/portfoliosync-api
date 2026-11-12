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

Updated 2026-11-12 (end of the day session). Sprints 0 to 3 are done and merged: PR #3 was merged into `main` with a merge commit (`88c14c0`, all 24 branch commits kept; branch `sprint-3-webhook` deleted). `main` is the working base again; Sprint 4 starts from a new branch.

What the webhook flow does now (`WebhookController.receiveDelivery`): HMAC (401) → `registerIfNew` (repeated: 200 and stop) → `ping`: `markIgnored` and 200 → anything else: inside one `try`, `payloadParser.parse(body)`, `recordRepository`, `syncService.sync(name)`, `markProcessed`; `catch (RuntimeException)`: `markFailed(deliveryId, message)` and rethrow.

Who wrote what in the final stretch (be honest about it in interviews): Nicolas wrote the entity transitions, `findByDeliveryId`, the `ping` branch, the new constructor fields and the `try` with parse/record/sync/markProcessed. Claude wrote, at his explicit request: the four `WebhookDeliveryService` methods and `WebhookDeliveryNotFoundException`, the `catch` block, the `ping`/`push` tests in `WebhookControllerTest` and the new tests in `WebhookDeliveryServiceTest`. He did not yet explain the `@Transactional`/dirty-checking questions in his own words (he skipped them).

Decisions taken: a repeated delivery returns 200; `ping` is `IGNORED`; GitHub does not retry on its own (a redelivery is manual and reuses the same `X-GitHub-Delivery`); a `FAILED` delivery is never reprocessed (Sprint 4's manual resync covers transient failures); the `catch` takes `RuntimeException` so no failure leaves a row in `RECEIVED`; the parse sits inside the `try`, so a broken body also closes the delivery as `FAILED`.

Known loose ends (not blockers):
- Only `ping` is ignored explicitly; an unknown event such as `issues` goes through the `push` path (probably one condition: anything that is not `push` is ignored).
- Broken JSON still reaches the client as a 500 (Jackson's exception escapes the parser); a missing `repository.name` is a 400.
- `repository.id` is not validated, but the upsert key is `github_repo_id`.
- A malformed `portfolio.json` makes Jackson throw an exception that `SyncService` does not catch (it is not hidden or recorded as `last_error`).
- `GlobalExceptionHandler` in `shared` imports a `webhook` exception (interview question: what happens when it imports one per feature?).
- `WebhookDelivery` has an unused `ResponseEntity` import (a domain entity must not depend on Spring Web).
- `shouldReturn200WhenSignatureIsValid` passes only because the Mockito default for `registerIfNew` is `false`; make the stub explicit.
- Two old CI runs stuck in `queued` for 25h+ (`43098351744`, `43097443294`): GitHub refuses both `gh run cancel` and `force-cancel` on them (stale state on GitHub's side, harmless; try again later or ignore).
- Not written: plain unit tests of the entity methods and a `findByDeliveryId` test (the service tests already cover their effect).

Next: Sprint 4 (token-protected manual resync, listing order).
