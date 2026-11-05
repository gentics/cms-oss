import {
    AccessControlledType,
    FormSaveRequest,
    GcmsPermission,
    NodeFeature,
    Variant,
} from '@gentics/cms-models';
import { cloneWithSymbols } from '@gentics/common';
import {
    EntityImporter,
    EXT_FORM_ONE,
    FORM_ONE,
    FORM_THREE,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_USER,
    ImportPermissions,
    isVariant,
    ITEM_TYPE_FORM,
    LANGUAGE_EN,
    loginWithForm,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    TestSize,
    UserImportData,
} from '@gentics/e2e-utils';
import { expect, Page, test } from '@playwright/test';
import {
    editorAction,
    fgAddBlock,
    fgAddControl,
    fgFindDropContainer,
    fgFindEditSidebar,
    fgFindElement,
    fgMoveElement,
    fgMoveElementToPage,
    fgSelectElementTab,
    findItem,
    findList,
    itemAction,
    selectNode,
} from './helpers';

test.describe('Form Editing', () => {
    test.skip(() => !isVariant(Variant.ENTERPRISE), 'Requires Enterpise features');

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'formedit';

    const NEW_FORM_NAME = 'Hello World';
    const CHANGE_FORM_NAME = 'Hello World again';
    const NEW_FORM_DESCRIPTION = 'This is an example text';

    const FORM_TYPE_GENERIC = 'generic';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_editor`,

        description: 'Form Editing: Editor',
        name: `group_${NAMESPACE}_editor`,
        permissions: [],
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_editor`,

        group: TEST_GROUP_BASE,

        email: 'something@example.com',
        firstName: 'FormEditing',
        lastName: 'Editor',
        login: `${NAMESPACE}_editor`,
        password: 'testforms123',
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
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [NodeFeature.FORMS]: true,
            });
            const nodeId = IMPORTER.get(NODE_MINIMAL).id;
            await IMPORTER.client.form.assignConfiguration(FORM_TYPE_GENERIC, nodeId).send();
            await IMPORTER.importData([FORM_ONE, FORM_THREE]);
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

    test('should display the label value as title', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19032',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const LABEL_TEXT = 'Hello World';

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const grid = page.locator('content-frame gtx-form-grid');
            const el = await fgAddControl(grid, 'number');

            await expect(el).toBeVisible();
            await el.click();
            await expect(el).toContainClass('is-selected');

            const editSidebar = fgFindEditSidebar(grid);
            await expect(editSidebar).toBeVisible();

            const elLabel = el.locator('.element-container .element-title');
            const translationTab = await fgSelectElementTab(editSidebar, 'translations');
            const labelCtrl = translationTab.locator('[data-control="label"] input');

            await labelCtrl.fill(LABEL_TEXT);
            await expect(elLabel).toHaveText(LABEL_TEXT);
        });
    });

    test('should edit and save selectable-options correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19335',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_ONE);
        const KEY_TEXT = 'Hello World';
        const VALUE_TEXT = 'Foo Bar Content!';

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        await test.step('Edit Form', async () => {
            const grid = page.locator('content-frame gtx-form-grid');
            const el = await fgAddControl(grid, 'catalog');

            await expect(el).toBeVisible();
            await el.click();
            await expect(el).toContainClass('is-selected');

            const editSidebar = fgFindEditSidebar(grid);
            await expect(editSidebar).toBeVisible();

            const definitionTab = await fgSelectElementTab(editSidebar, 'definition');
            const keyOptions = definitionTab.locator('[data-control="selectOptions"]');

            // Add a new option, fill it out, save
            await keyOptions.locator('[data-action="add-option"]').click();
            await keyOptions.locator('gtx-input input').fill(KEY_TEXT);

            // Open the translations tab, and edit the label text
            const translationTab = await fgSelectElementTab(editSidebar, 'translations');
            const valueOptions = translationTab.locator('[data-control="selectOptions"]');

            // Should have the key as default value set initially
            await expect(valueOptions.locator('input')).toHaveValue(KEY_TEXT);
            await valueOptions.locator('input').fill(VALUE_TEXT);
        });

        await test.step('Save and Validate', async () => {
            const saveReq = page.waitForResponse(matchRequest('PUT', '/rest/form/*'));
            await editorAction(page, 'save');
            const res = await saveReq;
            const req: FormSaveRequest = res.request().postDataJSON();

            const props = Object.entries(req.data.schema?.properties || {});
            expect(props).toHaveLength(1);

            const el = props[0][1];
            expect(el.formGridOptions.selectOptions).toEqual([{
                _defaulted: [], // Internal structure
                value: KEY_TEXT,
                label: {
                    [LANGUAGE_EN]: VALUE_TEXT,
                },
            }]);
        });
    });

    test('should correctly restrict/allow moving elements around via drag and drop', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20213',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_THREE);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        const grid = page.locator('content-frame gtx-form-grid');
        const firstPageTabLabel = grid.locator('gtx-form-page-manager .page-tab[data-page-index="0"] .page-tab-label');

        await test.step('Should display all imported elements with the correct structure', async () => {
            const root = fgFindDropContainer(grid);
            await expect(root.locator('> .form-item')).toHaveCount(3);
            await expect(fgFindElement(grid, 'input1')).toHaveAttribute('data-element-id', 'input1');
            await expect(fgFindElement(grid, 'agg1')).toHaveAttribute('data-element-id', 'agg1');
            await expect(fgFindElement(grid, 'agg2')).toHaveAttribute('data-element-id', 'agg2');

            const aggContainer = fgFindDropContainer(grid, 'agg1');
            await expect(aggContainer.locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'group1', 'agg1')).toHaveAttribute('data-element-id', 'group1');

            const groupContainer = fgFindDropContainer(grid, 'group1');
            await expect(groupContainer.locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1', 'group1')).toHaveAttribute('data-element-id', 'input1');
        });

        await test.step('Should block moving the root input1 into agg1', async () => {
            const source = fgFindElement(grid, 'input1');
            await fgMoveElement(grid, source, { containerId: 'agg1' });

            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(3);
            await expect(fgFindDropContainer(grid, 'agg1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1')).toHaveCount(1);
        });

        await test.step('Should block moving the root input1 into group1', async () => {
            const source = fgFindElement(grid, 'input1');
            await fgMoveElement(grid, source, { containerId: 'group1' });

            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(3);
            await expect(fgFindDropContainer(grid, 'group1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1')).toHaveCount(1);
        });

        await test.step('Should block moving group1 to the root container', async () => {
            const source = fgFindElement(grid, 'group1', 'agg1');
            await fgMoveElement(grid, source);

            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(3);
            await expect(fgFindDropContainer(grid, 'agg1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'group1', 'agg1')).toHaveCount(1);
        });

        await test.step('Should block moving the nested input1 (agg1/group1) to the root container', async () => {
            const source = fgFindElement(grid, 'input1', 'group1');
            await fgMoveElement(grid, source);

            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(3);
            await expect(fgFindDropContainer(grid, 'group1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1', 'group1')).toHaveCount(1);
        });

        await test.step('Should allow moving the nested input1 out of group1, within agg1', async () => {
            const source = fgFindElement(grid, 'input1', 'group1');
            await fgMoveElement(grid, source, { containerId: 'agg1' });

            await expect(fgFindDropContainer(grid, 'group1').locator('> .form-item')).toHaveCount(0);
            await expect(fgFindDropContainer(grid, 'agg1').locator('> .form-item')).toHaveCount(2);
            await expect(fgFindElement(grid, 'input1', 'agg1')).toHaveCount(1);
        });

        await test.step('Should allow moving the input1 back into group1, within agg1', async () => {
            const source = fgFindElement(grid, 'input1', 'agg1');
            await fgMoveElement(grid, source, { containerId: 'group1' });

            await expect(fgFindDropContainer(grid, 'agg1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindDropContainer(grid, 'group1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1', 'group1')).toHaveCount(1);
        });

        await test.step('Should allow moving the root input1 to another page', async () => {
            const source = fgFindElement(grid, 'input1');
            await fgMoveElementToPage(grid, source, 1);

            // The editor should've switched to the target page, which now only contains input1
            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1')).toHaveCount(1);

            // The original/first page should no longer contain it, only agg1 and agg2 should be left
            await firstPageTabLabel.click();
            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(2);
            await expect(fgFindElement(grid, 'agg1')).toHaveCount(1);
            await expect(fgFindElement(grid, 'agg2')).toHaveCount(1);
        });

        await test.step('Should allow moving agg1 (with its nested elements) to another page', async () => {
            const source = fgFindElement(grid, 'agg1');
            await fgMoveElementToPage(grid, source, 1);

            // The editor should've switched to the target page, which now contains both elements
            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(2);
            await expect(fgFindElement(grid, 'input1')).toHaveCount(1);
            await expect(fgFindElement(grid, 'agg1')).toHaveCount(1);

            // The nested structure of agg1 should still be intact
            await expect(fgFindDropContainer(grid, 'agg1').locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'group1', 'agg1')).toHaveCount(1);
            await expect(fgFindElement(grid, 'input1', 'group1')).toHaveCount(1);

            // The original/first page should now only contain agg2
            await firstPageTabLabel.click();
            await expect(fgFindDropContainer(grid).locator('> .form-item')).toHaveCount(1);
            await expect(fgFindElement(grid, 'agg2')).toHaveCount(1);
        });
    });

    test('should display all aggregate elements and have them editable', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-20336',
        }],
    }, async ({ page }) => {
        const EDITING_FORM = IMPORTER.get(FORM_THREE);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.VIEW_FORM, value: true },
                    { type: GcmsPermission.UPDATE_FORM, value: true },
                ],
            },
        ]);

        await test.step('Open Editor', async () => {
            const list = findList(page, ITEM_TYPE_FORM);
            const item = findItem(list, EDITING_FORM.id);
            await itemAction(item, 'edit');
        });

        const grid = page.locator('content-frame gtx-form-grid');
        const editSidebar = fgFindEditSidebar(grid);
        const LABEL_TEXT = 'Hello World';
        const AGG_ELEMENT_IDS = [
            'firstName',
            'lastName',
            'address',
            'addressExt',
            'postCode',
            'place',
            'salutation',
            'additionalRenters',
        ];

        await test.step('Should display agg2 with all of its nested elements', async () => {
            const agg2 = fgFindElement(grid, 'agg2');
            await expect(agg2).toHaveAttribute('data-element-id', 'agg2');
            await expect(agg2).toBeVisible();

            // These elements have no `formGridOptions.type`, so they can only be displayed
            // if their schema is correctly resolved via the unique (aggregate-prefixed) id.
            const aggContainer = fgFindDropContainer(grid, 'agg2');
            await expect(aggContainer.locator('> .form-item')).toHaveCount(AGG_ELEMENT_IDS.length);
            for (const id of AGG_ELEMENT_IDS) {
                await expect(fgFindElement(grid, id, 'agg2')).toBeVisible();
            }
        });

        await test.step('Should edit the definition of a nested string element', async () => {
            const el = fgFindElement(grid, 'firstName', 'agg2');
            // Click the title, as narrow elements may otherwise have their action buttons in the center
            await el.locator('.element-container .element-title').click();
            await expect(el).toContainClass('is-selected');
            await expect(editSidebar).toBeVisible();

            // The definition tab is only available if the schema of the element could be found
            const definitionTab = await fgSelectElementTab(editSidebar, 'definition');
            await expect(definitionTab.locator('[data-control="mandatory"] input[type="checkbox"]')).toBeChecked();

            const updateOnChange = definitionTab.locator('[data-control="updateOnChange"]');
            await expect(updateOnChange.locator('input[type="checkbox"]')).not.toBeChecked();
            await updateOnChange.locator('label').click();
            await expect(updateOnChange.locator('input[type="checkbox"]')).toBeChecked();
        });

        await test.step('Should edit the translations of a nested string element', async () => {
            const el = fgFindElement(grid, 'firstName', 'agg2');
            const elLabel = el.locator('.element-container .element-title');
            const translationTab = await fgSelectElementTab(editSidebar, 'translations');

            await translationTab.locator('[data-control="label"] input').fill(LABEL_TEXT);
            await expect(elLabel).toHaveText(LABEL_TEXT);
        });

        await test.step('Should edit the definition of a nested number element', async () => {
            const el = fgFindElement(grid, 'postCode', 'agg2');
            await el.locator('.element-container .element-title').click();
            await expect(el).toContainClass('is-selected');
            await expect(fgFindElement(grid, 'firstName', 'agg2')).not.toContainClass('is-selected');

            const definitionTab = await fgSelectElementTab(editSidebar, 'definition');
            const mandatory = definitionTab.locator('[data-control="mandatory"]');
            await expect(mandatory.locator('input[type="checkbox"]')).toBeChecked();
            await mandatory.locator('label').click();
            await expect(mandatory.locator('input[type="checkbox"]')).not.toBeChecked();
        });

        await test.step('Save and Validate', async () => {
            const saveReq = page.waitForResponse(matchRequest('PUT', '/rest/form/*'));
            await editorAction(page, 'save');
            const res = await saveReq;
            const req: FormSaveRequest = res.request().postDataJSON();

            const rootProps = req.data.schema?.properties || {};
            const aggProps = rootProps.agg2?.properties || {};

            // Changes have to be applied to the properties within the aggregate
            expect(aggProps.firstName?.updateOnChange).toBe(true);
            expect(aggProps.firstName?.mandatory).toBe(true);
            expect(aggProps.postCode?.mandatory).toBe(false);
            expect(aggProps.postCode?.type).toBe('number');
            expect(Object.keys(aggProps).sort()).toEqual([...AGG_ELEMENT_IDS].sort());

            // And must not leak into the root level
            expect(rootProps.firstName).toBeUndefined();
            expect(rootProps.postCode).toBeUndefined();

            const aggEl = req.data['ui-schema']?.pages?.[0]?.elements?.find((el) => el.id === 'agg2');
            const firstNameEl = aggEl?.elements?.find((el) => el.id === 'firstName');
            expect(Object.values(firstNameEl?.label || {})).toContain(LABEL_TEXT);
        });
    });

    test.describe('External Forms', () => {
        test.beforeEach(async () => {
            const node = IMPORTER.get(NODE_MINIMAL);
            await IMPORTER.client.form.assignConfiguration('external', node.id).send();
            await IMPORTER.importData([EXT_FORM_ONE]);
        });

        test('controls may not edit label or descriptions', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20301',
            }],
        }, async ({ page }) => {
            const EDITING_FORM = IMPORTER.get(EXT_FORM_ONE);

            await setupWithPermissions(page, [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.VIEW_FORM, value: true },
                        { type: GcmsPermission.UPDATE_FORM, value: true },
                    ],
                },
            ]);

            await test.step('Open Editor', async () => {
                const list = findList(page, ITEM_TYPE_FORM);
                const item = findItem(list, EDITING_FORM.id);
                await itemAction(item, 'edit');
            });

            await test.step('Validate control translations', async () => {
                const grid = page.locator('content-frame gtx-form-grid');
                const el = fgFindElement(grid, 'input1');

                await expect(el).toBeVisible();
                await el.click();
                await expect(el).toContainClass('is-selected');

                const editSidebar = fgFindEditSidebar(grid);
                await expect(editSidebar).toBeVisible();

                const translationTab = await fgSelectElementTab(editSidebar, 'translations');

                await expect(translationTab.locator('[data-control="label"] input')).toBeDisabled();
                await expect(translationTab.locator('[data-control="description"] input')).toBeDisabled();
                // Other translations should still be editable
                await expect(translationTab.locator('[data-control="summary"] input')).toBeEnabled();
            });
        });

        test('blocks may edit label or descriptions', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-20301',
            }],
        }, async ({ page }) => {
            const EDITING_FORM = IMPORTER.get(EXT_FORM_ONE);
            const LABEL_TEXT = 'Hello World';
            const DESCRIPTION_TEXT = 'Foo Bar Content!';

            await setupWithPermissions(page, [
                {
                    type: AccessControlledType.NODE,
                    instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                    subObjects: true,
                    perms: [
                        { type: GcmsPermission.READ, value: true },
                        { type: GcmsPermission.VIEW_FORM, value: true },
                        { type: GcmsPermission.UPDATE_FORM, value: true },
                    ],
                },
            ]);

            await test.step('Open Editor', async () => {
                const list = findList(page, ITEM_TYPE_FORM);
                const item = findItem(list, EDITING_FORM.id);
                await itemAction(item, 'edit');
            });

            await test.step('Edit Form', async () => {
                const grid = page.locator('content-frame gtx-form-grid');
                const el = await fgAddBlock(grid, 'sectionheadline', {});

                await expect(el).toBeVisible();
                await el.click();
                await expect(el).toContainClass('is-selected');

                const editSidebar = fgFindEditSidebar(grid);
                await expect(editSidebar).toBeVisible();

                const elLabel = el.locator('.element-container .element-title');
                const translationTab = await fgSelectElementTab(editSidebar, 'translations');
                const labelCtrl = translationTab.locator('[data-control="label"] input');
                const descriptionCtrl = translationTab.locator('[data-control="description"] input');

                await expect(labelCtrl).toBeEnabled();
                await expect(descriptionCtrl).toBeEnabled();

                await labelCtrl.fill(LABEL_TEXT);
                await expect(elLabel).toHaveText(LABEL_TEXT);
                await descriptionCtrl.fill(DESCRIPTION_TEXT);
            });

            await test.step('Save and Validate', async () => {
                const saveReq = page.waitForResponse(matchRequest('PUT', '/rest/form/*'));
                await editorAction(page, 'save');
                const res = await saveReq;
                const req: FormSaveRequest = res.request().postDataJSON();

                const elements = req.data['ui-schema']?.pages?.[0]?.elements || [];
                const block = elements.find((el) => el.label?.[LANGUAGE_EN] === LABEL_TEXT);
                expect(block).toBeDefined();
                expect(block.description).toEqual({
                    [LANGUAGE_EN]: DESCRIPTION_TEXT,
                });
            });
        });
    });
});
