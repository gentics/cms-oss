import { AccessControlledType } from '@gentics/cms-models';
import { EntityImporter, schedulePublisher, TestSize } from '@gentics/e2e-utils';
import { test, expect } from '@playwright/test';
import { AUTH_ADMIN } from './common';
import {
    loginWithForm,
    navigateToApp,
    navigateToModule,
    findTableRowById,
    selectTab,
    isMeshManagementVisible,
    loginWithCR,
    logoutMeshManagement,
    findTableRowByText,
    clickTableRowAction,
    findTrableRowByText,
    expandTrableRow,
    clickModalAction,
} from './helpers';

// Test data constants
const TEST_DATA = {
    NEW_PROJECT_NAME: 'New Project',
    TRABLE_PROJECTS: 'Projekte',
    TRABLE_NODES: 'Nodes',
    EXAMPLE_PROJECT: 'example',
    MINIMAL: 'Minimal',
    FOLDER_A: 'Folder A',
    FOLDER_B: 'Folder B',
    CR_ID: '1',
} as const;

// Selector constants
const SELECTORS = {
    EDITOR: 'gtx-content-repository-editor',
    TABS: {
        CONTAINER: '.gtx-entity-detail > gtx-tabs',
        MANAGEMENT: 'gtx-mesh-management',
    },
    PROJECT: {
        TABLE: 'gtx-mesh-project-table',
        MODAL: 'gtx-mesh-project-modal',
        SCHEMA_PICKER: 'gtx-mesh-schema-picker .select-button',
        SCHEMA_MODAL: 'gtx-mesh-select-schema-modal',
    },
    FORM: {
        NAME_INPUT: 'gtx-input[formcontrolname="name"] input[type="text"]',
        PASSWORD_INPUT: '.login-form input[type="password"]:nth(1)',
        SUBMIT: '.login-form button[type="submit"]',
        PASSWORD_CHECKBOX: '.password-checkbox label',
        PASSWORD_INPUTS: '[data-control="password"] input',
        FORCE_PASSWORD: '[data-control="forcePasswordChange"] label',
    },
} as const;

