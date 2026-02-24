import { AccessControlledType, GcmsPermission, ObjectPropertiesObjectType, ObjectPropertyUpdateRequest } from "@gentics/cms-models";
import {
    clickModalAction,
    clickTableRow,
    CONSTRUCT_BOOLEAN,
    EntityImporter,
    findTableRowById,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    LANGUAGE_DE,
    LANGUAGE_EN,
    LANGUAGE_ID_DE,
    LANGUAGE_ID_EN,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    OBJECT_PROPERTY_FOLDER_COLOR,
    pickSelectValue,
    setI18nGroupLanguage,
    TestSize,
    UserImportData,
    waitForResponseFrom
} from "@gentics/e2e-utils";
import { cloneWithSymbols } from "@gentics/ui-core/utils/clone-with-symbols";
import { expect, Locator, Page, Response, test } from "@playwright/test";
import { navigateToModule } from "./helpers";

test.describe('Object Properties Module', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'objectproperty';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_admin`,

        description: 'Object Properties: Admin',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_admin`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'Object Properties',
        lastName: 'Admin',
        login: `${NAMESPACE}_admin`,
        password: 'iamobjpropadmin',
    };

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

    test.beforeEach(async ({ request, context }) => {
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
    });

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<Locator> {
        await test.step('Test User Setup', async () => {
            const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
            TEST_GROUP.permissions = permissions;

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });

        let module: Locator;
        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            module = await navigateToModule(page, 'object-properties');
        });

        return module;
    }

    test('create object property', async ({ page }) => {
        const TEST_CONSTRUCT = (await IMPORTER.client.construct.get(CONSTRUCT_BOOLEAN).send()).construct;

        const module = await setupWithPermissions(page, [
            // General Admin-UI Permissions
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            // Object Property Permissions
            {
                type: AccessControlledType.CONTENT_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            // Permissions to load related/required elements for creating/editing
            {
                type: AccessControlledType.CONSTRUCT_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.LANGUAGE_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ]);

        const master = module.locator('gtx-object-property-master');
        const table = master.locator('gtx-object-property-table');

        await test.step('Create the Object Property', async () => {
            await table.locator('.entity-table-actions-bar [data-action="create"]').click();
            const modal = page.locator('gtx-create-object-property-modal');
            const form = modal.locator('gtx-object-property-properties form');

            await form.locator('[formcontrolname="keyword"] input').fill('testcreate');
            await pickSelectValue(form.locator('[formcontrolname="type"]'), ObjectPropertiesObjectType.FOLDER);
            await pickSelectValue(form.locator('[formcontrolname="constructId"]'), TEST_CONSTRUCT.id);

            const i18nGroup = form.locator('gtx-i18n-panel-group');
            await setI18nGroupLanguage(i18nGroup, LANGUAGE_ID_DE);
            await form.locator('[formcontrolname="nameI18n"] input').fill('Test DE');
            await setI18nGroupLanguage(i18nGroup, LANGUAGE_ID_EN);
            await form.locator('[formcontrolname="nameI18n"] input').fill('Test EN');

            const createReq = waitForResponseFrom(page, 'POST', '/rest/objectproperty');
            await clickModalAction(modal, 'confirm');
            await createReq;
        });
    });

    test('edit object property', async ({ page }) => {
        const TEST_OBJ_PROP = (await IMPORTER.client.objectProperty.get(OBJECT_PROPERTY_FOLDER_COLOR).send()).objectProperty;
        const module = await setupWithPermissions(page, [
            // General Admin-UI Permissions
            {
                type: AccessControlledType.ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            // Object Property Permissions
            {
                type: AccessControlledType.CONTENT_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_ADMIN,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10002',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
            // Permissions to load related/required elements for creating/editing
            {
                type: AccessControlledType.CONSTRUCT_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
            {
                type: AccessControlledType.LANGUAGE_ADMIN,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                ],
            },
        ]);

        const master = module.locator('gtx-object-property-master');
        const table = master.locator('gtx-object-property-table');
        const row = findTableRowById(table, TEST_OBJ_PROP.id);

        let listReq: Promise<Response>;

        await test.step('Edit the Object Property', async () => {
            await clickTableRow(row);

            const editor = page.locator('gtx-object-property-editor');
            const form = editor.locator('gtx-object-property-properties form');

            const i18nGroup = form.locator('gtx-i18n-panel-group');
            await setI18nGroupLanguage(i18nGroup, LANGUAGE_ID_DE);
            await form.locator('[formcontrolname="nameI18n"] input').fill('Test DE');
            await setI18nGroupLanguage(i18nGroup, LANGUAGE_ID_EN);
            await form.locator('[formcontrolname="nameI18n"] input').fill('Test EN');

            const updateReq = waitForResponseFrom(page, 'PUT', `/rest/objectproperty/${TEST_OBJ_PROP.id}`);
            listReq = waitForResponseFrom(page, 'GET', '/rest/objectproperty');
            await editor.locator('gtx-entity-detail-header [data-action="save"]').click();
            const updateRes = await updateReq;
            const updateData = updateRes.request().postDataJSON() as ObjectPropertyUpdateRequest;

            expect(updateData.nameI18n).toEqual({
                [LANGUAGE_DE]: 'Test DE',
                [LANGUAGE_EN]: 'Test EN',
            });
        });

        await test.step('Update list', async () => {
            await listReq;
            await expect(row.locator('.data-column[data-id="name"]')).toHaveText('Test DE');
        });
    });
});
