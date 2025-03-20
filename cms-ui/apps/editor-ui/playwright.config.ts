import { defineConfig, devices } from '@playwright/test';
import { nxE2EPreset } from '@nx/playwright/preset';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { workspaceRoot } from '@nx/devkit';
import { ENV_CI } from '@gentics/e2e-utils';

const IS_CI = !!process.env[ENV_CI];

// If playwright has a server to connect to, and therefore doesn't need additional local setup.
const HAS_SERVER = IS_CI || !!process.env['PW_TEST_CONNECT_WS_ENDPOINT'];

// For CI, you may want to set BASE_URL to the deployed application.
// eslint-disable-next-line @typescript-eslint/naming-convention
let BASE_URL = process.env['BASE_URL'];
if (!BASE_URL) {
    if (HAS_SERVER) {
        BASE_URL = 'http://cms:8080/editor';
    } else {
        BASE_URL = 'http://localhost:4200';
    }
}

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
    ...nxE2EPreset(__filename, { testDir: './e2e' }),
    /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
    use: {
        baseURL: BASE_URL,
        /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
        trace: IS_CI ? 'off' : 'on-first-retry',
    },
    reporter: IS_CI
        ? [
            ['dot'],
            ['junit', {
                outputFile: '../../.reports/apps/editor-ui/PLAYWRIGHT-report.xml',
            }],
        ]
        : [
            ['list'],
        ],
    fullyParallel: false,
    workers: 1,
    forbidOnly: IS_CI,
    /* Run your local dev server before starting the tests */
    webServer: HAS_SERVER ? undefined : {
        command: 'npm start editor-ui',
        url: 'http://127.0.0.1:4200',
        reuseExistingServer: true,
        cwd: workspaceRoot,
        stdout: 'pipe',
        // Wait for max of 2min for dev server to be ready
        timeout: 2 * 60_000,
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] },
        },
    ],
});
