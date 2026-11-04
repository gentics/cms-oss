import {
    clickModalAction,
    clickTableRow,
    EntityImporter,
    findTableRowById,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
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
    MASTER: 'gtx-node-master',
    EDITOR: 'gtx-node-editor',
    MESH_PROJECT_NAME_INPUT: 'gtx-node-properties gtx-input[formcontrolname="meshProjectName"] input',
    WIZARD: 'gtx-create-node-wizard gtx-wizard',
    TAB_LINK: 'gtx-tabs .tab-link',
    NAME_INPUT: 'gtx-node-properties gtx-input[formcontrolname="name"] input',
    HOST_INPUT: 'gtx-node-properties gtx-input[formcontrolname="host"] input',
    LANGUAGE_ROW: 'gtx-language-table .grid-row.data-row',
} as const;

/** IDs of the create node wizard steps. The "Forms" step is only shown when the feature is activated, which it isn't by default. */
const WIZARD_STEP = {
    PROPERTIES: 0,
    PUBLISHING: 1,
    FEATURES: 2,
    LANGUAGES: 3,
} as const;

const CLASS_ACTIVE = /is-active/;
const CLASS_DISABLED = /disabled/;

test.describe('Node Settings', () => {

    const IMPORTER = new EntityImporter();

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

        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
    });

    test.describe('Properties Tab', () => {
        // SUP-19754: meshProjectName field was always empty in the edit view,
        // even when set via REST and returned by the API.
        test('should show the saved value in meshProjectName when opening node settings', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19754',
            }],
        }, async ({ page }) => {
            const MESH_PROJECT_NAME = 'sup-19754-test-project';
            const node = IMPORTER.get(NODE_MINIMAL);

            await test.step('Set meshProjectName via REST', async () => {
                await IMPORTER.client.node.update(node.id, {
                    node: {
                        meshProjectName: MESH_PROJECT_NAME,
                    },
                }).send();
            });

            await test.step('Navigate to Nodes module', async () => {
                const master = await navigateToModule(page, 'nodes');
                const masterTable = master.locator(SELECTORS.MASTER).or(master);

                const row = await findTableRowById(masterTable, node.id);
                await clickTableRow(row);

                const editor = page.locator(SELECTORS.EDITOR);
                await expect(editor).toBeVisible();
            });

            await test.step('meshProjectName field should not be empty', async () => {
                const input = page.locator(SELECTORS.MESH_PROJECT_NAME_INPUT);
                await expect(input).toBeVisible();
                await expect(input).toHaveValue(MESH_PROJECT_NAME);
            });
        });
    });

    test.describe('Create Node Wizard', () => {
        const NODE_NAME = 'Wizard Test Node';
        const NODE_HOST = 'wizard-test.localhost';

        let wizard: Locator;
        let createNodeRequestCount: number;

        test.beforeEach(async ({ page }) => {
            // Count the requests which actually create a node
            createNodeRequestCount = 0;
            onRequest(page, matchRequest('PUT', '/rest/node'), () => createNodeRequestCount++);

            await test.step('Open the Create Node Wizard', async () => {
                const module = await navigateToModule(page, 'nodes');
                await findEntityTableActionButton(module, 'create').click();

                wizard = page.locator(SELECTORS.WIZARD);
                await expect(findTab(WIZARD_STEP.PROPERTIES)).toHaveClass(CLASS_ACTIVE);
            });
        });

        function findTab(step: number): Locator {
            return wizard.locator(`${SELECTORS.TAB_LINK}[data-id="${step}"]`);
        }

        async function fillRequiredProperties(page: Page): Promise<void> {
            await page.locator(SELECTORS.NAME_INPUT).fill(NODE_NAME);
            await page.locator(SELECTORS.HOST_INPUT).fill(NODE_HOST);
        }

        test.describe('Tab Navigation', () => {
            test('should not allow selecting the following steps while the current step is invalid', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async () => {
                await test.step('All following steps should be disabled', async () => {
                    await expect(findTab(WIZARD_STEP.PUBLISHING)).toHaveClass(CLASS_DISABLED);
                    await expect(findTab(WIZARD_STEP.FEATURES)).toHaveClass(CLASS_DISABLED);
                    await expect(findTab(WIZARD_STEP.LANGUAGES)).toHaveClass(CLASS_DISABLED);
                });

                await test.step('Clicking a disabled step should not change the active step', async () => {
                    await selectTab(wizard, WIZARD_STEP.LANGUAGES);
                    await expect(findTab(WIZARD_STEP.PROPERTIES)).toHaveClass(CLASS_ACTIVE);
                });
            });

            test('should allow selecting the following steps once the steps in between are valid', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async ({ page }) => {
                await fillRequiredProperties(page);

                await expect(findTab(WIZARD_STEP.PUBLISHING)).not.toHaveClass(CLASS_DISABLED);
                await expect(findTab(WIZARD_STEP.FEATURES)).not.toHaveClass(CLASS_DISABLED);
                await expect(findTab(WIZARD_STEP.LANGUAGES)).not.toHaveClass(CLASS_DISABLED);
            });

            test('should jump forward to the last step without creating the node', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async ({ page }) => {
                await fillRequiredProperties(page);

                const languagesTab = await selectTab(wizard, WIZARD_STEP.LANGUAGES);

                await expect(findTab(WIZARD_STEP.LANGUAGES)).toHaveClass(CLASS_ACTIVE);
                await expect(languagesTab.locator(SELECTORS.LANGUAGE_ROW).first()).toBeVisible();
                expect(createNodeRequestCount).toBe(0);
            });

            test('should go back to a previous step and keep the entered values', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async ({ page }) => {
                await fillRequiredProperties(page);

                await test.step('Jump forward to the last step', async () => {
                    await selectTab(wizard, WIZARD_STEP.LANGUAGES);
                    await expect(findTab(WIZARD_STEP.LANGUAGES)).toHaveClass(CLASS_ACTIVE);
                });

                await test.step('Go back to the first step', async () => {
                    await selectTab(wizard, WIZARD_STEP.PROPERTIES);
                    await expect(findTab(WIZARD_STEP.PROPERTIES)).toHaveClass(CLASS_ACTIVE);
                    await expect(page.locator(SELECTORS.NAME_INPUT)).toHaveValue(NODE_NAME);
                    await expect(page.locator(SELECTORS.HOST_INPUT)).toHaveValue(NODE_HOST);
                });
            });

            test('should disable the following steps again when a previous step becomes invalid', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async ({ page }) => {
                await fillRequiredProperties(page);

                await test.step('Navigate forward and back again', async () => {
                    await selectTab(wizard, WIZARD_STEP.PUBLISHING);
                    await expect(findTab(WIZARD_STEP.PUBLISHING)).toHaveClass(CLASS_ACTIVE);
                    await selectTab(wizard, WIZARD_STEP.PROPERTIES);
                    await expect(findTab(WIZARD_STEP.PROPERTIES)).toHaveClass(CLASS_ACTIVE);
                });

                await test.step('Invalidate the first step', async () => {
                    await page.locator(SELECTORS.NAME_INPUT).clear();

                    await expect(findTab(WIZARD_STEP.PUBLISHING)).toHaveClass(CLASS_DISABLED);
                    await selectTab(wizard, WIZARD_STEP.LANGUAGES);
                    await expect(findTab(WIZARD_STEP.PROPERTIES)).toHaveClass(CLASS_ACTIVE);
                });
            });

            test('should create exactly one node when finishing the wizard after navigating via the tabs', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-20153',
                }],
            }, async ({ page }) => {
                await fillRequiredProperties(page);

                await test.step('Jump forward to the last step and select a language', async () => {
                    const languagesTab = await selectTab(wizard, WIZARD_STEP.LANGUAGES);
                    await expect(findTab(WIZARD_STEP.LANGUAGES)).toHaveClass(CLASS_ACTIVE);
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
});
