import { defineConfig, devices } from '@playwright/test';

const API_PORT = 8181;
const FRONT_PORT = 4300;
const mavenWrapper = process.platform === 'win32' ? '.\\mvnw.cmd' : './mvnw';

export default defineConfig({
  testDir: './e2e/fullstack',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: `http://localhost:${FRONT_PORT}`,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: `${mavenWrapper} -q spring-boot:run`,
      cwd: '..',
      url: `http://localhost:${API_PORT}/api/catalog`,
      timeout: 180_000,
      reuseExistingServer: false,
      env: {
        SERVER_PORT: String(API_PORT),
        DB_URL: `jdbc:postgresql://${process.env['E2E_DB_HOST'] ?? 'localhost'}:${process.env['E2E_DB_PORT'] ?? '5432'}/${process.env['E2E_DB_NAME'] ?? 'eurder_e2e'}`,
        DB_USERNAME: process.env['E2E_DB_USERNAME'] ?? 'postgres',
        DB_PASSWORD: process.env['E2E_DB_PASSWORD'] ?? 'postgres',
        JWT_SECRET: 'secret-only-used-by-the-full-stack-end-to-end-tests',
        EURDER_JWT_ACCESSTOKENTTL: '5s',
      },
    },
    {
      command: `npx ng serve --port ${FRONT_PORT} --proxy-config proxy.e2e.json`,
      url: `http://localhost:${FRONT_PORT}`,
      timeout: 120_000,
      reuseExistingServer: false,
    },
  ],
});
