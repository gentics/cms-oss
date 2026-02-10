import { AccessControlledType, GcmsPermission, ObjectPropertiesObjectType, ObjectProperty, ObjectPropertyCreateRequest } from '@gentics/cms-models';
import {
    EntityImporter,
    FOLDER_A,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    ITEM_TYPE_FOLDER,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Locator, Page, test } from '@playwright/test';
import { itemAction, openPropertiesTab, selectNode, findItem, findList } from './helpers';

/**
 * Regression test for long/wrapped object-property tabs in folder properties.
 * Ensures the last entry is reachable/visible at 100% zoom (default).
 */

test.describe('Object Property Tabs Scrolling', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'objpropscroll';

    const OBJECT_PROPERTIES_TO_CREATE = 20;

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Object Property Scrolling: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'ObjectProperty',
        lastName: 'Scrolling',
        login: `${NAMESPACE}_editor`,
        password: 'test',
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

    async function setupWithPermissions(page: Page, permissions: ImportPermissions[]): Promise<void> {
        await test.step('Test User Setup', async () => {
            const TEST_GROUP = cloneWithSymbols(TEST_GROUP_BASE);
            TEST_GROUP.permissions = permissions;

            await IMPORTER.importData([
                TEST_GROUP,
                TEST_USER,
            ]);
        });

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, TEST_USER);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
        });
    }

    function longWrappedName(index: number): string {
        const padded = String(index).padStart(2, '0');
        return `ZZZ ${padded} Dies ist ein sehr langer Objekteigenschaftsname mit vielen Worten damit er im linken Tab-Menü umbrechen muss`;
    }

    function normalizeObjectPropertyKeyword(keyword: string): string {
        if (keyword.startsWith('object.')) {
            return keyword.substring('object.'.length);
        }
        return keyword;
    }

    async function ensureExpanded(group: Locator): Promise<void> {
        const isExpanded = await group.evaluate((el) => el.classList.contains('expanded'));
        if (!isExpanded) {
            await group.locator('.collapsible-header').click();
        }
    }

    async function cleanupCreatedObjectProperties(createdObjectProperties: ObjectProperty[]): Promise<void> {
        await IMPORTER.setupClient();

        const node = IMPORTER.get(NODE_MINIMAL);

        for (const op of createdObjectProperties) {
            try {
                try {
                    await IMPORTER.client.node.unassignObjectProperty(node.id, op.id).send();
                } catch (e) {
                }
                await IMPORTER.client.objectProperty.delete(op.id).send();
            } catch (e) {
            }
        }
    }

    async function createAndAssignFolderObjectProperties(runId: string): Promise<{ categoryId: number | string; created: ObjectProperty[] }> {
        await IMPORTER.setupClient();

        const existing = (await IMPORTER.client.objectProperty.list().send()).items || [];
        const byKeyword = existing.find((op) => op.keyword === 'test_color' && op.type === ObjectPropertiesObjectType.FOLDER);
        const byType = existing.find((op) => op.type === ObjectPropertiesObjectType.FOLDER);
        const base = byKeyword || byType;

        if (!base) {
            throw new Error('Could not find any existing folder object property to derive construct/category from.');
        }

        if (base.categoryId == null) {
            throw new Error(`Base folder object property "${base.keyword}" has no categoryId. Cannot target a tab-group reliably.`);
        }

        const node = IMPORTER.get(NODE_MINIMAL);
        const created: ObjectProperty[] = [];

        for (let i = 1; i <= OBJECT_PROPERTIES_TO_CREATE; i++) {
            const keyword = `zz_e2e_scroll_${runId}_${String(i).padStart(2, '0')}`;

            const payload: ObjectPropertyCreateRequest = {
                nameI18n: {
                    en: longWrappedName(i),
                    de: longWrappedName(i),
                },
                descriptionI18n: null,
                keyword,
                type: ObjectPropertiesObjectType.FOLDER,
                constructId: base.constructId,
                categoryId: base.categoryId,
                required: false,
                inheritable: false,
                syncContentset: false,
                syncChannelset: false,
                syncVariants: false,
                restricted: false,
            };

            const res = await IMPORTER.client.objectProperty.create(payload).send();
            created.push(res.objectProperty);

            await IMPORTER.client.node.assignObjectProperty(node.id, res.objectProperty.id).send();
        }

        return { categoryId: base.categoryId, created };
    }

    test('should keep long wrapped object-property list scrollable (100% zoom)', async ({ page }) => {
        test.setTimeout(120_000);

        const runId = `${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;

        let created: ObjectProperty[] = [];
        let categoryId: number | string;

        try {
            await setupWithPermissions(page, [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.READ_ITEMS, value: true },
                        { type: GcmsPermission.UPDATE_FOLDER, value: true },
                    ],
                },
                {
                    type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                    instanceId: `${ObjectPropertiesObjectType.FOLDER}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.UPDATE, value: true },
                    ],
                },
            ]);

            await test.step('Create & assign many long folder object-properties', async () => {
                const res = await createAndAssignFolderObjectProperties(runId);
                created = res.created;
                categoryId = res.categoryId;
            });

            await test.step('Reload UI to pick up new object-properties', async () => {
                await page.reload();
                await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
            });

            await test.step('Open folder properties', async () => {
                const folder = IMPORTER.get(FOLDER_A);
                const list = findList(page, ITEM_TYPE_FOLDER);
                const item = findItem(list, folder.id);

                await itemAction(item, 'properties');
            });

            await openPropertiesTab(page);

            const group = page.locator(`content-frame combined-properties-editor .properties-tabs .tab-group[data-id="${categoryId}"]`);
            await group.waitFor();

            await test.step('Expand object-property category group', async () => {
                await ensureExpanded(group);
            });

            const lastKeywordRaw = created[created.length - 1].keyword;
            const lastKeyword = normalizeObjectPropertyKeyword(lastKeywordRaw);
            const lastTab = group.locator(`.tab-link[data-id="object.${lastKeyword}"]`);

            await test.step('Scroll to last entry and assert it is visible', async () => {
                await lastTab.waitFor();
                await lastTab.scrollIntoViewIfNeeded();

                await expect(lastTab).toBeVisible();
                await expect(lastTab).toBeInViewport();

                const container = lastTab.locator('xpath=ancestor::div[contains(@class, "grouped-tabs")][1]');
                const [containerBox, tabBox] = await Promise.all([
                    container.boundingBox(),
                    lastTab.boundingBox(),
                ]);

                expect(containerBox, 'Expected properties tab list container to have a bounding box').not.toBeNull();
                expect(tabBox, 'Expected last tab to have a bounding box').not.toBeNull();

                // Haupttest -> Unterste Objekteigenschaft komplett sichtbar und nicht abgeschnitten
                expect(tabBox.y).toBeGreaterThanOrEqual(containerBox.y - 1);
                expect(tabBox.y + tabBox.height).toBeLessThanOrEqual(containerBox.y + containerBox.height + 1);
            });
        } finally {
            await test.step('Cleanup created object-properties', async () => {
                await cleanupCreatedObjectProperties(created);
            });
        }
    });
});
