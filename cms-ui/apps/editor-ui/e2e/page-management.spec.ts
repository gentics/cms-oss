import {
    AccessControlledType,
    Page as CMSPage,
    Response as CMSResponse,
    GcmsPermission,
    NodePageLanguageCode,
    NodeUrlMode,
    ResponseCode,
} from '@gentics/cms-models';
import {
    clickModalAction,
    clickNotificationAction,
    CONTENT_REPOSITORY_MESH,
    EntityImporter,
    findContextContent,
    findNotification,
    GroupImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_PAGE_TRANSLATION,
    IMPORT_TYPE_USER,
    ImportPermissions,
    ITEM_TYPE_PAGE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    loginWithForm,
    matchesUrl,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    NodeImportData,
    onRequest,
    openContext,
    PAGE_ONE,
    PageTranslationImportData,
    pickSelectValue,
    SCHEDULE_PUBLISHER,
    TestSize,
    UserImportData,
    waitForResponseFrom
} from '@gentics/e2e-utils';
import { cloneWithSymbols } from '@gentics/ui-core/utils/clone-with-symbols';
import { expect, Locator, Page, Response, test } from '@playwright/test';
import {
    closeObjectPropertyEditor,
    editorAction,
    expectItemOffline,
    expectItemPublished,
    findItem,
    findList,
    itemAction,
    openObjectPropertyEditor,
    openTagList,
    pageListRowLanguage,
    selectNode,
    setListLanguage,
} from './helpers';

