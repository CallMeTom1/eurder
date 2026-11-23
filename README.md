# Eurder

Back-end REST API for Örder, an order management application (Switchfully self-evaluation project).
Built as part of the Switchfully Java track.

## Tech stack

- Java 17, Spring Boot 4.1, Maven
- Spring Data JPA + PostgreSQL, schema managed by Flyway (`src/main/resources/db/migration`)
- H2 in-memory database for the tests
- Spring Security: stateless JWT authentication with refresh tokens, each endpoint authorized by a feature of the role (`@Secured`)
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
4. Open the API documentation: http://localhost:8080/api/docs. Each operation states the feature and role it requires, its response codes with the `ProblemDetail` error format, and an example request.

Every endpoint lives under `/api` (`server.servlet.context-path`), so that a front end and its proxy see the same paths as the API. The paths in this README are relative to it: `POST /auth/login` is `http://localhost:8080/api/auth/login`.

The Angular front end lives in [`front/`](front/README.md): with the API running, `npm start` there and open http://localhost:4200.

### Demo data

With the API running, `node demo/seed.mjs` fills it through its own endpoints: 19 items with a picture and various stocks (so every urgency shows), three customers in Belgium, France and Germany who log in with the password `demo1234`, and a few orders. Running it again skips what already exists. The pictures come from Wikimedia Commons, see [demo/CREDITS.md](demo/CREDITS.md) for their authors and licenses.

Uploaded pictures are stored in `data/images` (`IMAGES_DIRECTORY` to change it), outside of git.

## Configuration

The application reads four required keys, with no default value:

| Key | Example |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/eurder` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | your database password |
| `JWT_SECRET` | a random value of at least 32 bytes (`openssl rand -hex 32`) |

- They come from `.env` (a copy of `.env.example`, never committed) or from environment variables, which take precedence over `.env`.
- `.env` uses the `.properties` format: write `DB_PASSWORD=abc`, without quotes.
- A missing or blank key stops the startup with an `APPLICATION FAILED TO START` report that lists every missing key at once, without ever showing a value.
- Changing `JWT_SECRET` invalidates every access token.
- The tests do not need `.env`: they define the keys for their in-memory H2 database.

## Default admin

The Flyway migration `V2__insert_default_admin.sql` creates an administrator account:

| Email | Password | Role |
| --- | --- | --- |
| `admin@eurder.be` | `admin` | `ADMIN` |

Change this password before any real deployment.

An admin creates another admin with `POST /administrators` and the body `{"email": "...", "password": "..."}`. An admin only has credentials: no address, no phone number, and it does not appear among the customers.

The address of a customer holds a country among those Eurder ships to: `BELGIUM`, `FRANCE` or `GERMANY`. Customers registered before the country existed live in Belgium (migration `V12`).

Only the customer account creation (`POST /customers`), the catalog (`GET /catalog`), the pictures (`GET /images/{name}`), the `/auth` endpoints and the documentation are public. Every other request is authenticated before it reaches the API: a missing, invalid or expired access token gets a `401`, a role without the required feature gets a `403`.

## Authentication

The API is stateless: there is no server session, and a request carrying an access token is authenticated from the token alone, without reading the database.

| Endpoint | Request | Result |
| --- | --- | --- |
| `POST /auth/login` | `{"email": "...", "password": "..."}` | `200`, sets both token cookies, body `{"id", "role", "email", "firstName", "lastName"}` (no name for an admin). `401` when the email or password is wrong |
| `POST /auth/refresh` | The `refresh_token` cookie | `200`, sets new token cookies, same body as the login. `401` when the cookie is missing, or the token unknown, expired, revoked or already used |
| `GET /auth/me` | The access token | `200` with the same body as the login, `401` without a valid access token |
| `POST /auth/logout` | The `refresh_token` cookie | `204` and both cookies cleared, even without a cookie or with an unknown token |

The tokens are never in a response body, only in `HttpOnly` cookies that the JavaScript of a page cannot read. A browser sends the `access_token` cookie by itself. Other clients send the access token as `Authorization: Bearer <access token>`, which wins when both are present. `login`, `refresh` and `logout` ignore the access token, so an expired one does not block them.

A front end cannot read these cookies: it calls `GET /auth/me` at startup to know who is logged in.

| | Access token | Refresh token |
| --- | --- | --- |
| Form | JWT signed with HS256, containing the credentials id and the role | Random value |
| Lifetime | 15 minutes | 7 days, renewed by each refresh |
| Stored on the server | No | Only its SHA-256 hash |
| Cookie | `access_token; HttpOnly; Secure; SameSite=Strict; Path=/api`, kept 7 days | `refresh_token; HttpOnly; Secure; SameSite=Strict; Path=/api/auth`, kept 7 days |

The access token cookie is kept as long as the refresh token, longer than the JWT it holds: an expired JWT still reaches the API, which answers `error.security.access-token-expired`, so a front end knows it must refresh rather than log in again.

- A refresh token works once: each refresh replaces it. Using an old one again revokes every token descending from the same login, since one of the two parties may have stolen it. Two simultaneous refreshes with the same token have the same effect, so a client must not send them in parallel.
- Each login is independent: logging out or a compromised token on one device does not affect the others.
- An access token cannot be revoked: after a logout, it stays valid until it expires.
- The role is read from the access token, so a role change would only apply at the next refresh. The features of a role are read from the code, so they apply immediately.

### CSRF protection

A browser sends the token cookies by itself, even when another site triggers the request. So every `POST`, `PUT`, `PATCH` and `DELETE` must also carry a CSRF token (double-submit cookie):

1. Every response sets an `XSRF-TOKEN` cookie, readable by JavaScript (`Path=/; Secure; SameSite=Strict`), including a `401`, so a front end has it before logging in.
2. The front end copies its value in the `X-XSRF-TOKEN` header. Angular does it by itself, and so does Swagger UI.
3. The API compares both. Missing or different: `403` with `code: error.security.invalid-csrf-token`.

Another site can neither read our cookies nor add this header. `GET` requests need no token.

A request with an `Authorization: Bearer` header needs no CSRF token either (Spring Security exempts it): a browser never adds that header by itself, so it cannot be forged. To keep that exemption away from the cookie, the access token cookie becomes a `Bearer` header only after the CSRF check (`AccessTokenCookieFilter`).

With `curl` and the cookies, send any value twice: `-b "XSRF-TOKEN=abc" -H "X-XSRF-TOKEN: abc"`.

Not implemented yet: removing expired refresh tokens from the database, limiting login attempts, and CORS (a front end on another origin goes through a proxy).

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
| S | Security: roles, features, JWT authentication with Spring Security (technical) | Done |
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
| B | Create another admin (bonus) | Done |

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

## Public catalog

What a visitor of the shop sees, without logging in. The stock and its urgency stay internal: they are only in the admin overview (`GET /items`).

| Endpoint | Result |
| --- | --- |
| `GET /catalog` | Every item as `{"id", "name", "description", "price"}`, sorted by name |
| `GET /catalog/{id}` | One item, `404` with `code: error.item.not-found` when it does not exist |

## Ordering rules

- Each item group names an item, an amount from 1 to 1000 and optionally a state (`PRISTINE` when left out): `{"itemId": "...", "amount": 2, "state": "DAMAGED"}`.
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
