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
