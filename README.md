# Eurder

Back-end REST API for Örder, an order management application (Switchfully self-evaluation project).
Built as part of the Switchfully Java track.

## Tech stack

- Java 17, Spring Boot 4.1, Maven
- Spring Data JPA + PostgreSQL, schema managed by Flyway (`src/main/resources/db/migration`)
- H2 in-memory database for the tests
- OpenAPI / Swagger UI (springdoc)
- JUnit 5, AssertJ, Mockito, RestAssured, JaCoCo (≥ 80 % line coverage)

## Build tool

The project ships with the Maven wrapper (`mvnw` for Linux/macOS/Git Bash, `mvnw.cmd` for Windows).
Use it instead of a locally installed `mvn`: it downloads and runs the Maven version pinned in
`.mvn/wrapper/maven-wrapper.properties`, so local builds and CI use exactly the same version. Only Java 17+ is needed.

Maven is pinned to **3.9.16**: IntelliJ IDEA 2026.2 cannot import the project with Maven 3.10.0
(`Unable to create injector ... ArtifactHandlerManager`). Bump the version in that file once IntelliJ supports it.

## Run it

1. Start PostgreSQL with a database named `eurder`
2. Copy `.env.example` to `.env` and fill in your database URL and credentials
   (environment variables with the same names take precedence over `.env`)
3. `./mvnw spring-boot:run`
4. Open the API documentation: http://localhost:8080/docs

## Default admin

The Flyway migration `V2__insert_default_admin.sql` creates an administrator account:

| Email | Password | Role |
| --- | --- | --- |
| `admin@eurder.be` | `admin` | `ADMIN` |

The API uses HTTP Basic authentication: send `Authorization: Basic base64(email:password)` with every request,
or use the **Authorize** button in Swagger UI. Change this password before any real deployment.

## Test it

```
./mvnw verify
```

Tests run on in-memory H2 with the same Flyway scripts: no database needed.

## Stories

| Story | Description | Status |
| --- | --- | --- |
| 0 | Project setup | Done |
| T | Persistence: JPA, PostgreSQL, H2, transactions (technical) | Done |
| S | Security: roles, features, Basic authentication (technical) | Done |
| 1 | Create a customer account | Done |
| 2 | Add an item | Done |
| 3 | Order items | Done |
| 7 | View all customers | Done |
| 8 | View a single customer | Done |
| 4 | Update an item | Done |
| 5 | View report of orders | Nice to have |
| 6 | Reorder an existing order | Nice to have |
| 9 | Items shipping today | Nice to have |
| 10 | Item overview | Done |
| C | Required configuration validated at startup (technical) | To do |

## Ordering rules

- An item group ships the next day when the stock covers its whole amount, otherwise one week later.
- Item groups are handled in the order they are sent: ordering the same item twice, the second group sees the stock left by the first.
- The stock never goes below zero.
- An item group copies the name and price of the item, so later price changes do not affect past eurders.
- Two eurders changing the same item at the same time: the last one to commit gets a `409` and can retry (optimistic locking with `@Version`).

### Known limitations

The stock does not keep track of the units still owed to customers. Ordering 10 items with 5 in stock ships the whole item group a week later and sets the stock to 0: the 5 missing units are only visible in the item groups waiting to be shipped (Story 9).

Updating an item replaces all of its fields. Two admins updating the same item at the same time: the last one wins, since the request does not carry the version of the item it was based on.
