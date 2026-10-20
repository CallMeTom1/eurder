# Eurder

Back-end REST API for Örder, an order management application (Switchfully self-evaluation project).
Built as part of the Switchfully Java track.

## Tech stack

- Java 17, Spring Boot 4.1, Maven
- Spring Data JPA + PostgreSQL, schema managed by Flyway (`src/main/resources/db/migration`)
- H2 in-memory database for the tests
- Spring Security: HTTP Basic authentication, each endpoint authorized by a feature of the role (`@Secured`)
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
4. Open the API documentation: http://localhost:8080/docs. Each operation states the feature and role it requires, its response codes with the `ProblemDetail` error format, and an example request.

## Configuration

The application reads three required keys, with no default value:

| Key | Example |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/eurder` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | your database password |

- They come from `.env` (a copy of `.env.example`, never committed) or from environment variables, which take precedence over `.env`.
- `.env` uses the `.properties` format: write `DB_PASSWORD=abc`, without quotes.
- A missing or blank key stops the startup with an `APPLICATION FAILED TO START` report that lists every missing key at once, without ever showing a value.
- The tests do not need `.env`: they define the keys for their in-memory H2 database.

## Default admin

The Flyway migration `V2__insert_default_admin.sql` creates an administrator account:

| Email | Password | Role |
| --- | --- | --- |
| `admin@eurder.be` | `admin` | `ADMIN` |

The API uses HTTP Basic authentication: send `Authorization: Basic base64(email:password)` with every request,
or use the **Authorize** button in Swagger UI. Change this password before any real deployment.

Only the account creation (`POST /customers`) and the documentation are public. Every other request is authenticated before it reaches the API: a missing or wrong `Authorization` header gets a `401`, a role without the required feature gets a `403`.

## Test it

```
./mvnw verify
```

Tests run on in-memory H2 with the same Flyway scripts: no database needed.

Tests are independent and repeatable: they share the same database, so every test creates its own data (unique emails with `UniqueEmail`, unique item names), only asserts on that data, and never deletes data of others. They pass in any order, alone or together: classes and methods run in a random order (`src/test/resources/junit-platform.properties`), so a test depending on another one fails quickly. The seed of the order is in the test reports (`junit.jupiter.execution.order.random.seed` in `target/surefire-reports/TEST-*.xml`); replay the same order with `./mvnw test -Dtest.order.seed=<seed>`.

## Stories

| Story | Description | Status |
| --- | --- | --- |
| 0 | Project setup | Done |
| T | Persistence: JPA, PostgreSQL, H2, transactions (technical) | Done |
| S | Security: roles, features, Basic authentication with Spring Security (technical) | Done |
| 1 | Create a customer account | Done |
| 2 | Add an item | Done |
| 3 | Order items | Done |
| 7 | View all customers | Done |
| 8 | View a single customer | Done |
| 4 | Update an item | Done |
| 5 | View report of orders | Done |
| 6 | Reorder an existing order | Done |
| 9 | Items shipping today | Done |
| 10 | Item overview | Done |
| E | Item units and states (optional extension) | Done |
| C | Required configuration validated at startup (technical) | Done |

## Item units and states

Optional extension of the assignment: every piece in stock has a state, and damaged pieces are sold at a discount.

- An `Item` is a product of the catalog (name, description, base price). Each physical piece of it is an `ItemUnit`, with its own state. What the assignment calls "an Item representing a single fridge" is an `ItemUnit` here.
- States and discounts: `PRISTINE` (full price), `SLIGHTLY_DAMAGED` (-5%), `DAMAGED` (-10%). The discounted price of a unit is rounded half up to the cent: 49.99 becomes 47.49 and 44.99.
- The stock of an item is the number of its available units. A unit is available until it is linked to the item group that sold it.

| Endpoint | Role | Body |
| --- | --- | --- |
| `POST /items` | Create an item with its first units | `{"name": "...", "description": "...", "price": 49.99, "units": {"PRISTINE": 5, "DAMAGED": 1}}` |
| `PUT /items/{id}` | Replace the name, description and price | `{"name": "...", "description": "...", "price": 49.99}` |
| `GET /items/{id}/units` | List the units, by state, available ones first | |
| `POST /items/{id}/units` | Add units of a state (restocking) | `{"state": "PRISTINE", "amount": 10}` |
| `PATCH /items/{id}/units/{unitId}` | Change the state of an available unit | `{"state": "DAMAGED"}` |

At most 1000 units per state can be added in one request. A sold unit keeps the state it was sold in (`409`).

## Ordering rules

- Each item group names an item, an amount and optionally a state (`PRISTINE` when left out): `{"itemId": "...", "amount": 2, "state": "DAMAGED"}`.
- An item group ships the next day when enough units of that state are available, otherwise one week later.
- Item groups are handled in the order they are sent: ordering the same item twice, the second group sees the units left by the first.
- Ordering links the available units to the item group, up to the ordered amount.
- An item group copies the name, the state and the discounted price of the item, so later price changes do not affect past eurders.
- Two requests changing the units of the same item at the same time: the last one to commit gets a `409` and can retry (each eurder increments the version of its items, `OPTIMISTIC_FORCE_INCREMENT`).
- The stock urgency of the overview counts the available units of all states; the overview also shows them per state.
- The report of a customer lists their eurders oldest first, at the prices copied when they ordered.
- What ships today is grouped by eurder, with the name and address of the customer and the state of each item group.
- Reordering creates a new eurder with the same items, states and amounts, at the current prices and stock. An eurder of another customer is answered with a `404`, as if it did not exist.

### Known limitations

Units still owed to customers are not tracked. Ordering 10 units with 5 available ships the whole item group a week later and links the 5 available units to it: the 5 missing ones are only visible as the difference between the amount and the linked units.

Updating an item replaces all of its fields. Two admins updating the same item at the same time: the last one wins, since the request does not carry the version of the item it was based on.

Shipping is not tracked: an item group that was not shipped on its shipping date no longer appears in the items shipping today, and an item group whose units are still missing appears on its date anyway. Tracking a shipping status per item group would solve this limitation as well as the one about units still owed.
