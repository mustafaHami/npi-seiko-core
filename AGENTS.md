# Repository Guidelines

## Project Structure & Module Organization

This repository is a multi-module Maven project:

- `core/`: Spring Boot backend (controllers, services, repositories, security, config).
- `swagger/`: OpenAPI-driven model generation from `swagger/src/main/resources/swagger-npi-seiko.yaml`.
- `docs/`: SQL and implementation notes (`database-updates.sql`, `deployment.sql`).
- `db/migrations/`: database migration artifacts.
- Root infra files: `compose.yaml`, `Dockerfile`, `deploy-image.sh`.

Main Java code lives under `core/src/main/java/my/lokalix/planning/core`, with resources in `core/src/main/resources`.

## Build, Test, and Development Commands

- `mvn clean verify`: Build all modules and run tests.
- `docker compose up -d database`: Start local PostgreSQL dependency.
- `docker build -t planning-core-image:1.0.0 .`: Build backend container image.

Use `compose.yaml` for local service wiring and `deploy-image.sh <image:tag>` for image packaging/transfer.

## Coding Style & Naming Conventions

- Language: Java 25, Spring Boot 4.
- Follow existing style: 2-space indentation, Lombok for boilerplate reduction, MapStruct for mappings.
- Packages are lowercase (`...core.services`), classes are `PascalCase`, methods/fields are `camelCase`.
- Prefer clear suffixes by layer: `*Controller`, `*Service`, `*Repository`, `*Entity`, `*Mapper`.
-

Naming pattern: `<ClassName>Test` for unit tests and `<FeatureName>IT` for integration tests. Always run `mvn test`
before opening a PR.

## Commit & Pull Request Guidelines

Recent history favors short, imperative commit subjects (for example: `add new api for get status history`,
`update swagger`, `fix rework indexation...`).

- Keep commits focused to one concern.
- Mention affected module when useful (`core`, `swagger`, `docs`).
- PRs should include: purpose, key changes, local verification steps, DB/script impact, and API/spec changes when
  applicable.

## Security & Configuration Tips

`core/src/main/resources/application-dev.properties` contains environment-specific values. Do not commit real secrets or
production credentials; prefer environment variables or secret management for sensitive keys.

## Additional Mandatory Rules (From CLAUDE.md)

- Treat these as non-negotiable project conventions.
- MapStruct mappers must be `abstract class`es (never interfaces), with Spring component model.
- New entities must use UUID IDs initialized with `UUID.randomUUID()`, plus `@Setter(AccessLevel.NONE)` and
  `@EqualsAndHashCode(of = "entityId")`.
- Use `@NotBlank` for required `String` fields (not `@NotNull`), and initialize creation timestamps with
  `TimeUtils.nowOffsetDateTimeUTC()`.
- `@ManyToOne` relations must use `fetch = FetchType.LAZY`.
- `BigDecimal` fields must declare precision/scale: `@Column(precision = 25, scale = 6)` (and `nullable = false` when
  required).
- Endpoint security must be explicit: use `@Secured(...)` for restricted roles; no `@Secured` means any authenticated
  user.
- For business errors, use `GenericWithMessageException` (except standard existence cases with
  `EntityNotFoundException` / `EntityExistsException`).
- For each new entity, add retrieval helpers in `core/services/helper/EntityRetrievalHelper.java` (
  `getMustExist<Entity>ById(...)` pattern).
- Put business validation logic in dedicated `services/validator/*Validator` classes, not private service methods.
- DTO/entity mapping must go through MapStruct; avoid manual field-by-field mapping in controllers/services.
- API contract changes start in `swagger/src/main/resources/swagger-npi-seiko.yaml`, then regenerate models (
  `mvn clean install -pl swagger`); never hand-edit generated `SW*` classes.
