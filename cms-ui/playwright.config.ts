/* eslint-disable import/no-nodejs-modules,import/order */
import { ENV_CI } from '@gentics/e2e-utils';
import { nxE2EPreset } from '@nx/playwright/preset';
import { defineConfig, devices, PlaywrightTestConfig } from '@playwright/test';
import { config } from 'dotenv';
import { dirname, resolve } from 'path';

export function createConfiguration(originalConfig: string, appName: string, ciBaseUrl: string): PlaywrightTestConfig {
    const projectRoot = dirname(originalConfig);
    const workspaceRoot = __dirname;

    config({ path: resolve(workspaceRoot, '.env') });
    config({ path: resolve(workspaceRoot, '.env.local') });
    config({ path: resolve(projectRoot, '.env'), override: true });
    config({ path: resolve(projectRoot, '.env.local'), override: true });

    const IS_CI = !!process.env[ENV_CI];

    // Usually never defined, but allow overrides
    // eslint-disable-next-line @typescript-eslint/naming-convention
    let BASE_URL = process.env['BASE_URL'];

    /*
     * For consistency (and the only way for us right now to run the tests stable in Jenkins),
     * the actual playwright server is being run as container/service in the integration-tests
     * setup.
     * If we're running it on the CI/Jenkins, we want to test the actual baked in UI from the CMS.
     * Otherwise, we spin up a webserver with the current UI (usually for development or debugging),
     * and run the tests from there.
     * `hostmachine` is an extra hosts entry in the docker compose setup, which allows the playwright
     * container/service to access that webserver.
     */
    if (!BASE_URL) {
        if (IS_CI) {
            BASE_URL = ciBaseUrl;
        } else {
            BASE_URL = 'http://hostmachine:4200';
        }
    }

    /**
     * See https://playwright.dev/docs/test-configuration.
     */
    return defineConfig({
        ...nxE2EPreset(originalConfig, {
            testDir: './e2e',
        }),

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
                    outputFile: `../../.reports/apps/${appName}/PLAYWRIGHT-report.xml`,
                }],
            ]
            : [
                ['list'],
            ],
        fullyParallel: false,
        workers: 1,
        forbidOnly: IS_CI,
        /* Run your local dev server before starting the tests */
        webServer: IS_CI ? undefined : {
            command: `npm start ${appName}`,
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

}
