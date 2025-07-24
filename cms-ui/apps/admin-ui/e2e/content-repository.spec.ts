import { ContentRepository, Variant } from '@gentics/cms-models';
import { GCMSRestClientRequestError, RequestMethod } from '@gentics/cms-rest-client';
import {
    BASIC_TEMPLATE_ID,
    clickTableRow,
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
    isVariant,
    matchesPath,
    minimalNode,
    schedulePublisher,
    selectTableRow,
    TestSize,
} from '@gentics/e2e-utils';
import { UserUpdateRequest } from '@gentics/mesh-models';
import { expect, Locator, test } from '@playwright/test';
import { AUTH, AUTH_ADMIN, AUTH_MESH } from './common';
import {
    clickModalAction,
    findEntityTableActionButton,
    loginWithCR,
    loginWithForm,
    logoutMeshManagement,
    navigateToApp,
    navigateToModule,
    selectTab,
} from './helpers';

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
        NAME_INPUT: 'gtx-input[formcontrolname="name"] input[type="text"]',
    },
    LOGIN_FORM: {
        USERNAME: 'gtx-input[formcontrolname="username"] input',
        PASSWORD: 'gtx-input[formcontrolname="password"] input',
        NEW_PASSWORD: 'gtx-input[formcontrolname="newPassword"] input',
        SUBMIT: 'button[type="submit"]:not([disabled])',
    },
    FORM: {
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

    test.beforeAll(async ({ request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request }, testInfo) => {
        testInfo.setTimeout(120_000);
        IMPORTER.setApiContext(request);

        // Clean and setup test data
        await IMPORTER.cleanupTest();
        await IMPORTER.syncPackages(TestSize.MINIMAL);
        await IMPORTER.setupTest(TestSize.MINIMAL);

        testCr = IMPORTER.get(BASIC_TEMPLATE_ID as any) as any;

        await navigateToApp(page);
        await loginWithForm(page, AUTH_ADMIN);

        await navigateToModule(page, 'content-repositories');

        master = page.locator('gtx-content-repository-master');
        await master.waitFor({ state: 'visible' });
    });

    test('should have content repositories listed', async () => {
        const rows = master.locator('gtx-table .grid-row.data-row');
        await rows.waitFor({ state: 'visible' });
        await expect(rows).toHaveCount(1);
    });

    test('should open the details on click', async ({ page }) => {
        const row = master.locator('gtx-table .grid-row.data-row[data-id="1"]');
        await row.waitFor({ state: 'visible' });
        await clickTableRow(row);
        await expect(row).toHaveClass(CLASS_ACTIVE);

        const editor = page.locator(SELECTORS.EDITOR);
        await expect(editor).toBeVisible();
    });

    test('should be possible to select the management tab', async ({ page }) => {
        const row = findTableRowById(master, testCr.id);
        await row.waitFor({ state: 'visible' });
        await clickTableRow(row);

        const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
        await selectTab(tabs, 'management');

        const management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
        await management.waitFor({ state: 'visible' });
    });

    test.describe('Mesh Management', () => {
        test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

        let management: Locator;
        let managementContent: Locator;

        test.beforeEach(async ({ page }) => {
            const row = findTableRowById(master, testCr.id);
            await row.waitFor({ state: 'visible' });
            await clickTableRow(row);

            const tabs = page.locator(`${SELECTORS.EDITOR} ${SELECTORS.TABS.CONTAINER}`);
            await selectTab(tabs, 'management');

            management = tabs.locator(SELECTORS.TABS.MANAGEMENT);
            await management.waitFor({ state: 'visible' });
            managementContent = management.locator('.management-content');
        });

        test('should be possible to login via manual credentials and to logout again', async ({ page }) => {
            expect(await managementContent.isVisible()).toBe(false);

            await loginWithForm(management.locator('.login-form'), AUTH_MESH);
            await managementContent.waitFor({ state: 'visible' });

            await logoutMeshManagement(page);
            expect(await managementContent.isVisible()).toBe(false);
        });

        test('should be possible to login via CR credentials and to logout again', async ({ page }) => {
            expect(await managementContent.isVisible()).toBe(false);

            await loginWithCR(page);
            await managementContent.waitFor({ state: 'visible' });

            await logoutMeshManagement(page);
            expect(await managementContent.isVisible()).toBe(false);
        });

        test.describe('Login Gate forces new password', () => {

            // Cleanup of the admin-user in case the test broke somehow which would leave us
            // with a broken user and break all other tests which need the user for login.
            test.afterEach(async () => {
                let loggedIn = false;

                try {
                    // Check if we can login with regular login data
                    const res: any = await IMPORTER.client.contentRepository.proxyLogin(testCr.id).send();

                    // If we need a pw-reset, then we do it with a manual login which sets the new PW
                    if (res?.i18nKey === 'auth_login_password_change_required') {
                        await IMPORTER.client.executeMappedJsonRequest(RequestMethod.POST, `/contentrepositories/${testCr.id}/proxy/api/v2/auth/login`, {
                            username: AUTH[AUTH_MESH].username,
                            password: AUTH[AUTH_MESH].password,
                            newPassword: AUTH[AUTH_MESH].newPassword,
                        }).send();
                        loggedIn = true;
                    }

                    // For some reason, sometimes errornous responses are not thrown as actual errors.
                    // Since we can't login with the default login data, then that means we have to reset the PW now.
                    if (!res.token) {
                        throw new GCMSRestClientRequestError('Invalid response for proxy-login', null, 400, '', res);
                    }

                    return;
                } catch (err) {
                    if (!loggedIn) {
                        // Login with the temp password
                        await IMPORTER.client.executeMappedJsonRequest(RequestMethod.POST, `/contentrepositories/${testCr.id}/proxy/api/v2/auth/login`, {
                            username: AUTH[AUTH_MESH].username,
                            password: AUTH[AUTH_MESH].newPassword,
                        }).send();
                    }

                    // Get our user so we can update it
                    const me = await IMPORTER.client.executeMappedJsonRequest(RequestMethod.GET, `/contentrepositories/${testCr.id}/proxy/api/v2/auth/me`).send();
                    // Reset the password to the original
                    await IMPORTER.client.executeMappedJsonRequest(RequestMethod.POST, `/contentrepositories/${testCr.id}/proxy/api/v2/users/${me.uuid}`, {
                        password: AUTH[AUTH_MESH].password,
                    }).send();
                }
            });

            test('should force a new password, apply a new one, and reset it manually to the original one', async ({ page }) => {
                let userRow: Locator;

                await test.step('Login and force PW reset', async () => {
                    const usersReq = page.waitForResponse(response =>
                        response.ok() && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/users'),
                    );

                    await loginWithCR(page, true);
                    await selectTab(page, 'users');
                    await usersReq;

                    // Edit user and force password change
                    userRow = findTableRowByText(managementContent, AUTH[AUTH_MESH].username);
                    await findTableAction(userRow, 'edit').click();

                    const userModal = page.locator('gtx-mesh-user-modal');
                    await userModal.locator(SELECTORS.FORM.FORCE_PASSWORD).click();
                    await clickModalAction(userModal, 'confirm');

                    await logoutMeshManagement(page);
                });

                await test.step('Login with CR data should require password-reset', async () => {
                    // Login with CR should not be possible
                    await loginWithCR(page, false);
                    expect(managementContent).toBeVisible({ visible: false });
                });

                await test.step('Login with additional new password', async () => {
                    // Fill in the new password and submit the login again
                    const loginForm = management.locator('.login-form');
                    const newPassword = loginForm.locator(SELECTORS.LOGIN_FORM.NEW_PASSWORD);
                    await newPassword.waitFor({ state: 'visible' });

                    await loginForm.locator(SELECTORS.LOGIN_FORM.USERNAME).fill(AUTH[AUTH_MESH].username);
                    await loginForm.locator(SELECTORS.LOGIN_FORM.PASSWORD).fill(AUTH[AUTH_MESH].password);
                    await newPassword.fill(AUTH[AUTH_MESH].newPassword);

                    const pwResetLoginReq = page.waitForResponse(response =>
                        response.ok()
                            && response.request().method() === 'POST'
                            && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/auth/login'),
                    );
                    await loginForm.locator(SELECTORS.LOGIN_FORM.SUBMIT).click();
                    await pwResetLoginReq;

                    await managementContent.waitFor({ state: 'visible' });
                });

                await test.step('Reset the password to the original by editing own user', async () => {
                    // Reset password to original
                    await selectTab(page, 'users');
                    await findTableAction(userRow, 'edit').click();

                    const userModal = page.locator('gtx-mesh-user-modal');
                    await userModal.locator(SELECTORS.FORM.PASSWORD_CHECKBOX).click();
                    await userModal.locator(SELECTORS.FORM.PASSWORD_INPUTS).first().fill(AUTH[AUTH_MESH].password);
                    await userModal.locator(SELECTORS.FORM.PASSWORD_INPUTS).last().fill(AUTH[AUTH_MESH].password);

                    const updateReq = page.waitForResponse(response =>
                        response.ok()
                        && response.request().method() === 'POST'
                        && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/users/*'),
                    );
                    await clickModalAction(userModal, 'confirm');
                    const resetRes = await updateReq;
                    const resetReqData: UserUpdateRequest = resetRes.request().postDataJSON();
                    expect(resetReqData.password).toEqual(AUTH[AUTH_MESH].password);
                });

                await test.step('Login with CR data should work again', async () => {
                    // Logout and verify CR login works again
                    await logoutMeshManagement(page);
                    await loginWithCR(page);
                    await managementContent.waitFor({ state: 'visible' });
                });
            });
        });

        test.describe('Projects', () => {
            test.slow();

            test.beforeEach(async ({ page }) => {
                await IMPORTER.deleteMeshProjects();
                await IMPORTER.importData([schedulePublisher]);
                await IMPORTER.executeSchedule(schedulePublisher);
                await loginWithCR(page);
            });

            test.afterEach(async () => {
                await IMPORTER.deleteMeshProjects();
            });

            test('should be possible to create a new project', async ({ page }) => {
                const NEW_PROJECT_NAME = 'New Project';

                await selectTab(page, 'projects');

                await findEntityTableActionButton(managementContent, 'create').click();

                const createModal = page.locator(SELECTORS.PROJECT.MODAL);

                await createModal.locator(SELECTORS.PROJECT.NAME_INPUT)
                    .fill(NEW_PROJECT_NAME);

                await test.step('Select the schema "folder" as root schema', async () => {
                    await createModal.locator(SELECTORS.PROJECT.SCHEMA_PICKER)
                        .click();

                    const schemaSelectModal = page.locator(SELECTORS.PROJECT.SCHEMA_MODAL);
                    const schemaRow = findTableRowByText(schemaSelectModal, 'folder', true);
                    await schemaRow.waitFor({ state: 'visible' });
                    await selectTableRow(schemaRow);
                    await clickModalAction(schemaSelectModal, 'confirm');
                });

                await clickModalAction(createModal, 'confirm');

                await test.step('Delete the newly created Project', async () => {
                    const projectRow = findTableRowByText(managementContent, NEW_PROJECT_NAME);
                    await expect(projectRow).toBeVisible();

                    await findTableAction(projectRow, 'delete').click();

                    await clickModalAction(page, 'confirm');
                });
            });
        });

        test.describe('Role Permissions', () => {
            test.slow();

            test.beforeEach(async ({ page }) => {
                await IMPORTER.deleteMeshProjects();
                await IMPORTER.importData([schedulePublisher]);
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

                const permModal = page.locator('gtx-mesh-role-permissions-modal');

                await test.step('Navigate to the target node in the trable', async () => {
                    // Navigate through project structure
                    const projectsRow = findTrableRowById(permModal, '_projects');
                    await projectsRow.waitFor({ timeout: 60_000 });
                    await expandTrableRow(projectsRow);

                    const exampleRow = findTrableRowByText(permModal, CR_PREFIX_MESH);
                    await exampleRow.waitFor({ state: 'visible' });
                    const projectId = await exampleRow.evaluate((el) => {
                        return el.getAttribute('data-id');
                    })
                    await expandTrableRow(exampleRow);

                    const nodesRow = findTrableRowById(permModal, `_project_${projectId}_nodes`);
                    await nodesRow.waitFor({ timeout: 60_000 });
                    await expandTrableRow(nodesRow);
                });

                // Verify initial state
                const minimalRow = findTrableRowByText(permModal, IMPORTER.get(minimalNode).name);
                await minimalRow.waitFor({ state: 'visible' });
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).not.toHaveClass(CLASS_GRANTED);

                await test.step('Edit permissions for the node', async () => {
                    await findTableAction(minimalRow, 'edit').click();
                    const editModal = page.locator('gtx-mesh-role-permissions-edit-modal');
                    await editModal.locator('gtx-checkbox[formcontrolname="readPublished"] label').click();

                    const loadRequest = page.waitForResponse(response =>
                        response.ok()
                        && matchesPath(response.url(), '/rest/contentrepositories/'),
                    );

                    await clickModalAction(editModal, 'confirm');
                    await loadRequest;
                });

                // Verify permission was set
                await expect(minimalRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);

                await test.step('Apply permissions recursively to child nodes', async () => {
                    // Apply permissions recursively
                    const recursiveLoadReq = page.waitForResponse(response =>
                        response.ok()
                            && response.request().method() === 'GET'
                            && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/roles/*/permissions/projects/*/nodes/*'),
                    );
                    await findTableAction(minimalRow, 'applyRecursive').click();
                    await clickModalAction(page, 'confirm');
                    await recursiveLoadReq;

                    // Load/Open child elements first
                    const childLoad = page.waitForResponse(response =>
                        response.ok()
                        && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/*/graphql'),
                    );
                    await expandTrableRow(minimalRow);
                    await childLoad;

                    // Verify permissions have been set
                    const folderARow = findTrableRowByText(permModal, IMPORTER.get(folderA).name);
                    const folderBRow = findTrableRowByText(permModal, IMPORTER.get(folderB).name);
                    await expect(folderARow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);
                    await expect(folderBRow.locator('.permission-icon[data-id="readPublished"]')).toHaveClass(CLASS_GRANTED);
                });

                // Close modal
                await clickModalAction(page, 'cancel');
            });
        });
    });
});
