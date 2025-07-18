import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import { EntityImporter, hasMatchingParams, matchesPath, ITEM_TYPE_PAGE, minimalNode, pageOne, TestSize } from '@gentics/e2e-utils';
import { expect, Locator, test } from '@playwright/test';
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
    AUTH_ADMIN,
} from './common';
import {
    editorAction,
    findAlohaComponent,
    findItem,
    findList,
    HelperWindow,
    initPage,
    itemAction,
    login,
    selectNode,
    selectOption,
    getAlohaIFrame,
} from './helpers';

// Skipped until we find out why it can't find the iframe content in jenkins
test.describe('Page Editing', () => {
    // Mark this suite as slow - Because it is
    test.slow();

    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);

        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
        await IMPORTER.syncTag(1, 'content');

        await initPage(page);
        await page.goto('/');
        await login(page, AUTH_ADMIN);
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
    });

    test.describe('Edit Mode', () => {

        let editor: Locator;

        test.beforeEach(async ({ page }) => {
            // Setup page for editing
            const list = findList(page, ITEM_TYPE_PAGE);
            const item = findItem(list, IMPORTER.get(pageOne).id);
            await itemAction(item, 'edit');

            // Wait for editor to be ready
            const iframe = await getAlohaIFrame(page);
            editor = iframe.locator('main [contenteditable="true"]');
            await editor.waitFor({ timeout: 60_000 });
        });

        test('should be able to add new text to the content-editable', async ({ page }) => {
            const TEXT_CONTENT = 'Foo bar hello world';

            // Type content into editor
            await editor.click();
            await editor.clear();
            await editor.fill(TEXT_CONTENT);

            // Save and verify request
            const saveRequest = page.waitForRequest(request => {
                const matches = request.method() === 'POST'
                    && matchesPath(request.url(), `/rest/page/save/${IMPORTER.get(pageOne).id}`);
                console.log('is save request', request.url(), matches);
                return matches;
            });

            // TODO: Investigate why we need this timeout/why save isn't executed immediately.
            // Possible reason being angular event bindings.
            await page.waitForTimeout(2000);

            await editorAction(page, 'save');

            const request = await saveRequest;
            const data = await request.postDataJSON();

            // Verify content was saved correctly
            const contentText = data?.page?.tags?.content?.properties?.text?.stringValue || '';
            const trimmedContent = contentText.replace(/<\/?p>/g, '').trim();
            expect(trimmedContent).toBe(TEXT_CONTENT);
        });

        test('should cancel editing when the edit view is closed', async ({ page }) => {
            const cancelRequest = page.waitForRequest(request =>
                request.url().includes('/rest/page/cancel/') &&
                request.method() === 'POST',
            );

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
            await editor.click();
            await editor.clear();
            await editor.fill(TEXT_CONTENT + LINK_TEXT);

            // Select text to make into link
            expect(await editor.evaluate((el, context) => {
                window.getSelection().removeAllRanges();
                const applied = (window as unknown as HelperWindow).selectRange(
                    el as HTMLElement,
                    context.TEXT_CONTENT.length,
                    context.TEXT_CONTENT.length + context.LINK_TEXT.length,
                );

                if (applied) {
                    (window as unknown as HelperWindow).updateAlohaRange(window, window.getSelection().getRangeAt(0));
                }

                return applied;
            }, { TEXT_CONTENT, LINK_TEXT })).toBe(true);

            const linkButton = findAlohaComponent(page, { slot: 'insertLink', type: 'toggle-split-button' });
            await linkButton.locator('button[data-action="primary"]').click();

            // Fill link form
            const modal = page.locator('gtx-dynamic-form-modal');
            const form = modal.locator('.form-wrapper');

            // Select internal page
            await form.locator('[data-slot="url"] .target-wrapper .internal-target-picker').click();
            const repoBrowser = page.locator('repository-browser');
            await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${LINK_ITEM.id}"] .item-checkbox label`).click();
            await repoBrowser.locator('.modal-footer [data-action="confirm"] button[data-action="primary"]').click();

            // Fill other fields
            await form.locator('[data-slot="url"] .anchor-input input').fill(LINK_ANCHOR);
            await form.locator('[data-slot="title"] input').fill(LINK_TITLE);
            await selectOption(form.locator('[data-slot="target"] gtx-select'), LINK_TARGET);
            await form.locator('[data-slot="lang"] input').fill(LINK_LANGUAGE);

            // Confirm link creation
            await modal.locator('.modal-footer [data-action="confirm"] button[data-action="primary"]').click();

            // Verify link was created
            const linkElement = editor.locator('a');
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

        test.describe.skip('Formatting', () => {
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
                        await editor.click();
                        await editor.clear();
                        await editor.fill(TEXT);
                        await page.keyboard.press('Control+a');

                        // Apply format
                        await editorAction(page, format.action);

                        // Verify format is applied
                        const formattedText = await editor.locator(format.tag).textContent();
                        expect(formattedText).toBe(TEXT);

                        // Remove format
                        await page.keyboard.press('Control+a');
                        await editorAction(page, ACTION_REMOVE_FORMAT);

                        // Verify format is removed
                        const hasFormat = await editor.locator(format.tag).count();
                        expect(hasFormat).toBe(0);
                    });
                }
            });

            test('should add and remove quote formatting', async ({ page }) => {
                const TEXT = 'Hello World';

                await editor.click();
                await editor.clear();
                await editor.fill(TEXT);
                await page.keyboard.press('Control+a');

                await editorAction(page, ACTION_FORMAT_QUOTE);

                const quote = editor.locator('blockquote');
                await expect(quote).toContainText(TEXT);

                await page.keyboard.press('Control+a');
                await editorAction(page, ACTION_REMOVE_FORMAT);

                const hasQuote = await editor.locator('blockquote').count();
                expect(hasQuote).toBe(0);
            });

            test('should add abbreviation with title', async ({ page }) => {
                const TEXT = 'HTML';
                const TITLE = 'HyperText Markup Language';

                await editor.click();
                await editor.clear();
                await editor.fill(TEXT);
                await page.keyboard.press('Control+a');

                await editorAction(page, ACTION_FORMAT_ABBR);

                // Fill abbreviation form
                const modal = page.locator('gtx-dynamic-modal');
                await modal.locator('input[formcontrolname="title"]').fill(TITLE);
                await modal.locator('.modal-footer [data-action="confirm"]').click();

                // Verify abbreviation
                const abbr = editor.locator('abbr');
                await expect(abbr).toHaveAttribute('title', TITLE);
                await expect(abbr).toContainText(TEXT);
            });

            test('should add citation with source', async ({ page }) => {
                const TEXT = 'The quote';
                const SOURCE = 'Famous Author';

                await editor.click();
                await editor.clear();
                await editor.fill(TEXT);
                await page.keyboard.press('Control+a');

                await editorAction(page, ACTION_FORMAT_CITE);

                // Fill citation form
                const modal = page.locator('gtx-dynamic-modal');
                await modal.locator('input[formcontrolname="cite"]').fill(SOURCE);
                await modal.locator('.modal-footer [data-action="confirm"]').click();

                // Verify citation
                const cite = editor.locator('cite');
                await expect(cite).toHaveAttribute('title', SOURCE);
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
                && matchesPath(request.url(), '/rest/construct/list')
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
        const editor = iframe.contentFrame().locator('main [contenteditable="true"]');
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
