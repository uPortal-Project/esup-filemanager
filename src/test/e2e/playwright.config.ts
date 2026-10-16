import { defineConfig, devices } from '@playwright/test';

const results = process.env.E2E_OUTPUT || './results/local';
const campaign = process.env.E2E_CAMPAIGN || 'full';
if (!['smoke', 'full'].includes(campaign)) {
  throw new Error('E2E_CAMPAIGN must be smoke or full');
}

export default defineConfig({
  testDir: './specs',
  workers: 1,
  forbidOnly: !!process.env.CI,
  failOnFlakyTests: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  timeout: 120_000,
  expect: { timeout: 15_000 },
  outputDir: results + '/test-results',
  reporter: [
    ['line'],
    ['html', { outputFolder: results + '/html', open: 'never' }],
    ['junit', { outputFile: results + '/junit.xml' }],
    ['json', { outputFile: results + '/report.json' }],
  ],
  grep: campaign === 'smoke' ? /@smoke/ : undefined,
  use: {
    baseURL: 'http://app-e2e:8080',
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: process.env.E2E_VIDEO === 'on' ? 'on' : 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 60_000,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile-chromium', testMatch: 'file-access.spec.ts', use: { ...devices['Pixel 7'] } },
    { name: 'mobile-webkit', testMatch: 'file-access.spec.ts', use: { ...devices['iPhone 13'] } },
  ],
});