test.describe('Page Management', () => {

    const IMPORTER = new EntityImporter();
    const NAMESPACE = 'pagemngt';

    const TEST_GROUP_BASE: GroupImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
        [IMPORT_ID]: `group_${NAMESPACE}_test`,

        description: 'Page-Management: Test Group',
        name: `${NAMESPACE}_test`,
    };

    const TEST_USER: UserImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_USER,
        [IMPORT_ID]: `user_${NAMESPACE}_test`,

        email: 'page-management@example.com',
        firstName: 'Page Management',
        lastName: 'User',
        login: `${NAMESPACE}_user`,
        password: 'somethingsomething',

        group: TEST_GROUP_BASE,
    };

    const NODE_SINGLE_LANGUAGE: NodeImportData = {
        [IMPORT_TYPE]: IMPORT_TYPE_NODE,
        [IMPORT_ID]: 'singleLanguageNode',

        node: {
            name : 'Single Language',
            publishDir : '',
            binaryPublishDir : '',
            pubDirSegment : true,
            publishImageVariants : false,
            host : 'singlelanguage.localhost',
            publishFs : false,
            publishFsPages : false,
            publishFsFiles : false,
            publishContentMap : true,
            publishContentMapPages : true,
            publishContentMapFiles : true,
            publishContentMapFolders : true,
            urlRenderWayPages: NodeUrlMode.AUTOMATIC,
            urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
            omitPageExtension : false,
            pageLanguageCode : NodePageLanguageCode.FILENAME,
            meshPreviewUrlProperty : '',
        },
        description: 'single language test',

        languages : ['en'],
        templates: [
            '57a5.5db4acfa-3224-11ef-862c-0242ac110002',
        ],
    };

    const NEW_PAGE_NAME = 'Hello World';
    const CHANGE_PAGE_NAME = 'Foo bar change';
    const OBJECT_PROPERTY = 'test_color';
    const TEST_CATEGORY_ID = 2;
    const COLOR_ID = 2;

    // eslint-disable-next-line @typescript-eslint/naming-convention
    let TEST_PAGE: CMSPage;

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
            TEST_PAGE = IMPORTER.get(PAGE_ONE);
        });
    });

    test.afterEach(async () => {
        try {
            await IMPORTER.client.page.update(TEST_PAGE.id, { unlock: true, page: {} }).send();
        } catch (err) {}
    });

    async function setupWithPermissions(
        page: Page,
        permissions: ImportPermissions[],
        node: NodeImportData = NODE_MINIMAL,
    ): Promise<void> {
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
            await selectNode(page, IMPORTER.get(node)!.id);
        });
    }

    test('should be possible to create a new page', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ_TEMPLATES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        let createReq: Promise<Response>;

        await test.step('Create a new Page', async () => {
            await list.locator('.header-controls [data-action="create-new-item"] button').click();
            const modal = page.locator('create-page-modal');
            const form = modal.locator('gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(NEW_PAGE_NAME);
            createReq = page.waitForResponse(matchRequest('POST', '/rest/page/create'));
            await modal.locator('.modal-footer [data-action="confirm"] button').click();
        });

        const response = await createReq;
        const responseBody = await response.json();
        const pageId = responseBody.page.id;

        // The new page should now be displayed in the list
        await expect(findItem(list, pageId)).toBeVisible();
    });

    test('should have folder language pre-selected when creating a page', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19579',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ_TEMPLATES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const header = list.locator('.header-controls');

        async function testLanguage(lang: string): Promise<void> {
            await test.step(`Should pre-select language "${lang}" correctly`, async () => {
                await setListLanguage(list, lang);
                await header.locator('[data-action="create-new-item"]').click();
                const modal = page.locator('create-page-modal');
                await expect(modal.locator('gtx-page-properties [formControlName="language"] .view-value')).toHaveAttribute('data-value', lang);
                await clickModalAction(modal, 'cancel');
            });
        }

        await testLanguage(LANGUAGE_DE);
        await testLanguage(LANGUAGE_EN);
    });

    test('should be possible to edit the page properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await expect(item.locator('.item-name .item-name-only')).toHaveText(TEST_PAGE.name);

        await test.step('Update the page name', async () => {
            await itemAction(item, 'properties');
            const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(CHANGE_PAGE_NAME);
            // eslint-disable-next-line playwright/no-wait-for-timeout
            await page.waitForTimeout(500); // Have to wait for internals to propagate

            await editorAction(page, 'save');
        });

        await expect(item.locator('.item-name .item-name-only')).toHaveText(CHANGE_PAGE_NAME);
    });

    test('should not be possible to edit the page properties without permissions', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-19638',
            }],
        }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL)!.folderId}`,
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            }
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await itemAction(item, 'properties');

        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
        await expect(form.locator('[formcontrolname="name"] input')).toBeDisabled();
        await expect(form.locator('[formcontrolname="description"] input')).toBeDisabled();
    });

    test('should be possible to publish the page after saving properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18802',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);
        let form: Locator;

        // expect the page to be offline
        await expectItemOffline(item);

        await test.step('Update the page', async () => {
            await itemAction(item, 'properties');
            form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(CHANGE_PAGE_NAME);
            await editorAction(page, 'save');
        });

        await test.step('Publish the page with toast action', async () => {
            const publishNotification = findNotification(page, `page-save-success-with-publish:${TEST_PAGE.id}`);
            const publishReq = page.waitForResponse(matchRequest('POST', `/rest/page/publish/${TEST_PAGE.id}`));
            await clickNotificationAction(publishNotification);
            await publishReq;
            await expect(form).toBeHidden();
        });

        // page should be published now
        await expectItemPublished(item);
    });

    test('should be possible to edit the page object-properties', async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
            {
                type: AccessControlledType.OBJECT_PROPERTY_TYPE,
                instanceId: '10007',
                subObjects: true,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.UPDATE, value: true },
                ],
            },
        ]);

        let list = findList(page, ITEM_TYPE_PAGE);
        let item = findItem(list, TEST_PAGE.id);

        await test.step('Update Page Object-Properties', async () => {
            await itemAction(item, 'properties');
            await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
            await page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger').click();
            await page.locator(`gtx-dropdown-content li.select-option[data-id="${COLOR_ID}"]`).click();
            await editorAction(page, 'save');
        });

        // Reopen the editor to reload fresh values
        await test.step('Validate the changes are correct after reloading', async () => {
            await closeObjectPropertyEditor(page);
            list = findList(page, ITEM_TYPE_PAGE);
            item = findItem(list, TEST_PAGE.id);
            await itemAction(item, 'properties');
            await openObjectPropertyEditor(page, TEST_CATEGORY_ID, OBJECT_PROPERTY);
            await expect(page.locator('gentics-tag-editor select-tag-property-editor gtx-select gtx-dropdown-trigger .view-value')).toHaveAttribute('data-value', `${COLOR_ID}`);
        });
    });

    test('should be possible to open the context-menu in the page-properties', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18791',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await itemAction(item, 'properties');
        await editorAction(page, 'editor-context');
        const content = findContextContent(page, 'item-editor');

        await expect(content).toBeAttached();
    });

    test('should handle errors when taking a page offline correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19444',
        }],
    }, async ({ page }) => {
        // First we need to publish the page
        await IMPORTER.client.page.publish(TEST_PAGE.id, {
            alllang: true,
        }).send();
        await IMPORTER.importData([SCHEDULE_PUBLISHER]);
        await IMPORTER.executeSchedule(SCHEDULE_PUBLISHER);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        // Delete the page, to force an error once we try to take the page offline
        await IMPORTER.client.page.delete(TEST_PAGE.id).send();

        const offlineReq = waitForResponseFrom(page, 'POST', `/rest/page/takeOffline/${TEST_PAGE.id}`, {
            skipStatus: true,
        });
        await itemAction(item, 'take-offline');
        const offlineRes = await offlineReq;
        const offlineBody = await offlineRes.json() as CMSResponse;

        const toasts = page.locator('gtx-toast');

        // eslint-disable-next-line playwright/no-wait-for-timeout
        await page.waitForTimeout(500); // Allow for notifications to spawn
        expect(await toasts.all()).toHaveLength(1);
        await expect(toasts.locator('.message')).toContainText(offlineBody.messages[0].message);
    });

    test('should display error notification and log correctly on invalid requests', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19452',
        }],
    }, async ({ page }) => {
        const NICE_URL = '/test';

        // Create another page which has the nice-url set
        const DUMMY_PAGE = cloneWithSymbols(PAGE_ONE);
        DUMMY_PAGE[IMPORT_ID] = 'page-management_dummypage';
        DUMMY_PAGE.pageName = 'Dummy Page';
        DUMMY_PAGE.niceUrl = NICE_URL;
        await IMPORTER.importData([DUMMY_PAGE]);

        // Basic setup
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        // Edit the nice-url to use the one already in use
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await itemAction(item, 'properties');
        const form = page.locator('content-frame combined-properties-editor .properties-content gtx-page-properties');
        await form.locator('[formcontrolname="niceUrl"] input').fill(NICE_URL);

        const errorMessages: string[] = [];
        page.on('console', (msg) => {
            if (msg.type() === 'error') {
                errorMessages.push(msg.text());
            }
        });

        const saveReq = waitForResponseFrom(page, 'POST', '/rest/page/save/*', {
            skipStatus: true, // Since we expect it to fail
        });
        await editorAction(page, 'save');
        const saveRes = await saveReq;
        const resBody = await saveRes.json() as CMSResponse;
        const resMessage = resBody.messages[0].message;

        // Delay for multiple toasts to appear
        // eslint-disable-next-line playwright/no-wait-for-timeout
        await page.waitForTimeout(500);

        const toasts = page.locator('gtx-toast');
        expect(await toasts.all()).toHaveLength(1);
        await expect(toasts.locator('.message'))
            .toContainText(resMessage.replace('<br/>', '\n'));
        // expect(errorMessages).toHaveLength(1); // Disabled until further notice
    });

    async function setupInstantPublishing(): Promise<void> {
        await test.step('Setup Instant Publishing', async () => {
            // Activate instant publishing
            const CR = IMPORTER.get(CONTENT_REPOSITORY_MESH);
            await IMPORTER.client.contentRepository.update(CR.id, {
                instantPublishing: true,
            }).send();
            // Make sure that mesh is properly setup
            await IMPORTER.client.contentRepository.repairStructure(CR.id).send();
        });
    }

    async function testPageTakeOffline(page: Page, successful: boolean): Promise<void> {
        await test.step('Page Setup', async () => {
            // First we need to publish the page
            await IMPORTER.client.page.publish(TEST_PAGE.id, {
                alllang: true,
            }).send();
            await IMPORTER.importData([SCHEDULE_PUBLISHER]);
            await IMPORTER.executeSchedule(SCHEDULE_PUBLISHER);
        });

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);
        let toastMessage: string;

        // Validate initial state
        await expectItemPublished(item);

        await test.step('Take page offline', async () => {
            // eslint-disable-next-line playwright/no-conditional-in-test
            if (!successful) {
                // Mock an error
                await page.route((url) => matchesUrl(url, `/rest/page/takeOffline/${TEST_PAGE.id}`), (route) => {
                    return route.fulfill({
                        status: 200, // Old endpoint which still returns 200 on failure
                        json: {
                            messages: [{
                                type: 'CRITICAL',
                                message: 'An error occurred while accessing the backend system.',
                                timestamp: new Date().getTime(),
                            }],
                            responseInfo: {
                                responseCode: ResponseCode.FAILURE,
                                responseMessage: `Error while taking offline page ${TEST_PAGE.id}: Mocked error`,
                            },
                        } as CMSResponse,
                    });
                });
            }

            const offlineReq = waitForResponseFrom(page, 'POST', `/rest/page/takeOffline/${TEST_PAGE.id}`);
            await itemAction(item, 'take-offline');
            const offlineRes = await offlineReq;
            const offlineBody = await offlineRes.json() as CMSResponse;
            // eslint-disable-next-line playwright/no-conditional-in-test
            toastMessage = successful
                ? offlineBody.messages[0].message
                : offlineBody.responseInfo.responseMessage;
        });

        await test.step('Validate UI state', async () => {
            const toasts = page.locator('gtx-toast');
            await page.waitForTimeout(500); // Allow for notifications to spawn

            // Get all notifications now, and check length.
            // Using the build in helper is wrong in this case, as when 2 are displayed,
            // it'd wait until one fades away and resolves to true.
            expect(await toasts.all()).toHaveLength(1);

            await expect(toasts.locator('.message')).toContainText(toastMessage);

            // eslint-disable-next-line playwright/no-conditional-in-test
            if (successful) {
                await expect(toasts.locator('.gtx-toast')).toContainClass('success');
                // Item should be updated correctly in the list as well
                await expectItemOffline(item);
            } else {
                await expect(toasts.locator('.gtx-toast')).toContainClass('alert');
                // Since an error occurred, page should still be published
                await expectItemPublished(item);
            }
        });
    }

    test('should be able to take a page offline', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19444',
        }],
    }, async ({ page }) => {
        await testPageTakeOffline(page, true);
    });

    test('should be able to take a page offline with instant publishing', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19444',
        }],
    }, async ({ page }) => {
        await setupInstantPublishing();
        await testPageTakeOffline(page, true);
    });

    test('should handle errors when taking a page offline', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19444',
        }],
    }, async ({ page }) => {
        await testPageTakeOffline(page, false);
    });

    test('should handle errors when taking a page offline with instant publishing', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19444',
        }],
    }, async ({ page }) => {
        await setupInstantPublishing();
        await testPageTakeOffline(page, false);
    });

    test('should be able to switch properties tabs without saving changes modal', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19560',
        }],
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);
        await itemAction(item, 'properties');
        await openTagList(page);

        await page.waitForTimeout(500);
        await expect(page.locator('confirm-navigation-modal')).not.toBeAttached();
    });

    test('should suggest names for new pages correctly', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19560',
        }]
    }, async ({ page }) => {
        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ_TEMPLATES, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        await list.locator('.header-controls [data-action="create-new-item"] button').click();

        const modal = page.locator('create-page-modal');
        const form = modal.locator('gtx-page-properties');
        const fileName = form.locator('[formcontrolname="fileName"] input');
        const pageName = form.locator('[formcontrolname="name"] input');
        const lang = form.locator('[formcontrolname="language"]');

        let didRequest = false;
        let checkForRequests = false;
        onRequest(page, matchRequest('POST', '/rest/page/suggest/filename'), () => {
            if (checkForRequests) {
                didRequest = true;
            }
        });

        // Set the language first so we are sure about the requests being sent
        await pickSelectValue(lang, LANGUAGE_DE);

        // Simple name first
        await test.step('Simple name', async () => {
            const suggestReq = waitForResponseFrom(page, 'POST', '/rest/page/suggest/filename');
            await pageName.fill(NEW_PAGE_NAME);
            await suggestReq;
            await expect(fileName).toHaveValue('Hello-World.de.html');
        });

        // Update the name, file-name should update accordingly
        await test.step('Extend the name', async () => {
            const suggestReq = waitForResponseFrom(page, 'POST', '/rest/page/suggest/filename');
            await pageName.press('1');
            await suggestReq;
            await expect(fileName).toHaveValue('Hello-World1.de.html');
        });

        // Change the language, which should change the file-name as well
        await test.step('Language change', async () => {
            const suggestReq = waitForResponseFrom(page, 'POST', '/rest/page/suggest/filename');
            await pickSelectValue(lang, LANGUAGE_EN);
            await suggestReq;
            await expect(fileName).toHaveValue('Hello-World1.en.html');
        });

        // Update the file-name manually, to disable automatic suggestions
        await fileName.fill('example');

        // Changes to the page-name and language shouldn't trigger any suggestions now
        await test.step('No update after manual change', async () => {
            didRequest = false;
            checkForRequests = true;

            await pageName.fill('Something new');
            await pickSelectValue(lang, LANGUAGE_DE);

            await page.waitForTimeout(500);
            expect(didRequest).toBe(false);
            checkForRequests = false;
        });

        // Manual suggestion should set it
        await test.step('Trigger manual syggestion', async () => {
            const suggestReq = waitForResponseFrom(page, 'POST', '/rest/page/suggest/filename');
            await form.locator('[data-action="suggest-filename"]').click();
            await suggestReq;
            await expect(fileName).toHaveValue('Something-new.de.html');
        });

        await test.step('Changing page-name should not update', async () => {
            didRequest = false;
            checkForRequests = true;

            await pageName.fill(NEW_PAGE_NAME);
            await page.waitForTimeout(500);
            expect(didRequest).toBe(false);
            await expect(fileName).toHaveValue('Something-new.de.html');
            checkForRequests = false;
        });

        await test.step('Clearing file-name creates suggestion', async () => {
            const suggestReq = waitForResponseFrom(page, 'POST', '/rest/page/suggest/filename');
            await fileName.fill('');
            await fileName.blur();
            await suggestReq;
            await expect(fileName).toHaveValue('Hello-World.de.html');
        });
    });

    test('should display correct publish status for page in a node with one language', async ({ page }) => {
        const nodeData = cloneWithSymbols(NODE_SINGLE_LANGUAGE);
        const cr = IMPORTER.get(CONTENT_REPOSITORY_MESH);
        if (cr) {
            nodeData.node.contentRepositoryId = cr.id;
        }

        await IMPORTER.importData([nodeData]);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(nodeData).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                    { type: GcmsPermission.CREATE_ITEMS, value: true },
                    { type: GcmsPermission.READ_TEMPLATES, value: true },
                    { type: GcmsPermission.PUBLISH_PAGES, value: true },
                ],
            },
        ], nodeData);

        await setupInstantPublishing();

        const list = findList(page, ITEM_TYPE_PAGE);
        let createReq: Promise<Response>;
        let listOptions = list.locator('[data-action="open-list-context"]');

        await test.step('Change Status Icon Settings', async () => {
            const dropdown = await openContext(listOptions);
            await dropdown.locator('gtx-dropdown-item[data-action="toggle-status-icons"]').click();
        });

        await test.step('Create a new Page', async () => {
            await list.locator('.header-controls [data-action="create-new-item"] button').click();
            const modal = page.locator('create-page-modal');
            const form = modal.locator('gtx-page-properties');
            await form.locator('[formcontrolname="name"] input').fill(NEW_PAGE_NAME);
            createReq = page.waitForResponse(matchRequest('POST', '/rest/page/create'));
            await clickModalAction(modal, 'confirm');
        });

        const response = await createReq;
        const responseBody = await response.json();
        const pageId = responseBody.page.id;
        const pageItem = findItem(list, pageId);

        await expect(pageItem).toBeVisible();
        await expectItemOffline(pageItem);

        // Close the edit-mode, as creating a new page will always open it
        await editorAction(page, 'close');

        await test.step('Publish Page from context menu', async () => {
            const publishReq = page.waitForResponse(matchRequest('POST', `/rest/page/publish/${pageId}`));
            await itemAction(pageItem, 'publish');
            await publishReq;
        });

        await expectItemPublished(pageItem);
    });

    test('should be able to preview a page language', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19770',
        }],
    }, async ({ page }) => {
        const TRANSLATION_LANG = LANGUAGE_DE;
        const TEST_TRANSLATION: PageTranslationImportData = {
            [IMPORT_TYPE]: IMPORT_TYPE_PAGE_TRANSLATION,
            [IMPORT_ID]: `${NAMESPACE}_${TEST_PAGE[IMPORT_ID]}_${TRANSLATION_LANG}`,

            language: TRANSLATION_LANG,
            pageId: PAGE_ONE[IMPORT_ID],
            pageName: 'Translation Test 123',
        };

        await IMPORTER.importData([
            TEST_TRANSLATION,
        ]);

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                    { type: GcmsPermission.UPDATE_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        // We have to make sure the language is in english as "default", otherwise we won't find it
        await setListLanguage(list, LANGUAGE_EN);

        const item = findItem(list, TEST_PAGE.id);
        const langIndicator = pageListRowLanguage(item, TRANSLATION_LANG);
        const contentFrame = page.locator('content-frame');

        await expect(langIndicator).toBeVisible();
        const langContext = await openContext(langIndicator);
        const previewAction = langContext.locator('[data-action="preview"]');

        // Should load the preview-mode of the translated page
        const alohaLoadReq = waitForResponseFrom(page, 'GET', '/alohapage', {
            params: {
                real: 'newview',
                realid: `${IMPORTER.get(TEST_TRANSLATION).id}`,
                nodeid: `${IMPORTER.get(NODE_MINIMAL).id}`,
            },
        })
        await expect(contentFrame).not.toBeAttached();
        await expect(previewAction).toBeVisible();
        await previewAction.click();

        await expect(contentFrame).toBeVisible();
        await alohaLoadReq;
    });

    test('should display a recent relative time for a locked page, not a time decades in the past', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19877',
        }],
    }, async ({ page }) => {
        // Unix timestamp in seconds as returned by the REST API (Java int) — 30 seconds ago
        const lockedSinceSeconds = Math.round(Date.now() / 1000) - 30;

        // Register the route BEFORE navigation so it intercepts the initial page list load
        // triggered by selectNode inside setupWithPermissions.
        // This simulates the exact format the REST API returns (Java int, Unix seconds).
        await page.route(url => matchesUrl(url, '/rest/folder/getPages/*'), async (route) => {
            const response = await route.fetch();
            const body = await response.json();
            const lockedPage = body.pages?.find((p: any) => p.id === TEST_PAGE.id);
            if (lockedPage) {
                lockedPage.locked = true;
                lockedPage.lockedSince = lockedSinceSeconds;
                lockedPage.lockedBy = { id: 1, firstName: 'Admin', lastName: 'User' };
            }
            await route.fulfill({ json: body });
        });

        await setupWithPermissions(page, [
            {
                type: AccessControlledType.NODE,
                instanceId: `${IMPORTER.get(NODE_MINIMAL).folderId}`,
                perms: [
                    { type: GcmsPermission.READ, value: true },
                    { type: GcmsPermission.READ_ITEMS, value: true },
                ],
            },
        ]);

        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, TEST_PAGE.id);

        await test.step('Open the lock state contextmenu', async () => {
            await item.locator('item-status-label .status-label').click();
        });

        await test.step('Verify lock time is not displayed as decades in the past', async () => {
            const contextMenu = page.locator('item-state-contextmenu');
            await expect(contextMenu).toBeVisible();
            await expect(contextMenu).not.toContainText('years ago');
            await expect(contextMenu).not.toContainText('Jahren');
        });
    });
});
