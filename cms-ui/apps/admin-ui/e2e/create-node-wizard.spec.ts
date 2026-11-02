import {
    clickModalAction,
    EntityImporter,
    loginWithForm,
    matchRequest,
    navigateToApp,
    onRequest,
    selectTab,
    selectTableRow,
    TestSize,
    waitForResponseFrom,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { findEntityTableActionButton, navigateToModule } from './helpers';

const SELECTORS = {
    WIZARD: 'gtx-create-node-wizard gtx-wizard',
    TAB_LINK: 'gtx-tabs .tab-link',
    NAME_INPUT: 'gtx-node-properties gtx-input[formcontrolname="name"] input',
    HOST_INPUT: 'gtx-node-properties gtx-input[formcontrolname="host"] input',
    LANGUAGE_ROW: 'gtx-language-table .grid-row.data-row',
} as const;

/** IDs of the wizard steps. The "Forms" step is only shown when the feature is activated, which it isn't by default. */
const STEP = {
    PROPERTIES: 0,
    PUBLISHING: 1,
    FEATURES: 2,
    LANGUAGES: 3,
} as const;

const NODE_NAME = 'Wizard Test Node';
const NODE_HOST = 'wizard-test.localhost';

test.describe('Create Node Wizard', () => {

    const IMPORTER = new EntityImporter();

    let wizard: Locator;
    let createNodeRequestCount: number;

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });
    });

    test.beforeEach(async ({ page, request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.syncPackages(TestSize.MINIMAL);
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        // Count the requests which actually create a node
        createNodeRequestCount = 0;
        onRequest(page, matchRequest('PUT', '/rest/node'), () => createNodeRequestCount++);

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);

        await test.step('Open the Create Node Wizard', async () => {
            const module = await navigateToModule(page, 'nodes');
            await findEntityTableActionButton(module, 'create').click();

            wizard = page.locator(SELECTORS.WIZARD);
            await expect(findTab(STEP.PROPERTIES)).toHaveClass(/is-active/);
        });
    });

    function findTab(step: number): Locator {
        return wizard.locator(`${SELECTORS.TAB_LINK}[data-id="${step}"]`);
    }

    async function fillRequiredProperties(page: Page): Promise<void> {
        await page.locator(SELECTORS.NAME_INPUT).fill(NODE_NAME);
        await page.locator(SELECTORS.HOST_INPUT).fill(NODE_HOST);
    }

    test.describe('Tab Navigation', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20153',
        }],
    }, () => {
        test('should not allow selecting the following steps while the current step is invalid', async () => {
            await test.step('All following steps should be disabled', async () => {
                await expect(findTab(STEP.PUBLISHING)).toHaveClass(/disabled/);
                await expect(findTab(STEP.FEATURES)).toHaveClass(/disabled/);
                await expect(findTab(STEP.LANGUAGES)).toHaveClass(/disabled/);
            });

            await test.step('Clicking a disabled step should not change the active step', async () => {
                await selectTab(wizard, STEP.LANGUAGES);
                await expect(findTab(STEP.PROPERTIES)).toHaveClass(/is-active/);
            });
        });

        test('should allow selecting the following steps once the steps in between are valid', async ({ page }) => {
            await fillRequiredProperties(page);

            await expect(findTab(STEP.PUBLISHING)).not.toHaveClass(/disabled/);
            await expect(findTab(STEP.FEATURES)).not.toHaveClass(/disabled/);
            await expect(findTab(STEP.LANGUAGES)).not.toHaveClass(/disabled/);
        });

        test('should jump forward to the last step without creating the node', async ({ page }) => {
            await fillRequiredProperties(page);

            const languagesTab = await selectTab(wizard, STEP.LANGUAGES);

            await expect(findTab(STEP.LANGUAGES)).toHaveClass(/is-active/);
            await expect(languagesTab.locator(SELECTORS.LANGUAGE_ROW).first()).toBeVisible();
            expect(createNodeRequestCount).toBe(0);
        });

        test('should go back to a previous step and keep the entered values', async ({ page }) => {
            await fillRequiredProperties(page);

            await test.step('Jump forward to the last step', async () => {
                await selectTab(wizard, STEP.LANGUAGES);
                await expect(findTab(STEP.LANGUAGES)).toHaveClass(/is-active/);
            });

            await test.step('Go back to the first step', async () => {
                await selectTab(wizard, STEP.PROPERTIES);
                await expect(findTab(STEP.PROPERTIES)).toHaveClass(/is-active/);
                await expect(page.locator(SELECTORS.NAME_INPUT)).toHaveValue(NODE_NAME);
                await expect(page.locator(SELECTORS.HOST_INPUT)).toHaveValue(NODE_HOST);
            });
        });

        test('should disable the following steps again when a previous step becomes invalid', async ({ page }) => {
            await fillRequiredProperties(page);

            await test.step('Navigate forward and back again', async () => {
                await selectTab(wizard, STEP.PUBLISHING);
                await expect(findTab(STEP.PUBLISHING)).toHaveClass(/is-active/);
                await selectTab(wizard, STEP.PROPERTIES);
                await expect(findTab(STEP.PROPERTIES)).toHaveClass(/is-active/);
            });

            await test.step('Invalidate the first step', async () => {
                await page.locator(SELECTORS.NAME_INPUT).clear();

                await expect(findTab(STEP.PUBLISHING)).toHaveClass(/disabled/);
                await selectTab(wizard, STEP.LANGUAGES);
                await expect(findTab(STEP.PROPERTIES)).toHaveClass(/is-active/);
            });
        });

        test('should create exactly one node when finishing the wizard after navigating via the tabs', async ({ page }) => {
            await fillRequiredProperties(page);

            await test.step('Jump forward to the last step and select a language', async () => {
                const languagesTab = await selectTab(wizard, STEP.LANGUAGES);
                await expect(findTab(STEP.LANGUAGES)).toHaveClass(/is-active/);
                await selectTableRow(languagesTab.locator(SELECTORS.LANGUAGE_ROW).first());
            });

            await test.step('Finish the wizard', async () => {
                const createReq = waitForResponseFrom(page, 'PUT', '/rest/node');
                await clickModalAction(wizard, 'next');
                await createReq;

                await expect(wizard).toBeHidden();
                expect(createNodeRequestCount).toBe(1);
            });
        });
    });
});
