# Eurder front end

Angular 22 front end of the Eurder API: a public shop and an admin back office.

## Run it

1. Start the API (see the README of the repository): it listens on `http://localhost:8080/api`
2. `npm install`
3. `npm start`, then open http://localhost:4200

`npm start` serves the app with `proxy.conf.json`: every call to `/api` goes to `http://localhost:8080`. For the browser, the page and the API have the same origin, so there is no CORS, and the `HttpOnly; SameSite=Strict` token cookies of the API work as they are.

## Tests

| Command                                   | What                                                                             |
| ----------------------------------------- | -------------------------------------------------------------------------------- |
| `npm test`                                | Unit tests (Vitest), in watch mode                                               |
| `npm run test:ci`                         | Unit tests, once                                                                 |
| `npm run e2e`                             | End-to-end tests (Playwright, Chromium). Starts `npm start` if it is not running |
| `npm run format` / `npm run format:check` | Prettier                                                                         |

## Imports

TypeScript aliases avoid `../../../`:

| Alias         | Folder                                                            |
| ------------- | ----------------------------------------------------------------- |
| `@core/*`     | `src/app/core/*`: app-wide services (security, http, i18n, theme) |
| `@shared/*`   | `src/app/shared/*`: reusable components, directives, pipes        |
| `@layout/*`   | `src/app/layout/*`: front office and back office layouts          |
| `@features/*` | `src/app/features/*`: the pages, per feature                      |
