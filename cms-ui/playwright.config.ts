/* eslint-disable import/no-nodejs-modules,import/order */
import {
    ENV_E2E_APP_PATH,
    ENV_E2E_APP_URL,
    ENV_E2E_CMS_URL,
    ENV_E2E_FORCE_REPEATS,
    ENV_E2E_LOCAL_APP,
    ENV_E2E_LOCAL_PLAYWRIGHT,
    ENV_SKIP_E2E_LOCAL_APP_LAUNCH,
    isCIEnvironment,
    isEnvBool,
} from '@gentics/e2e-utils';
import { nxE2EPreset } from '@nx/playwright/preset';
import { defineConfig, devices, PlaywrightTestConfig } from '@playwright/test';
import { config } from 'dotenv';
import { dirname, resolve } from 'path';

export function createConfiguration(
    originalConfig: string,
    appName: string,
    serviceBaseUrl: string,
): PlaywrightTestConfig {
    const projectRoot = dirname(originalConfig);
    const workspaceRoot = __dirname;

    /*
     * Loading the environment info for overrides from the workspace.
     */
    config({ path: resolve(workspaceRoot, '.env') });
    config({ path: resolve(workspaceRoot, '.env.local') });
    config({ path: resolve(projectRoot, '.env'), override: true });
    config({ path: resolve(projectRoot, '.env.local'), override: true });

    const isCI = isCIEnvironment();
    const forceRepeats = isEnvBool(process.env[ENV_E2E_FORCE_REPEATS]);
    /** If we want to use the local app, but don't actually start it */
    const useLocalApp = isEnvBool(process.env[ENV_E2E_LOCAL_APP]);
    const startLocalApp = !isEnvBool(process.env[ENV_SKIP_E2E_LOCAL_APP_LAUNCH]);
    /** If we want to use the playwright server locally instead of from the container */
    const useLocalPlaywright = useLocalApp || isEnvBool(process.env[ENV_E2E_LOCAL_PLAYWRIGHT]);

    /*
     * If we're running it on the CI/Jenkins, we want to test the actual baked in UI from the CMS.
     * Otherwise, we spin up a webserver with the current UI (usually for development or debugging),
     * and run the tests from there.
     * `hostmachine` is an extra hosts entry in the docker compose setup, which allows the playwright
     * container/service to access that webserver.
     */

    if (!process.env[ENV_E2E_CMS_URL]) {
        // Set iy up in the ENV since it isn't defined yet
        process.env[ENV_E2E_CMS_URL] = !useLocalPlaywright ? 'http://cms:8080' : 'http://localhost:8080';
    }

    if (!process.env[ENV_E2E_APP_PATH]) {
        process.env[ENV_E2E_APP_PATH] = !useLocalApp ? serviceBaseUrl : '/';
    }

    if (!process.env[ENV_E2E_APP_URL]) {
        process.env[ENV_E2E_APP_URL] = (isCI || !useLocalApp)
            ? `${process.env[ENV_E2E_CMS_URL]}${process.env[ENV_E2E_APP_PATH]}`
            : 'http://localhost:4200';
    }

    /**
     * See https://playwright.dev/docs/test-configuration.
     */
    return defineConfig({
        ...nxE2EPreset(originalConfig, {
            testDir: './e2e',
        }),
        name: `${appName} integration tests`,

        /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
        use: {
            baseURL: process.env[ENV_E2E_APP_URL],
            /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
            trace: isCI ? 'off' : 'retain-on-first-failure',
            /*
             * For consistency (and the only way for us right now to run the tests stable in Jenkins),
             * the actual playwright server is being run as container/service in the integration-tests setup.
             */
            connectOptions: isCI || !useLocalPlaywright ? {
                wsEndpoint: 'ws://127.0.0.1:3000/',
            } : null,
            bypassCSP: true,
            /* Make sure we have a consistent timezone for our tests. */
            timezoneId: 'Europe/Vienna',
        },
        /*
         * None of our tests can be run in parallel, as they all access the same CMS instance
         * and all perform a cleanup and setup routine which would interfere with other tests.
         */
        fullyParallel: false,
        workers: 1,
        /*
         * Repeat the tests multiple times to make sure the tests aren't flaky.
         * Also allow retries to see if they are entirely broken or just flaky.
         */
        retries: 2,
        repeatEach: forceRepeats ? 3 : 0,
        /*
         * Making sure no accidental `only` runs are being run on CI which would skip all
         * other tests, potentially marking it successful.
         */
        forbidOnly: isCI,
        /*
         * Don't perserve data on the CI, as it can't be retrieved anyways.
         */
        preserveOutput: isCI ? 'never' : 'always',
        /*
         * If it's on the CI, we use the UI from the CMS container.
         * Otherwise start the dev server for the app and use that to run the tests.
         * Useful for local debugging and/or creating new tests, so you don't have to create
         * a new docker image for every single change.
         */
        webServer: isCI || !useLocalApp || !startLocalApp ? undefined : {
            command: `npm start ${appName}`,
            url: 'http://localhost:4200',
            reuseExistingServer: true,
            cwd: workspaceRoot,
            stdout: 'pipe',
            // Wait for max of 2min for dev server to be ready
            timeout: 2 * 60_000,
        },
        /*
         * Logging for CI is minimal in the STDOUT, since errors will be fully logged anyways
         * and are otherwise also visible in the report.
         * Report is in junit so Jenkins can pick it up.
         */
        reporter: isCI
            ? [
                ['dot'],
                ['junit', {
                    outputFile: `../../.reports/apps/${appName}/PLAYWRIGHT-report.xml`,
                }],
            ]
            : [
                ['list'],
            ],
        /*
         * The browsers/devices to test against.
         * Currently only chromium as it's the most common used.
         */
        projects: [
            {
                name: 'chromium',
                use: { ...devices['Desktop Chrome'] },
            },
        ],
    });
}
