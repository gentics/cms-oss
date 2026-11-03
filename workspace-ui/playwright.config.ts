import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
    testDir: './e2e',

    // Prevent accidental test.only from reaching CI.
    forbidOnly: !!process.env.CI,

    // Retry failed tests in CI, but not during local development.
    retries: process.env.CI ? 2 : 0,

    // Keep CI predictable.
    workers: process.env.CI ? 1 : undefined,

    reporter: process.env.CI
        ? [['list'], ['github']]
        : [['list'], ['html', { open: 'never' }]],

    use: {
        baseURL: 'http://127.0.0.1:5173',

        // Collect a trace when a test fails and is retried.
        trace: 'on-first-retry',

        // Only save screenshots when something goes wrong.
        screenshot: 'only-on-failure',
    },

    // Playwright starts Vite automatically: the app, and the component catalogue for
    // `e2e/catalogue.spec.ts` (which sets its own `baseURL`).
    webServer: [
        {
            command: 'npm run dev -- --host 127.0.0.1',
            url: 'http://127.0.0.1:5173',
            reuseExistingServer: !process.env.CI,
        },
        {
            command: 'npm run catalogue -- --host 127.0.0.1',
            url: 'http://127.0.0.1:5174',
            reuseExistingServer: !process.env.CI,
        },
    ],

    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
            },
        },
    ],
});
