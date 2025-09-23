import { Page as CmsPage, PageSaveRequest, Template } from '@gentics/cms-models';
import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import {
    BASIC_TEMPLATE_ID,
    EntityImporter,
    hasMatchingParams,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchesPath,
    matchRequest,
    minimalNode,
    navigateToApp,
    pageOne,
    pickSelectValue,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Frame, Locator, Page, test } from '@playwright/test';
import {
    ACTION_FORMAT_ABBR,
    ACTION_FORMAT_BOLD,
    ACTION_FORMAT_CITE,
    ACTION_FORMAT_CODE,
    ACTION_FORMAT_ITALIC,
    ACTION_FORMAT_QUOTE,
    ACTION_FORMAT_STRIKETHROUGH,
    ACTION_FORMAT_SUBSCRIPT,
    ACTION_FORMAT_SUPERSCRIPT,
    ACTION_FORMAT_UNDERLINE,
    ACTION_REMOVE_FORMAT,
    AUTH,
    HelperWindow,
} from './common';
import {
    createInternalLink,
    editorAction,
    findAlohaComponent,
    findItem,
    findList,
    getAlohaIFrame,
    itemAction,
    selectEditorTab,
    selectNode,
    selectRangeIn,
    selectTextIn,
    setupHelperWindowFunctions,
    openPageForEditing,
    createExternalLink,
} from './helpers';