test.describe('Content Repositories Module', () => {
    const IMPORTER = new EntityImporter({
        logRequests: false,
        logImports: false,
    });

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request }) => {
        // Reset importer client to avoid 401 errors
        IMPORTER.setApiContext(request);

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupTest(TestSize.MINIMAL);

        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        // Wait for table loading
        const tableLoadPromise = page.waitForResponse(response =>
            response.url().includes('/rest/contentrepositories') && response.ok(),
        );

        await navigateToModule(page, 'content-repositories', AccessControlledType.CONTENT_ADMIN);
        await tableLoadPromise;
    });

    test('should have content repositories listed', async ({ page }) => {
        const rows = page.locator('gtx-table .grid-row');
        await expect(rows).toHaveCount(1);
    });

    test('should open the details on click', async ({ page }) => {
        const row = await findTableRowById(page, TEST_DATA.CR_ID);
        await row.click({ force: true });
        await expect(row).toHaveClass(/active/);

        const editor = page.locator(SELECTORS.EDITOR);
        await expect(editor).toBeVisible();
    });

    test('should be possible to select the management tab', async ({ page }) => {
        const row = await findTableRowById(page, TEST_DATA.CR_ID);
        await row.click({ force: true });

        const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
        await selectTab(tabs, 'management');

        const management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
        await expect(management).toBeVisible();
    });

    test.describe('Mesh Management', () => {
        test.beforeEach(async ({ page }) => {
            const row = await findTableRowById(page, TEST_DATA.CR_ID);
            await row.click({ force: true });

            const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
            await selectTab(tabs, 'management');

            const management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
            await expect(management).toBeVisible();
        });

        test('should be possible to login via manual credentials and to logout again', async ({ page }) => {
            await expect(await isMeshManagementVisible(page)).toBe(false);

            await loginWithForm(page, 'mesh');
            await expect(await isMeshManagementVisible(page)).toBe(true);

            await logoutMeshManagement(page);
            await expect(await isMeshManagementVisible(page)).toBe(false);
        });

        test('should be possible to login via CR credentials and to logout again', async ({ page }) => {
            await expect(await isMeshManagementVisible(page)).toBe(false);

            await loginWithCR(page);
            await expect(await isMeshManagementVisible(page)).toBe(true);

            await logoutMeshManagement(page);
            await expect(await isMeshManagementVisible(page)).toBe(false);
        });

        test('should force a new password, apply a new one, and reset it manually to the original one', async ({ page }) => {
            const auth = require('../fixtures/auth.json');

            await loginWithCR(page);

            // Edit user and force password change
            await selectTab(page, 'users');
            const userRow = await findTableRowByText(page, auth.mesh.username);
            await clickTableRowAction(userRow, 'edit');

            await page.locator(SELECTORS.FORM.FORCE_PASSWORD).click();
            await clickModalAction(page, 'confirm');

            await logoutMeshManagement(page);

            // Login with CR should not be possible
            await loginWithCR(page);
            await expect(await isMeshManagementVisible(page)).toBe(false);

            // Attempt manual login and set new password
            await loginWithForm(page, 'mesh');

            await page.locator(SELECTORS.FORM.PASSWORD_INPUT).fill(auth.mesh.newPassword);
            await page.locator(SELECTORS.FORM.SUBMIT).click();

            await expect(await isMeshManagementVisible(page)).toBe(true);

            // Reset password to original
            await selectTab(page, 'users');
            await clickTableRowAction(userRow, 'edit');

            await page.locator(SELECTORS.FORM.PASSWORD_CHECKBOX).click();
            await page.locator(SELECTORS.FORM.PASSWORD_INPUTS).first().fill(auth.mesh.password);
            await page.locator(SELECTORS.FORM.PASSWORD_INPUTS).last().fill(auth.mesh.password);

            await clickModalAction(page, 'confirm');

            // Logout and verify CR login works again
            await logoutMeshManagement(page);
            await loginWithCR(page);
            await expect(await isMeshManagementVisible(page)).toBe(true);
        });

        test.describe('Projects', () => {
            test.beforeEach(async ({ page }) => {
                await loginWithCR(page);
            });

            test.afterEach(async ({ page }) => {
                await logoutMeshManagement(page);
            });

            test('should be possible to create a new project', async ({ page }) => {
                await selectTab(page, 'projects');

                await page.locator(`${SELECTORS.PROJECT.TABLE} [data-action="create"]`).click();

                await page.locator(`${SELECTORS.PROJECT.MODAL} ${SELECTORS.FORM.NAME_INPUT}`).fill(TEST_DATA.NEW_PROJECT_NAME);

                await page.locator(`${SELECTORS.PROJECT.MODAL} ${SELECTORS.PROJECT.SCHEMA_PICKER}`).click();
                const schemaRow = await findTableRowByText(page, 'folder');
                await schemaRow.locator('.select-column gtx-checkbox label').click();
                await clickModalAction(page, 'confirm');

                await clickModalAction(page, 'confirm');

                const projectRow = await findTableRowByText(page, TEST_DATA.NEW_PROJECT_NAME);
                await expect(projectRow).toBeVisible();

                await clickTableRowAction(projectRow, 'delete');
                await clickModalAction(page, 'confirm');
            });
        });

        test.describe('Role Permissions', () => {
            test.beforeEach(async ({ page }) => {
                await IMPORTER.deleteMeshProjects();
                await IMPORTER.executeSchedule(schedulePublisher);
                await loginWithCR(page);
            });

            test.afterEach(async () => {
                await IMPORTER.deleteMeshProjects();
            });

            test('should be possible to read and modify role permissions on projects', async ({ page }) => {
                await selectTab(page, 'roles');

                const anonymousRow = await findTableRowByText(page, 'anonymous');
                await clickTableRowAction(anonymousRow, 'managePermissions');

                // Navigate through project structure
                const projectsRow = await findTrableRowByText(page, TEST_DATA.TRABLE_PROJECTS);
                await expandTrableRow(projectsRow);

                const exampleRow = await findTrableRowByText(page, TEST_DATA.EXAMPLE_PROJECT);
                await expandTrableRow(exampleRow);

                const nodesRow = await findTrableRowByText(page, TEST_DATA.TRABLE_NODES);
                await expandTrableRow(nodesRow);

                // Verify initial state
                const minimalRow = await findTrableRowByText(page, TEST_DATA.MINIMAL);
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).not.toHaveClass(/granted/);

                // Edit permissions
                await clickTableRowAction(minimalRow, 'edit');
                await page.locator('gtx-mesh-role-permissions-edit-modal gtx-checkbox[formcontrolname="readPublished"] label').click();

                const loadRequest = page.waitForResponse(response =>
                    response.url().includes('/rest/contentrepositories/') && response.ok(),
                );
                await clickModalAction(page, 'confirm');
                await loadRequest;

                // Verify permission was set
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(/granted/);

                // Apply permissions recursively
                await clickTableRowAction(minimalRow, 'applyRecursive');
                await clickModalAction(page, 'confirm');

                // Expand minimal folder
                await expandTrableRow(minimalRow);

                // Verify recursive permissions
                const folderARow = await findTrableRowByText(page, TEST_DATA.FOLDER_A);
                const folderBRow = await findTrableRowByText(page, TEST_DATA.FOLDER_B);

                await expect(folderARow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(/granted/);
                await expect(folderBRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(/granted/);

                // Close modal
                await clickModalAction(page, 'cancel');
            });
        });
    });
});
