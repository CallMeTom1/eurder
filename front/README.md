# Eurder front end

Angular 22 front end of the Eurder API: a public shop and an admin back office.

## Run it

1. Start the API (see the README of the repository): it listens on `http://localhost:8080/api`
2. `npm install`
3. `npm start`, then open http://localhost:4200

`npm start` serves the app with `proxy.conf.json`: every call to `/api` goes to `http://localhost:8080`. For the browser, the page and the API have the same origin, so there is no CORS, and the `HttpOnly; SameSite=Strict` token cookies of the API work as they are.

## Tests

| Command                                   | What                                                                                               |
| ----------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `npm test`                                | Unit tests (Vitest), in watch mode                                                                 |
| `npm run test:ci`                         | Unit tests, once                                                                                   |
| `npm run e2e`                             | End-to-end tests with a mocked API (Playwright, Chromium). Starts `npm start` if it is not running |
| `npm run e2e:fullstack`                   | End-to-end tests against the real API and a real database                                          |
| `npm run format` / `npm run format:check` | Prettier                                                                                           |

### Two kinds of end-to-end tests

|          | `e2e/mocked`                                                      | `e2e/fullstack`                                                              |
| -------- | ----------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| API      | Answers written in the test with `page.route`                     | The real API, started by Playwright                                          |
| Needs    | Nothing                                                           | Java, PostgreSQL on `localhost:5432`                                         |
| Good for | Cases hard to cause for real: API down, slow API, every role      | What a mock cannot show: cookies, CSRF, the proxy, the refresh, the database |
| Visitor  | Anonymous by default (`fixtures.ts`), `loggedInAs(page, 'ADMIN')` | Real accounts, created by each test                                          |

`npm run e2e:fullstack`:

1. recreates the `eurder_e2e` database (`e2e/fullstack/reset-database.mjs`),
2. starts its own API on port 8181 on that database, with access tokens lasting 5 seconds so that the refresh happens for real,
3. starts its own dev server on port 4300 (`proxy.e2e.json` sends `/api` to port 8181),
4. runs the tests in parallel: each one creates its own customer with a unique email, so they never share data.

The development API (8080), dev server (4200) and database (`eurder`) are never touched. The database connection defaults to `postgres` / `postgres`: override it with `E2E_DB_HOST`, `E2E_DB_PORT`, `E2E_DB_USERNAME`, `E2E_DB_PASSWORD` or `E2E_DB_NAME`.

## Imports

TypeScript aliases avoid `../../../`:

| Alias         | Folder                                                            |
| ------------- | ----------------------------------------------------------------- |
| `@core/*`     | `src/app/core/*`: app-wide services (security, http, i18n, theme) |
| `@shared/*`   | `src/app/shared/*`: reusable components, directives, pipes        |
| `@layout/*`   | `src/app/layout/*`: front office and back office layouts          |
| `@features/*` | `src/app/features/*`: the pages, per feature                      |

## Styles

One primary color drives everything:

```text
#3F5A7A ──ng generate @angular/material:theme-color──► src/styles/_theme-colors.scss (generated, never edited)
        ──mat.theme() in src/styles/_theme.scss──────► --mat-sys-* CSS variables, light and dark
        ├─► Angular Material components
        └─► Tailwind colors in src/styles/tailwind.css (bg-primary, text-on-surface...)
```

- To change the brand: run the schematic again with another `--primary-color` and `--directory=src/styles/`.
- Material for the components, Tailwind for the layout. Never restyle a Material component with Tailwind classes.
- Tailwind only knows the colors of the theme (`--color-*: initial`): no hard-coded color.
- `color-scheme: light dark` on `<html>`: the browser picks the palette with `light-dark()`. `ThemeService` (`@core/theme`) stores the choice of the user (`light`, `dark` or `system`) in `localStorage` under `eurder.theme` and sets `data-theme` on `<html>`, which `_theme.scss` turns into `color-scheme`. `ThemeToggle` (`@shared/ui`) is its menu.
- Font: Inter, served from `node_modules` (`@fontsource-variable/inter`), not from Google.

`/design-system` shows the colors and the shared components. It only exists in development: the production build replaces `dev-only.routes.ts` with an empty list.

## Languages

French, English and Dutch, with [ngx-translate](https://ngx-translate.org): one file per language in `public/i18n/`, loaded at runtime.

- `LanguageService` (`@core/i18n`) picks the stored choice (`localStorage`, `eurder.lang`), else the first supported browser language, else English. It sets `<html lang>`. The app waits for the texts before showing anything, so no raw key flashes on screen.
- `LanguageSwitcher` (`@shared/ui`) names each language in its own language.
- In a template: `{{ 'common.theme.dark' | translate }}`. Keys are grouped by area: `common.*` now, then `catalog.*`, `admin.*`...
- The error codes of the API (`error.*`, `validation.*`) will be translated from the same files.
- `translations.spec.ts` fails when a key is missing in one language, or a text is empty.
- In unit tests, `provideTestTranslations()` (`@core/i18n/testing`) loads the real files.

## API layer

`src/app/core/api/eurder-api.ts` holds the types of every DTO of the API, generated from its OpenAPI description. Never edit it by hand.

```bash
npm run api:types   # the API must run on http://localhost:8080
```

- Run it again when the API changes: the compiler then points at every place to adapt, and the diff of the file shows what changed.
- Types only exist at compile time: nothing of this file reaches the production bundle. The one runtime value used, the list of error codes (`problemDetailCodeValues`), is only imported by a test.
- The script calls `openapi-typescript` through `npx`, with a pinned version: it needs TypeScript 5 while Angular 22 uses TypeScript 6, so it stays out of the dependencies.
- `--properties-required-by-default`: a response field is always present unless the API says it can be `null` or missing.

Conventions:

| Piece            | Where                                                                      | Rule                                                                                                                                           |
| ---------------- | -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| URLs             | `ApiRoutes` (`@core/api/api-routes`)                                       | Every URL of the API in one place                                                                                                              |
| DTO              | `import type { CatalogItemDto } from '@core/api/eurder-api'`               | Exactly what the API sends, used with `HttpClient`                                                                                             |
| Model and mapper | Next to the feature (`current-user.ts`: `CurrentUser` and `toCurrentUser`) | Only when the mapping adds something: a computed field, cents instead of decimal prices, `Date` instead of ISO strings. Otherwise, use the DTO |
| Errors           | An interceptor (next step)                                                 | They stay errors, typed as `ApiProblem`. No `{ result, data }` envelope                                                                        |

## Bundle budget

The initial bundle warns above 800 kB (the CLI default is 500 kB). Our own code is about 23 kB: the rest is Angular and Angular Material with the CDK (about 310 kB). Compressed, the browser downloads about 160 kB. Every page but the catalog is loaded on demand with `loadComponent`, so the budget only grows with what the home page needs.