test.describe.configure({ mode: 'serial' });
test.describe('Page Editing', () => {
    // Mark this suite as slow - Because it is
    test.slow();

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
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.syncTag(BASIC_TEMPLATE_ID, 'content');
        });

        await setupHelperWindowFunctions(page);
        await navigateToApp(page);
        await loginWithForm(page, AUTH.admin);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test.describe('Edit Mode', () => {

        let editingPage: CmsPage;
        let itemRow: Locator;
        let iframe: Frame;
        let mainEditable: Locator;

        async function openEditingPageInEditmode(page: Page) {
            const { row, iframe: pageIFrame, editable } = await openPageForEditing(page, editingPage);
            itemRow = row;
            iframe = pageIFrame;
            mainEditable = editable;
        }

        test.beforeEach(async ({ page }) => {
            editingPage = IMPORTER.get(pageOne);
            await openEditingPageInEditmode(page);
        });

        test('should be able to add new text to the content-editable', async ({ page }) => {
            const TEXT_CONTENT = 'Foo bar hello world';

            // Type content into editor
            await mainEditable.click();
            await mainEditable.clear();
            await mainEditable.fill(TEXT_CONTENT);

            // Save and verify request
            const saveRequest = page.waitForResponse(matchRequest('POST', `/rest/page/save/${editingPage.id}`));

            // TODO: Investigate why we need this timeout/why save isn't executed immediately.
            // Possible reason being angular event bindings.
            await page.waitForTimeout(2000);

            await editorAction(page, 'save');

            const response = await saveRequest;
            const data = await response.request().postDataJSON();

            // Verify content was saved correctly
            const contentText = data?.page?.tags?.content?.properties?.text?.stringValue || '';
            const trimmedContent = contentText.replace(/<\/?p>/g, '').trim();
            expect(trimmedContent).toBe(TEXT_CONTENT);
        });

        test('should cancel editing when the edit view is closed', async ({ page }) => {
            const cancelRequest = page.waitForResponse(matchRequest('POST', `/rest/page/cancel/${editingPage.id}`));

            await editorAction(page, 'close');

            await cancelRequest;
        });

        test('should be able to select an internal page as link', async ({ page }) => {
            const TEXT_CONTENT = 'Hello ';
            const LINK_TEXT = 'World';
            const LINK_ITEM = IMPORTER.get(pageOne);
            const ITEM_NODE = IMPORTER.get(minimalNode)!;
            const LINK_TITLE = 'My Link Title';
            const LINK_TARGET = '_blank';
            const LINK_ANCHOR = 'test-anchor';
            const LINK_LANGUAGE = 'en';

            // Type content and select text for link
            await mainEditable.click();
            await mainEditable.clear();
            await mainEditable.fill(TEXT_CONTENT + LINK_TEXT);

            // Select text to make into link
            expect(await selectRangeIn(mainEditable, TEXT_CONTENT.length, TEXT_CONTENT.length + LINK_TEXT.length)).toBe(true);
            await createInternalLink(page, async repoBrowser => {
                await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${LINK_ITEM.id}"] .item-checkbox label`).click();
            }, async form => {
                await form.locator('[data-slot="url"] .anchor-input input').fill(LINK_ANCHOR);
                await form.locator('[data-slot="title"] input').fill(LINK_TITLE);
                await pickSelectValue(form.locator('[data-slot="target"]'), LINK_TARGET);
                await form.locator('[data-slot="lang"] input').fill(LINK_LANGUAGE);
            });

            // Verify link was created
            const linkElement = mainEditable.locator('a');
            await expect(linkElement).toHaveAttribute('href', `/alohapage?real=newview&realid=${LINK_ITEM.id}&nodeid=${ITEM_NODE.id}`);
            await expect(linkElement).toHaveAttribute('hreflang', LINK_LANGUAGE);
            await expect(linkElement).toHaveAttribute('target', LINK_TARGET);
            await expect(linkElement).toHaveAttribute('title', LINK_TITLE);
            await expect(linkElement).toHaveAttribute('data-gentics-aloha-repository', 'com.gentics.aloha.GCN.Page');
            await expect(linkElement).toHaveAttribute('data-gcn-target-label', LINK_ITEM.name);
            await expect(linkElement).toHaveAttribute('data-gentics-aloha-object-id', `10007.${LINK_ITEM.id}`);
            await expect(linkElement).toHaveAttribute('data-gcn-channelid', `${ITEM_NODE.id}`);
            await expect(linkElement).toHaveText(LINK_TEXT);
        });

        test('should be able to edit inline-editables with simple formatting', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18800',
            }],
        }, async ({ page }) => {
            let inlineEditable = iframe.locator('main p[contenteditable="true"]');

            // Enter some sample text in multiple lines
            const LINES = ['test content', 'foo bar', 'final text'];
            await inlineEditable.click();
            await inlineEditable.fill(LINES[0]);
            await inlineEditable.press('Enter');
            await inlineEditable.type(LINES[1]);
            await inlineEditable.press('Enter');
            await inlineEditable.type(LINES[2]);

            // Select some text
            await selectTextIn(inlineEditable, LINES[0]);

            // Certain formatting should not be available
            await selectEditorTab(page, 'formatting');
            await expect(findAlohaComponent(page, { slot: 'typography' })).not.toBeVisible();
            await expect(findAlohaComponent(page, { slot: 'listOrdered' })).not.toBeVisible();
            await expect(findAlohaComponent(page, { slot: 'listUnordered' })).not.toBeVisible();
            await expect(findAlohaComponent(page, { slot: 'listDefinition' })).not.toBeVisible();

            // Format selected text as bold
            await findAlohaComponent(page, { slot: 'bold' }).click();

            // Save the page and re-open the page in edit-mode
            const saveReq = page.waitForResponse(matchRequest('POST', `/rest/page/save/${editingPage.id}`));
            await editorAction(page, 'save');
            await saveReq;
            await editorAction(page, 'close');

            // Wait until content-frame is actually closed before trying to go to edit-mode
            await page.locator('content-frame').waitFor({ state: 'detached' });

            await itemAction(itemRow, 'edit');
            iframe = await getAlohaIFrame(page);
            inlineEditable = iframe.locator('main p[contenteditable="true"]');
            await inlineEditable.waitFor({ timeout: 60_000 });

            expect(await inlineEditable.innerHTML()).toEqual([`<b>${LINES[0]}</b>`, ...LINES.slice(1)].join('<br>'));
        });

        async function createLinkCopyPasteTest(page: Page, handler: () => Promise<void>): Promise<void> {
            const TEXT_CONTENT = 'Example Link';
            const TEMPLATE = IMPORTER.get(BASIC_TEMPLATE_ID) as Template;
            const TEMPLATE_TAGS = Object.keys(TEMPLATE.templateTags);

            await test.step('Create internal link', async () => {
                // Type content
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT_CONTENT);

                // Select text to make into link
                expect(await selectTextIn(mainEditable, TEXT_CONTENT)).toBe(true);
                await handler();
            });

            // We need to save the page first and re-open it,
            // as otherwise the links are not created as actual tags yet.
            await test.step('Save and re-open the page', async () => {
                // Wait till properly saved
                const saveReq = page.waitForResponse(matchRequest('POST', `/rest/page/save/${editingPage.id}`));
                await editorAction(page, 'save');
                const req = await saveReq;
                const pageUpdate = await req.request().postDataJSON() as PageSaveRequest;

                const tags = new Set<string>(Object.keys(pageUpdate.page.tags || {}));
                // Remove template tags
                TEMPLATE_TAGS.forEach(tagName => tags.delete(tagName));

                // Make sure we actually save only one link tag
                expect(tags.size).toEqual(1);

                // Wait till properly closed
                await editorAction(page, 'close');
                await page.locator('content-frame').waitFor({ state: 'detached' });

                // Now open the page again
                await openEditingPageInEditmode(page);
            });

            await test.step('Copy and paste the link', async () => {
                // Select the text again
                expect(await selectTextIn(mainEditable, TEXT_CONTENT)).toBe(true);

                await mainEditable.press('ControlOrMeta+c');
                await mainEditable.press('ArrowRight');
                await mainEditable.press('Enter');
                await mainEditable.press('ControlOrMeta+v');

                // Wait for the content to be loaded
                await mainEditable.locator('img').waitFor({ state: 'detached' });

                const linkIds = await mainEditable.evaluate(el => {
                    return Array.from(el.querySelectorAll('a'))
                        .map(link => link.getAttribute('data-gcn-tagid'))
                        .filter(id => id != null);
                });

                expect(linkIds).toHaveLength(2);
                expect(linkIds[0]).not.toEqual(linkIds[1]);

                const saveReq = page.waitForRequest(matchRequest('POST', `/rest/page/save/${editingPage.id}`));
                await editorAction(page, 'save');
                const req = await saveReq;
                const pageUpdate = await req.postDataJSON() as PageSaveRequest;

                const tags = new Set<string>(Object.keys(pageUpdate.page.tags || {}));
                // Remove template tags
                TEMPLATE_TAGS.forEach(tagName => tags.delete(tagName));

                // Make sure we actually save 2 different tags
                expect(tags.size).toEqual(2);
            });
        }

        test('should be possible to copy an internal link', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18537',
            }],
        }, async ({ page }) => {
            const LINK_ITEM = IMPORTER.get(pageOne);
            await createLinkCopyPasteTest(page, async () => {
                await createInternalLink(page, async repoBrowser => {
                    await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${LINK_ITEM.id}"] .item-checkbox label`).click();
                }, async () => {
                    // nop
                });
            })
        });

        test('should be possible to copy an external link', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18537',
            }],
        }, async ({ page }) => {
            await createLinkCopyPasteTest(page, async () => {
                await createExternalLink(page, async form => {
                    await form.locator('[data-slot="url"] .target-input input').fill('https://example.com');
                    await form.locator('[data-slot="title"] input').fill('A very interesting site');
                });
            });
        });

        test.describe('Formatting', () => {
            test.describe('add and remove basic formats', () => {
                const TEXT = 'Hello World';
                const FORMAT_ACTIONS = [
                    { action: ACTION_FORMAT_BOLD, tag: 'b' },
                    { action: ACTION_FORMAT_ITALIC, tag: 'i' },
                    { action: ACTION_FORMAT_UNDERLINE, tag: 'u' },
                    { action: ACTION_FORMAT_STRIKETHROUGH, tag: 's' },
                    { action: ACTION_FORMAT_SUBSCRIPT, tag: 'sub' },
                    { action: ACTION_FORMAT_SUPERSCRIPT, tag: 'sup' },
                    { action: ACTION_FORMAT_CODE, tag: 'code' },
                ];

                for (const format of FORMAT_ACTIONS) {
                    test(`should add and remove ${format.action} formatting`, async ({ page }) => {
                        // Type and select text
                        await mainEditable.click();
                        await mainEditable.clear();
                        await mainEditable.fill(TEXT);
                        await page.keyboard.down('ControlOrMeta');
                        await page.keyboard.press('a');
                        await page.keyboard.up('ControlOrMeta');

                        await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();

                        // Apply format
                        let formatButton = findAlohaComponent(page, { slot: format.action, type: 'toggle-button' });
                        await formatButton.click();

                        // Verify format is applied
                        const formattedText = await mainEditable.locator(format.tag).textContent();
                        expect(formattedText).toBe(TEXT);
                        await mainEditable.click();

                        // Remove format
                        await page.keyboard.down('ControlOrMeta');
                        await page.keyboard.press('a');
                        await page.keyboard.up('ControlOrMeta');
                        // Activate the toolbar
                        await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();

                        formatButton = findAlohaComponent(page, { slot: ACTION_REMOVE_FORMAT, type: 'button' });
                        await formatButton.click();

                        // Verify format is removed
                        const hasFormat = await mainEditable.locator(format.tag).count();
                        expect(hasFormat).toBe(0);
                    });
                }
            });

            test('should add and remove quote formatting', async ({ page }) => {
                const TEXT = 'Hello World';

                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);
                await page.keyboard.down('ControlOrMeta');
                await page.keyboard.press('a');
                await page.keyboard.up('ControlOrMeta');

                await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();
                let formatButton = findAlohaComponent(page, { slot: ACTION_FORMAT_QUOTE, type: 'toggle-button' });
                await formatButton.click();

                const quote = mainEditable.locator('q');
                await expect(quote).toContainText(TEXT);

                await page.keyboard.down('ControlOrMeta');
                await page.keyboard.press('a');
                await page.keyboard.up('ControlOrMeta');

                await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();
                formatButton = findAlohaComponent(page, { slot: ACTION_REMOVE_FORMAT, type: 'button' });
                await formatButton.click();

                const hasQuote = await mainEditable.locator('q').count();
                expect(hasQuote).toBe(0);
            });

            test.skip('should add abbreviation with title', async ({ page }) => {
                const TEXT = 'HTML';
                const TITLE = 'HyperText Markup Language';

                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);
                await page.keyboard.down('ControlOrMeta');
                await page.keyboard.press('a');
                await page.keyboard.up('ControlOrMeta');

                await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();
                const formatButton = findAlohaComponent(page, { slot: ACTION_FORMAT_ABBR, type: 'toggle-button' });
                await formatButton.click();

                // Fill abbreviation form
                const modal = page.locator('gtx-dynamic-modal');
                await modal.locator('input[formcontrolname="title"]').fill(TITLE);
                await modal.locator('.modal-footer [data-action="confirm"]').click();

                // Verify abbreviation
                const abbr = mainEditable.locator('abbr');
                await expect(abbr).toHaveAttribute('title', TITLE);
                await expect(abbr).toContainText(TEXT);
            });

            test('should add citation with source', async ({ page }) => {
                const TEXT = 'The quote';
                const SOURCE = 'Famous Author';

                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);
                await page.keyboard.down('ControlOrMeta');
                await page.keyboard.press('a');
                await page.keyboard.up('ControlOrMeta');

                await page.locator('gtx-page-editor-tabs button[data-id="formatting"]').click();
                const formatButton = findAlohaComponent(page, { slot: ACTION_FORMAT_CITE, type: 'toggle-button' });
                await formatButton.click();

                // Verify citation
                const cite = mainEditable.locator('cite');
                // await expect(cite).toHaveAttribute('title', SOURCE);
                await expect(cite).toContainText(TEXT);
            });
        });
    });

    test('should load constructs correctly when switching to edit mode', {
        annotation: {
            type: 'issue',
            description: 'SUP-17542',
        },
    }, async ({ page }) => {
        // Admin request which shouldn't be used/called
        let adminEndpointCalled = false;
        page.on('request', request => {
            if (request.method() === 'POST' && matchesPath(request.url(), '/rest/construct')) {
                adminEndpointCalled = true;
            }
        });

        // Regular endpoint which should be used
        const constructLoadRequest = page.waitForRequest(request =>
            request.method() === 'GET'
                && matchesPath(request.url(), '/rest/construct')
                && hasMatchingParams(request.url(), {
                    nodeId: IMPORTER.get(minimalNode).id.toString(),
                    pageId: IMPORTER.get(pageOne).id.toString(),
                }),
        );

        // Setup page for editing
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, IMPORTER.get(pageOne).id);
        await itemAction(item, 'edit');

        // Switch to preview mode first
        // Wait for editor to be ready
        const iframe = page.locator('content-frame iframe.master-frame[loaded="true"]');
        await iframe.waitFor({ timeout: 60_000 });
        const editor = iframe.contentFrame().locator('main .container [contenteditable="true"]');
        await editor.waitFor({ timeout: 60_000 });

        // Wait for subscriptions to be set up
        await page.waitForTimeout(2000);

        await constructLoadRequest;
        expect(adminEndpointCalled).toBe(false);

        // Switch to constructs tab
        const tabs = page.locator('content-frame gtx-page-editor-tabs');
        await tabs.locator(`[data-id="${TAB_ID_CONSTRUCTS}"]`).click();

        // Click in editor to activate constructs
        await editor.click();

        // Verify constructs are loaded
        const controls = page.locator('content-frame gtx-page-editor-controls');
        const noConstructs = controls.locator('gtx-construct-controls .no-constructs');
        await expect(noConstructs).not.toBeVisible();

        const constructCategories = controls.locator('gtx-construct-controls .groups-container .construct-category');
        await expect(constructCategories).toHaveCount(2);
    });
});
