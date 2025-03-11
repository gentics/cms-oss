import { AccessControlledType, ContentRepository } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    clickTableRow,
    clickTableRowAction,
    CR_PREFIX_MESH,
    EntityImporter,
    expandTrableRow,
    findTableAction,
    findTableRowById,
    findTableRowByText,
    findTrableRowById,
    findTrableRowByText,
    folderA,
    folderB,
    matchesPath,
    minimalNode,
    schedulePublisher,
    selectTableRow,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
import { AUTH, AUTH_ADMIN, AUTH_MESH } from './common';
import {
    clickModalAction,
    loginWithCR,
    loginWithForm,
    logoutMeshManagement,
    navigateToApp,
    navigateToModule,
    selectTab,
} from './helpers';
import { UserUpdateRequest } from '@gentics/mesh-models';

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

const CLASS_ACTIVE = /active/;
const CLASS_GRANTED = /granted/;

test.describe.configure({ mode: 'serial' });
test.describe('Content Repositories Module', () => {

    const IMPORTER = new EntityImporter({
        logRequests: false,
        logImports: false,
    });

    let testCr: ContentRepository;
    let master: Locator;

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

        testCr = IMPORTER.get(BASIC_TEMPLATE_ID as any) as any;

        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        // Wait for table loading
        const tableLoadPromise = page.waitForResponse(response =>
            matchesPath(response.url(), '/rest/contentrepositories') && response.ok(),
        );

        await navigateToModule(page, 'content-repositories', AccessControlledType.CONTENT_ADMIN);
        await tableLoadPromise;

        master = page.locator('gtx-content-repository-master');
        await master.waitFor();
    });

    test('should have content repositories listed', async ({ page }) => {
        const rows = master.locator('gtx-table .grid-row.data-row');
        await expect(rows).toHaveCount(1);
    });

    test('should open the details on click', async ({ page }) => {
        const row = findTableRowById(master, testCr.id);
        await clickTableRow(row);
        await expect(row).toHaveClass(CLASS_ACTIVE);

        const editor = page.locator(SELECTORS.EDITOR);
        await expect(editor).toBeVisible();
    });

    test('should be possible to select the management tab', async ({ page }) => {
        const row = findTableRowById(master, testCr.id);
        await clickTableRow(row);

        const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
        await selectTab(tabs, 'management');

        const management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
        await expect(management).toBeVisible();
    });

    test.describe('Mesh Management', () => {
        let management: Locator;
        let managementContent: Locator;

        test.beforeEach(async ({ page }) => {
            const row = findTableRowById(master, testCr.id);
            await clickTableRow(row);

            const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
            await selectTab(tabs, 'management');

            management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
            await expect(management).toBeVisible();
            managementContent = management.locator('.management-content');
        });

        test('should be possible to login via manual credentials and to logout again', async ({ page }) => {
            expect(await managementContent.isVisible()).toBe(false);

            const loginReq = page.waitForResponse(response =>
                response.ok() && (
                    matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/auth/login')
                    || matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/')
                ),
            );

            await loginWithForm(management, AUTH_MESH);
            await loginReq;
            expect(await managementContent.isVisible()).toBe(true);

            await logoutMeshManagement(page);
            expect(await managementContent.isVisible()).toBe(false);
        });

        test('should be possible to login via CR credentials and to logout again', async ({ page }) => {
            expect(await managementContent.isVisible()).toBe(false);

            await loginWithCR(page);
            expect(await managementContent.isVisible()).toBe(true);

            await logoutMeshManagement(page);
            expect(await managementContent.isVisible()).toBe(false);
        });

        test('should force a new password, apply a new one, and reset it manually to the original one', async ({ page }) => {
            const usersReq = page.waitForResponse(response =>
                response.ok() && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/users'),
            );

            await loginWithCR(page);
            await selectTab(page, 'users');
            await usersReq;

            // Edit user and force password change
            const userRow = findTableRowByText(managementContent, AUTH[AUTH_MESH].username);
            await findTableAction(userRow, 'edit').click();

            let userModal = page.locator('gtx-mesh-user-modal');
            await userModal.locator(SELECTORS.FORM.FORCE_PASSWORD).click();
            await clickModalAction(userModal, 'confirm');

            await logoutMeshManagement(page);

            // Login with CR should not be possible
            await loginWithCR(page);
            expect(await managementContent.isVisible()).toBe(false);

            // Attempt manual login and set new password
            await loginWithForm(page, AUTH_MESH);

            const updateReq = page.waitForResponse(response =>
                response.ok()
                && response.request().method() === 'POST'
                && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/users/*'),
            );

            await page.locator(SELECTORS.FORM.PASSWORD_INPUT).fill(AUTH[AUTH_MESH].newPassword);
            await page.locator(SELECTORS.FORM.SUBMIT).click();
            await updateReq;

            expect(await managementContent.isVisible()).toBe(true);

            // Reset password to original
            await selectTab(page, 'users');
            await findTableAction(userRow, 'edit').click();

            userModal = page.locator('gtx-mesh-user-modal');
            await userModal.locator(SELECTORS.FORM.PASSWORD_CHECKBOX).click();
            await userModal.locator(SELECTORS.FORM.PASSWORD_INPUTS).first().fill(AUTH[AUTH_MESH].password);
            await userModal.locator(SELECTORS.FORM.PASSWORD_INPUTS).last().fill(AUTH[AUTH_MESH].password);

            const resetReq = page.waitForResponse(response =>
                response.ok()
                && response.request().method() === 'POST'
                && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/users/*'),
            );
            await clickModalAction(userModal, 'confirm');
            const resetRes = await resetReq;
            const resetReqData: UserUpdateRequest = resetRes.request().postDataJSON();
            expect(resetReqData.password).toEqual(AUTH[AUTH_MESH].password);

            // Logout and verify CR login works again
            await logoutMeshManagement(page);

            await loginWithCR(page);
            expect(await managementContent.isVisible()).toBe(true);
        });

        test.describe('Projects', () => {
            test.beforeEach(async ({ page }) => {
                await loginWithCR(page);
            });

            test.afterEach(async ({ page }) => {
                await logoutMeshManagement(page);
            });

            test('should be possible to create a new project', async ({ page }) => {
                const NEW_PROJECT_NAME = 'New Project';

                await selectTab(page, 'projects');

                await managementContent.locator(`${SELECTORS.PROJECT.TABLE} [data-action="create"]`).click();

                const createModal = page.locator(SELECTORS.PROJECT.MODAL);

                await createModal.locator(SELECTORS.FORM.NAME_INPUT)
                    .fill(NEW_PROJECT_NAME);
                await createModal.locator(SELECTORS.PROJECT.SCHEMA_PICKER)
                    .click();

                const schemaSelectModal = page.locator(SELECTORS.PROJECT.SCHEMA_MODAL);
                const schemaRow = findTableRowByText(schemaSelectModal, 'folder');
                await selectTableRow(schemaRow);
                await clickModalAction(schemaSelectModal, 'confirm');

                await clickModalAction(createModal, 'confirm');

                const projectRow = findTableRowByText(managementContent, NEW_PROJECT_NAME);
                await expect(projectRow).toBeVisible();

                await findTableAction(projectRow, 'delete').click();

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

                const anonymousRow = findTableRowByText(page, 'anonymous');
                await findTableAction(anonymousRow, 'managePermissions').click();

                // Navigate through project structure
                const projectsRow = findTrableRowById(page, '_projects');
                await expandTrableRow(projectsRow);

                const exampleRow = findTrableRowByText(page, CR_PREFIX_MESH);
                const projectId = await exampleRow.evaluate((el) => {
                    return el.getAttribute('data-id');
                })
                await expandTrableRow(exampleRow);

                const nodesRow = findTrableRowById(page, `_project_${projectId}_nodes`);
                await expandTrableRow(nodesRow);

                // Verify initial state
                const minimalRow = findTrableRowByText(page, IMPORTER.get(minimalNode).name);
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).not.toHaveClass(CLASS_GRANTED);

                // Edit permissions
                await findTableAction(minimalRow, 'edit').click();
                await page.locator('gtx-mesh-role-permissions-edit-modal gtx-checkbox[formcontrolname="readPublished"] label').click();

                const loadRequest = page.waitForResponse(response =>
                    response.url().includes('/rest/contentrepositories/') && response.ok(),
                );
                await clickModalAction(page, 'confirm');
                await loadRequest;

                // Verify permission was set
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);

                // Apply permissions recursively
                await findTableAction(minimalRow, 'applyRecursive').click();
                await clickModalAction(page, 'confirm');

                // Expand minimal folder
                await expandTrableRow(minimalRow);

                // Verify recursive permissions
                const folderARow = findTrableRowById(page, IMPORTER.get(folderA).id);
                const folderBRow = findTrableRowById(page, IMPORTER.get(folderB).id);

                await expect(folderARow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);
                await expect(folderBRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);

                // Close modal
                await clickModalAction(page, 'cancel');
            });
        });
    });
});
