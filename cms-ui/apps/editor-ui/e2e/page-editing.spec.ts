import { TAB_ID_CONSTRUCTS } from '@gentics/cms-integration-api-models';
import { Page as CmsPage, PageSaveRequest, Template } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    clickModalAction,
    CONSTRUCT_CATEGORY_TESTS,
    CONSTRUCT_TEST_IMAGE,
    EntityImporter,
    FIXTURE_IMAGE_ONE,
    IMAGE_ONE,
    IMPORT_ID,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    loginWithForm,
    matchesUrl,
    matchRequest,
    navigateToApp,
    NODE_MINIMAL,
    onRequest,
    openContext,
    PAGE_ONE,
    pickSelectValue,
    TestSize,
    wait,
    waitForResponseFrom,
    findNotification,
} from '@gentics/e2e-utils';
import { expect, Frame, Locator, Page, test } from '@playwright/test';
import {
    ACTION_FORMAT_ABBR,
    ACTION_FORMAT_CITE,
    ACTION_FORMAT_QUOTE,
    ACTION_REMOVE_FORMAT,
    ACTION_SIMPLE_FORMAT_MAPPING,
    AUTH,
} from './common';
import {
    createExternalLink,
    createInternalLink,
    upsertLink,
    editorAction,
    findAlohaComponent,
    findDynamicDropdown,
    findItem,
    findList,
    findRepoBrowserItem,
    findRepoBrowserList,
    getAlohaIFrame,
    itemAction,
    openPageForEditing,
    overrideAlohaConfig,
    selectEditorTab,
    selectNode,
    selectRangeIn,
    selectTextIn,
    setupHelperWindowFunctions,
} from './helpers';

const CLASS_ACTIVE = 'active';

test.describe.configure({ mode: 'serial' });
test.describe('Page Editing', () => {
    // Mark this suite as slow - Because it is
    // test.slow();

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
            await IMPORTER.setupBinaryFiles({
                [IMAGE_ONE[IMPORT_ID]]: FIXTURE_IMAGE_ONE,
            });
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Specialized Test Setup', async () => {
            await IMPORTER.syncTag(BASIC_TEMPLATE_ID, 'content');
            await setupHelperWindowFunctions(page);
        });

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
        });
    });

    test.describe('Edit Mode', () => {

        let editingPage: CmsPage;
        let itemRow: Locator;
        let iframe: Frame;
        let mainEditable: Locator;

        async function openEditingPageInEditmode(page: Page) {
            await test.step('Open Page in Edit-Mode', async () => {
                const { row, iframe: pageIFrame, editable } = await openPageForEditing(page, editingPage);
                itemRow = row;
                iframe = pageIFrame;
                mainEditable = editable;
            });
        }

        test.describe('Basic Editing', () => {
            test.beforeEach(async ({ page }) => {
                editingPage = IMPORTER.get(PAGE_ONE);
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

                // FIXME: Investigate why we need this timeout/why save isn't executed immediately.
                // Possible reason being angular event bindings.
                await page.waitForTimeout(2000);

                await editorAction(page, 'save');

                const response = await saveRequest;
                const data = await response.request().postDataJSON();

                // Verify content was saved correctly
                // eslint-disable-next-line playwright/no-conditional-in-test
                const contentText = data?.page?.tags?.content?.properties?.text?.stringValue || '';
                const trimmedContent = contentText.replace(/<\/?p>/g, '').trim();
                expect(trimmedContent).toBe(TEXT_CONTENT);
            });

            test('should cancel editing when the edit view is closed', async ({ page }) => {
                const cancelRequest = page.waitForResponse(matchRequest('POST', `/rest/page/cancel/${editingPage.id}`));

                await editorAction(page, 'close');

                const res = await cancelRequest;
                expect(res.ok()).toBe(true);
            });

            test.describe('Mobile view', () => {
                test.use({ viewport: { width: 450, height: 812 } });

                test('should display list correctly after closing in mobile view', {
                    annotation: [{
                        type: 'ticket',
                        description: 'SUP-19055',
                    }],
                }, async ({ page }) => {
                    await page.route((url) => matchesUrl(url, `/rest/page/save/${editingPage.id}`), (route) => {
                        setTimeout(() => {
                            route.continue();
                        }, 5_000);
                    });

                    const saveRequest = page.waitForResponse(matchRequest('POST', `/rest/page/save/${editingPage.id}`));

                    await mainEditable.click();
                    await mainEditable.clear();
                    await mainEditable.fill('Foobardoo');

                    await editorAction(page, 'save');

                    await saveRequest;

                    await editorAction(page, 'close');

                    await page.locator('content-frame').waitFor({ state: 'detached' });
                    await expect(page.locator('folder-contents')).toBeInViewport({ ratio: 1.0 });
                });
            });

            test('should display list correctly after closing', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-19055',
                }],
            }, async ({ page }) => {
                await page.route((url) => matchesUrl(url, `/rest/page/save/${editingPage.id}`), (route) => {
                    setTimeout(() => {
                        route.continue();
                    }, 5_000);
                });

                const saveRequest = page.waitForResponse(matchRequest('POST', `/rest/page/save/${editingPage.id}`));

                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill('Foobardoo');

                await editorAction(page, 'save');

                await saveRequest;

                await editorAction(page, 'close');

                await page.locator('content-frame').waitFor({ state: 'detached' });
                // Ratio is "rather low", as the content may overflow/cause scrolling, and that
                // also counts towards viewport visibilty.
                await expect(page.locator('folder-contents')).toBeInViewport({ ratio: 0.8 });
            });
        });

        test.describe('Formatting', () => {
            test.beforeEach(() => {
                editingPage = IMPORTER.get(PAGE_ONE);
            });

            test.describe('add and remove basic formats', () => {
                const TEXT = 'Hello World';
                const FORMAT_ACTIONS = Object.entries(ACTION_SIMPLE_FORMAT_MAPPING);

                for (const format of FORMAT_ACTIONS) {
                    test(`should add and remove ${format[0]} formatting`, async ({ page }) => {
                        await openEditingPageInEditmode(page);

                        // Type and select text
                        await mainEditable.click();
                        await mainEditable.clear();
                        await mainEditable.fill(TEXT);

                        await mainEditable.press('ControlOrMeta+a');

                        await selectEditorTab(page, 'formatting');

                        // Apply format
                        let formatButton = findAlohaComponent(page, { slot: format[0], type: 'toggle-button' });
                        await formatButton.click();

                        // Verify format is applied
                        const formattedText = mainEditable.locator(format.tag);
                        await expect(formattedText).toHaveText(TEXT);
                        await mainEditable.click();

                        // Remove format
                        await mainEditable.press('ControlOrMeta+a');
                        // Activate the toolbar
                        await selectEditorTab(page, 'formatting');

                        formatButton = findAlohaComponent(page, { slot: ACTION_REMOVE_FORMAT, type: 'button' });
                        await formatButton.click();

                        // Verify format is removed
                        const hasFormat = await mainEditable.locator(format[1]).count();
                        expect(hasFormat).toBe(0);
                    });
                }
            });

            test('should be possible to configure the interchangeable node-names', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-19357',
                }],
            }, async ({ page }) => {
                const WORDS = ['Sample', 'text', 'to', 'test', 'out', 'different', 'formattings'];
                await overrideAlohaConfig(page, 'aloha-config-interchangable-names.js');
                await openEditingPageInEditmode(page);

                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(WORDS.join(' '));

                const boldButton = findAlohaComponent(page, { slot: 'bold' });
                const strongButton = findAlohaComponent(page, { slot: 'strong' });
                const italicButton = findAlohaComponent(page, { slot: 'italic' });
                const emphasizeButton = findAlohaComponent(page, { slot: 'emphasize' });

                await selectEditorTab(page, 'formatting');

                await test.step('format as bold', async () => {
                    await selectTextIn(mainEditable, WORDS[1]);
                    await boldButton.click();
                    await expect(mainEditable.locator('b')).toBeAttached();
                    await expect(boldButton).toContainClass(CLASS_ACTIVE);
                    await expect(strongButton).toContainClass(CLASS_ACTIVE);
                });

                await test.step('format as italic', async () => {
                    await selectTextIn(mainEditable, WORDS[2]);
                    await italicButton.click();
                    await expect(mainEditable.locator('i')).toBeAttached();
                    await expect(italicButton).toContainClass(CLASS_ACTIVE);
                    await expect(emphasizeButton).toContainClass(CLASS_ACTIVE);
                });

                await test.step('format as strong', async () => {
                    await selectTextIn(mainEditable, WORDS[3]);
                    await strongButton.click();
                    await expect(mainEditable.locator('strong')).toBeAttached();
                    await expect(strongButton).toContainClass(CLASS_ACTIVE);
                    await expect(boldButton).toContainClass(CLASS_ACTIVE);
                });

                await test.step('format as em', async () => {
                    await selectTextIn(mainEditable, WORDS[4]);
                    await emphasizeButton.click();
                    await expect(mainEditable.locator('em')).toBeAttached();
                    await expect(emphasizeButton).toContainClass(CLASS_ACTIVE);
                    await expect(italicButton).toContainClass(CLASS_ACTIVE);
                });
            });

            test.describe('toggle formats with keybinds', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-18814',
                }],
            }, () => {
                const KEYBINDS = [
                    { tag: 'b', combo: 'ControlOrMeta+b' },
                    { tag: 'i', combo: 'ControlOrMeta+i' },
                    { tag: 'u', combo: 'ControlOrMeta+u' },
                    // FIXME: Bugged
                    { tag: 'pre', combo: 'ControlOrMeta+p' },
                    // FIXME: Bugged
                    { tag: 'del', combo: 'ControlOrMeta+d' },
                    { tag: 'sub', combo: 'Alt+Shift+s' },
                    { tag: 'sup', combo: 'Control+Shift+s' },
                ];

                const TEXT = 'Test-Text';

                for (const bind of KEYBINDS) {
                    test(`should toggle format "${bind.tag}" with a keybind correctly`, async ({ page }) => {
                        await openEditingPageInEditmode(page);

                        await mainEditable.click();
                        await mainEditable.clear();
                        await mainEditable.fill(TEXT);

                        await mainEditable.press('ControlOrMeta+a');
                        await mainEditable.press(bind.combo);

                        // Verify format is applied
                        const formattedText = mainEditable.locator(bind.tag);
                        await expect(formattedText).toHaveText(TEXT);
                    });
                }
            });

            // FIXME: Bugged
            test('should switch typography with keybindings', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-18814',
                }],
            }, async ({ page }) => {
                const TEXT = 'Test-Text';

                await openEditingPageInEditmode(page);
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                for (let i = 1; i < 6; i++) {
                    await test.step(`change to h${i}`, async () => {
                        await mainEditable.press(`ControlOrMeta+Alt+${i}`);

                        // Verify typography is applied
                        const formattedText = mainEditable.locator(`h${i}`);
                        await expect(formattedText).toHaveText(TEXT);
                    });
                }

                await test.step('Change back to paragraph', async () => {
                    await mainEditable.press('ControlOrMeta+Alt+0');

                    // Verify typography is applied
                    const formattedText = mainEditable.locator('p');
                    await expect(formattedText).toHaveText(TEXT);
                });
            });

            test('should add and remove quote formatting', async ({ page }) => {
                const TEXT = 'Hello World';

                await openEditingPageInEditmode(page);
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                await selectEditorTab(page, 'formatting');
                let formatButton = findAlohaComponent(page, { slot: ACTION_FORMAT_QUOTE, type: 'toggle-button' });
                await formatButton.click();

                const quote = mainEditable.locator('q');
                await expect(quote).toContainText(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                await selectEditorTab(page, 'formatting');
                formatButton = findAlohaComponent(page, { slot: ACTION_REMOVE_FORMAT, type: 'button' });
                await formatButton.click();

                const hasQuote = await mainEditable.locator('q').count();
                expect(hasQuote).toBe(0);
            });

            test.skip('should add abbreviation with title', async ({ page }) => {
                const TEXT = 'HTML';
                const TITLE = 'HyperText Markup Language';

                await openEditingPageInEditmode(page);
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                await selectEditorTab(page, 'formatting');
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

                await openEditingPageInEditmode(page);
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                await selectEditorTab(page, 'formatting');
                const formatButton = findAlohaComponent(page, { slot: ACTION_FORMAT_CITE, type: 'toggle-button' });
                await formatButton.click();

                // Verify citation
                const cite = mainEditable.locator('cite');
                // await expect(cite).toHaveAttribute('title', SOURCE);
                await expect(cite).toContainText(TEXT);
            });

            test('should be able to set link anchor for empty external link', async ({ page }) => {
                const TEXT_CONTENT = 'Hello ';
                const LINK_TEXT = 'World';
                const LINK_ANCHOR = 'test-anchor';

                await openEditingPageInEditmode(page);
                await test.step('Create link', async () => {
                    // Type content and select text for link
                    await mainEditable.click();
                    await mainEditable.clear();
                    await mainEditable.fill(TEXT_CONTENT + LINK_TEXT);

                    expect(await selectRangeIn(mainEditable, TEXT_CONTENT.length, TEXT_CONTENT.length + LINK_TEXT.length)).toBe(true);

                    await createExternalLink(page, async (form) => {
                        await form.locator('[data-slot="url"] .anchor-input input').fill(LINK_ANCHOR);
                    });
                });

                await test.step('Verify link was created', async () => {
                    const linkElement = mainEditable.locator('a');

                    await expect(linkElement).toHaveAttribute('href', `#${LINK_ANCHOR}`);
                    await expect(linkElement).toHaveAttribute('data-gentics-gcn-anchor', LINK_ANCHOR);
                    await expect(linkElement).toHaveText(LINK_TEXT);
                });

                await test.step('Verify anchor is set when re-opening dialog', async () => {
                    const insertLinkButton = findAlohaComponent(page, {
                        slot: 'insertLink',
                        action: 'secondary',
                        type: 'toggle-split-button',
                    });

                    await insertLinkButton.click();

                    const modal = page.locator('gtx-dynamic-form-modal');
                    const form = modal.locator('.form-wrapper');

                    await expect(form.locator('[data-slot="url"] .target-input input')).toHaveValue('');
                    await expect(form.locator('[data-slot="url"] .anchor-input input')).toHaveValue(LINK_ANCHOR);
                });
            });

            test('should be able to edit inline-editables with simple formatting', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-18800',
                }],
            }, async ({ page }) => {
                await openEditingPageInEditmode(page);

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
                await expect(findAlohaComponent(page, { slot: 'typography' })).toBeHidden();
                await expect(findAlohaComponent(page, { slot: 'listOrdered' })).toBeHidden();
                await expect(findAlohaComponent(page, { slot: 'listUnordered' })).toBeHidden();
                await expect(findAlohaComponent(page, { slot: 'listDefinition' })).toBeHidden();

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
        });

        test.describe('Links', () => {
            test.beforeEach(async ({ page }) => {
                editingPage = IMPORTER.get(PAGE_ONE);
                await openEditingPageInEditmode(page);
            });

            test('should be able to select an internal page as link', async ({ page }) => {
                const TEXT_CONTENT = 'Hello ';
                const LINK_TEXT = 'World';
                const LINK_ITEM = IMPORTER.get(PAGE_ONE);
                const ITEM_NODE = IMPORTER.get(NODE_MINIMAL);
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
                await createInternalLink(page, async (repoBrowser) => {
                    await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${LINK_ITEM.id}"] .item-checkbox label`).click();
                }, async (form) => {
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

            test('should show an alert and an inactive modal while IO error on opening a link editor', async ({ page }) => {
                const TEXT_CONTENT = 'Gen ';
                const LINK_TEXT = 'ticks';
                const LINK_ITEM = IMPORTER.get(PAGE_ONE);
                const ITEM_NODE = IMPORTER.get(NODE_MINIMAL)!;
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

                page.route(new RegExp(`\\/rest\\/page\\/load\\/${LINK_ITEM.id}`), (route) => {
                    route.abort('connectionreset');
                });
                await upsertLink(page, async (_form) => {
                    await expect(findNotification(page)).toBeVisible();
                    await expect(page.locator('.modal-footer [data-action="confirm"] button[data-action="primary"]')).toHaveAttribute('disabled');
                }, 'secondary');
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
                    TEMPLATE_TAGS.forEach((tagName) => tags.delete(tagName));

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

                    // Wait for the content to be loaded and processed
                    await mainEditable.locator('img').waitFor({ state: 'detached' });
                    await page.waitForTimeout(1_000);

                    const linkIds = await mainEditable.evaluate((el) => {
                        return Array.from(el.querySelectorAll('a'))
                            .map((link) => link.getAttribute('data-gcn-tagid'));
                    });

                    expect(linkIds).toHaveLength(2);
                    expect(linkIds[0]).not.toEqual(linkIds[1]);

                    const saveReq = page.waitForRequest(matchRequest('POST', `/rest/page/save/${editingPage.id}`));
                    await editorAction(page, 'save');
                    const req = await saveReq;
                    const pageUpdate = await req.postDataJSON() as PageSaveRequest;

                    const tags = new Set<string>(Object.keys(pageUpdate.page.tags || {}));
                    // Remove template tags
                    TEMPLATE_TAGS.forEach((tagName) => tags.delete(tagName));

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
                const LINK_ITEM = IMPORTER.get(PAGE_ONE);
                await createLinkCopyPasteTest(page, async () => {
                    await createInternalLink(page, async (repoBrowser) => {
                        await repoBrowser.locator(`repository-browser-list[data-type="page"] [data-id="${LINK_ITEM.id}"] .item-checkbox label`).click();
                    }, async () => {
                        // nop
                    });
                });
            });

            test('should be possible to copy an external link', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-18537',
                }],
            }, async ({ page }) => {
                await createLinkCopyPasteTest(page, async () => {
                    await createExternalLink(page, async (form) => {
                        await form.locator('[data-slot="url"] .target-input input').fill('https://example.com');
                        await form.locator('[data-slot="title"] input').fill('A very interesting site');
                    });
                });
            });

            test('should be possible to paste plain text', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-19262',
                }],
            }, async ({ page, context }) => {
                await mainEditable.click();
                await mainEditable.clear();

                await context.grantPermissions(['clipboard-write']);
                await page.evaluate(() => navigator.clipboard.writeText('Hello from Playwright!'));
                await mainEditable.press('ControlOrMeta+v');
                await expect(mainEditable).toHaveText('Hello from Playwright!');
            });

            // FIXME: Bugged
            test('should be possible to insert a link with a keybind', {
                annotation: [{
                    type: 'ticket',
                    description: 'SUP-18814',
                }],
            }, async ({ page }) => {
                const TEXT = 'Hello World';

                // Type and select text
                await mainEditable.click();
                await mainEditable.clear();
                await mainEditable.fill(TEXT);

                await mainEditable.press('ControlOrMeta+a');

                await mainEditable.press('Control+k');

                // Modal should be open now
                await expect(page.locator('gtx-dynamic-form-modal')).toBeVisible();
            });
        });

        test.describe('Tables', () => {
            const SLOT_CELL_STYLE = 'tableCellStyle';
            const SLOT_CREATE_TABLE = 'createTable';
            const ROW_COUNT = 3;
            const COLUMN_COUNT = 3;

            test('should be able to insert a table', async ({ page }) => {
                await editPageAndCreateTable(page);

                await expect(mainEditable.locator('table')).toBeVisible();
            });

            test('should not be able to style table without config', async ({ page }) => {
                await editPageAndCreateTable(page);

                const table = mainEditable.locator('table');
                await expect(table).toBeVisible();

                const topLeftCell = table.locator('.aloha-table-leftuppercorner');
                await topLeftCell.click();

                const styleToggle = findAlohaComponent(page, { slot: SLOT_CELL_STYLE });
                await expect(styleToggle).toBeDisabled();
            });

            test('should be able to style table with config', async ({ page }) => {
                const STYLE_NAME = 'table-style-1';

                await overrideAlohaConfig(page, 'aloha-config-table-test.js');
                await editPageAndCreateTable(page);

                const table = mainEditable.locator('table');
                await expect(table).toBeVisible();

                const topLeftCell = table.locator('.aloha-table-leftuppercorner');
                await topLeftCell.click();

                const styleToggle = findAlohaComponent(page, { slot: SLOT_CELL_STYLE });
                await styleToggle.click();

                const styleOptionsContainer = findDynamicDropdown(page, SLOT_CELL_STYLE);
                const styleOptions = styleOptionsContainer.locator('.select-menu-entry');
                await expect(styleOptions).toHaveCount(8);

                const tableStyle = styleOptionsContainer.locator(`.select-menu-entry[data-id="${STYLE_NAME}"]`);
                await tableStyle.click();

                await expect(table).toContainClass(STYLE_NAME);
            });

            test('should be able to style column with config', async ({ page }) => {
                const STYLE_NAME = 'column-style-1';

                await overrideAlohaConfig(page, 'aloha-config-table-test.js');
                await editPageAndCreateTable(page);

                const table = mainEditable.locator('table');
                const firstColumn = table.locator('.aloha-table-selectcolumn td:nth-child(2)');
                await firstColumn.click();

                const styleToggle = findAlohaComponent(page, { slot: SLOT_CELL_STYLE });
                await styleToggle.click();

                const styleOptionsContainer = findDynamicDropdown(page, SLOT_CELL_STYLE);
                const styleOptions = styleOptionsContainer.locator('.select-menu-entry');
                await expect(styleOptions).toHaveCount(4);

                const columnStyle = styleOptionsContainer.locator(`.select-menu-entry[data-id="${STYLE_NAME}"]`);
                await columnStyle.click();

                const cells = await mainEditable.locator('table tr:not(.aloha-table-selectcolumn) td:not(.aloha-table-selectrow):nth-child(2)').all();

                expect(cells).toHaveLength(COLUMN_COUNT);
                for (const cell of cells) {
                    await expect(cell).toContainClass(STYLE_NAME);
                }
            });

            test('should be able to style row with config', async ({ page }) => {
                const STYLE_NAME = 'row-style-1';

                await overrideAlohaConfig(page, 'aloha-config-table-test.js');
                await editPageAndCreateTable(page);

                const table = mainEditable.locator('table');
                const firstRow = table.locator('tr:nth-child(2) .aloha-table-selectrow');
                await firstRow.click();

                const styleToggle = findAlohaComponent(page, { slot: SLOT_CELL_STYLE });
                await styleToggle.click();

                const styleOptionsContainer = findDynamicDropdown(page, SLOT_CELL_STYLE);
                const styleOptions = styleOptionsContainer.locator('.select-menu-entry');
                await expect(styleOptions).toHaveCount(4);

                const rowStyle = styleOptionsContainer.locator(`.select-menu-entry[data-id="${STYLE_NAME}"]`);
                await rowStyle.click();

                const cells = await mainEditable.locator('table tr:not(.aloha-table-selectcolumn):nth-child(2) td:not(.aloha-table-selectrow)').all();

                expect(cells).toHaveLength(ROW_COUNT);
                for (const cell of cells) {
                    await expect(cell).toContainClass(STYLE_NAME);
                }
            });

            async function editPageAndCreateTable(page) {
                editingPage = IMPORTER.get(PAGE_ONE);

                await openEditingPageInEditmode(page);
                await mainEditable.click();
                await mainEditable.clear();

                await selectEditorTab(page, 'insert');
                await findAlohaComponent(page, { slot: SLOT_CREATE_TABLE }).click();
                const dropdown = findDynamicDropdown(page, SLOT_CREATE_TABLE);

                await dropdown.locator(`gtx-table-size-select .grid-row:nth-child(${ROW_COUNT}) .cell:nth-child(${COLUMN_COUNT})`).click();
            }
        });

        test.describe('Constructs', () => {
            test.beforeEach(async ({ page }) => {
                editingPage = IMPORTER.get(PAGE_ONE);
                await openEditingPageInEditmode(page);
            });

            // FIXME: The drag-n-drop simply doesn't do anything in the test; Therefore functionality can't be properly tested.
            // Tried already all kinds of workarounds, but sadly nothing works so far.
            test.skip('should be able to move a construct between two existing ones', async ({ page }) => {
                const TEST_IMAGE = IMPORTER.get(IMAGE_ONE);

                // Clear the content
                await mainEditable.click();
                await mainEditable.clear();

                await selectEditorTab(page, 'gtx.constructs');
                const toolbar = page.locator('content-frame gtx-editor-toolbar');
                const controls = toolbar.locator('gtx-construct-controls');
                const category = controls.locator(`.construct-category[data-global-id="${CONSTRUCT_CATEGORY_TESTS}"]`);

                let imageCounter = 1;
                async function createImageTag(): Promise<void> {
                    await test.step(`Create Image ${imageCounter++}`, async () => {
                        const createReq = waitForResponseFrom(page, 'POST', `/rest/page/newtag/${editingPage.id}`);
                        const dropdown = await openContext(category);
                        await dropdown.locator(`[data-global-id="${CONSTRUCT_TEST_IMAGE}"]`).click();
                        await createReq;

                        const block = mainEditable.locator('.GENTICS_block').last();
                        await block.locator('.gcn-construct-button-edit').click();

                        const modal = page.locator('gtx-tag-editor-modal');
                        const editor = modal.locator('file-or-image-url-tag-property-editor');
                        await editor.locator('browse-box [data-action="browse"] button').click();

                        const repoBrowser = page.locator('repository-browser');
                        const images = findRepoBrowserList(repoBrowser, ITEM_TYPE_IMAGE);
                        const targetImage = findRepoBrowserItem(images, TEST_IMAGE.id);
                        await targetImage.locator('gtx-checkbox label').click();
                        await clickModalAction(repoBrowser, 'confirm');

                        const renderReq = waitForResponseFrom(page, 'POST', '/rest/page/renderTag/*');
                        await modal.locator('[data-action="confirm"] button').click();
                        await renderReq;
                    });
                }

                await createImageTag();
                await createImageTag();
                await createImageTag();

                await test.step('Move image', async () => {
                    const blocks = mainEditable.locator('.GENTICS_block');

                    const idsBefore = await Promise.all((await blocks.all()).map((loc) => loc.getAttribute('id')));

                    const originImage = blocks.last();
                    const targetImage = blocks.first();

                    const blockId = await originImage.getAttribute('id');
                    const targetRect = await targetImage.evaluate(el => el.getBoundingClientRect());

                    await originImage.locator('.aloha-block-handle .gcn-construct-drag-handle').dragTo(targetImage, {
                        targetPosition: {
                            x: 10,
                            y: targetRect.bottom,
                        },
                    });

                    const idsAfter = await Promise.all((await blocks.all()).map((loc) => loc.getAttribute('id')));

                    // IDs should have changed
                    expect(idsBefore).not.toStrictEqual(idsAfter);
                    await expect(blocks.last()).not.toHaveAttribute('id', blockId);
                });
            });

            test('should render new tag after inserting into editable', async ({page}) => {
                // Clear the content
                await mainEditable.click();
                await mainEditable.clear();

                await selectEditorTab(page, 'gtx.constructs');
                const toolbar = page.locator('content-frame gtx-editor-toolbar');
                const controls = toolbar.locator('gtx-construct-controls');
                const category = controls.locator(`.construct-category[data-global-id="${CONSTRUCT_CATEGORY_TESTS}"]`);

                const renderUrl = '/rest/page/renderTag/*';
                let postedEditableContent = "";
                page.on('request', request => {
                    if (request.method() === 'POST' && matchesPath(request.url(), renderUrl)) {
                        const body = JSON.parse(request.postData());
                        postedEditableContent = body.tags['content'].properties.text.stringValue;
                    }
                });
                const createReq = waitForResponseFrom(page, 'POST', `/rest/page/newtag/${editingPage.id}`);
                const renderReq = waitForResponseFrom(page, 'POST', renderUrl);
                const dropdown = await openContext(category);
                await dropdown.locator(`[data-global-id="${CONSTRUCT_TEST_IMAGE}"]`).click();
                const createResponse = await createReq;
                const createResponseBody = await createResponse.json();
                const tagName = createResponseBody.tag.name;
                await renderReq;

                expect(postedEditableContent).toContain(`<node ${tagName}>`);
            });
        });
    });

    test('should load edit mode correctly when switching from preview mode', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-19297',
        }]
    }, async ({ page }) => {
        // Setup aloha-page listener
        let calls = 0;
        page.route((url) => matchesUrl(url, '/alohapage'), async (route) => {
            if (route.request().method() === 'GET') {
                calls++;
                // Delay by 3 seconds to emulate rendering
                await wait(3_000);
            }
            return route.continue();
        });

        // Open page in preview
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, IMPORTER.get(PAGE_ONE).id);
        await item.locator('.item-primary .item-name-only').click();

        // Wait for preview to be loaded completely
        const iframe = page.locator('content-frame iframe.master-frame[loaded="true"]');
        await iframe.waitFor({ timeout: 60_000 });
        await iframe.contentFrame().locator('main').waitFor({ timeout: 60_000 });

        // Now open edit-mode and wait for it to load
        await editorAction(page, 'edit');
        await iframe.waitFor({ timeout: 60_000 });
        await iframe.contentFrame().locator('main .container [contenteditable="true"]').waitFor({ timeout: 60_000 });

        expect(calls).toEqual(2);
    });

    test('should load constructs correctly when switching to edit mode', {
        annotation: {
            type: 'ticket',
            description: 'SUP-17542',
        },
    }, async ({ page }) => {
        // Admin request which shouldn't be used/called
        let adminEndpointCalled = false;
        onRequest(page, matchRequest('POST', '/rest/construct'), () => {
            adminEndpointCalled = true;
        });

        // Regular endpoint which should be used
        const constructLoadRequest = waitForResponseFrom(page, 'GET', '/rest/construct', {
            params: {
                nodeId: IMPORTER.get(NODE_MINIMAL).id.toString(),
                pageId: IMPORTER.get(PAGE_ONE).id.toString(),
            },
        });

        // Setup page for editing
        const list = findList(page, ITEM_TYPE_PAGE);
        const item = findItem(list, IMPORTER.get(PAGE_ONE).id);
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
        await selectEditorTab(page, TAB_ID_CONSTRUCTS);

        // Click in editor to activate constructs
        await editor.click();

        // Verify constructs are loaded
        const controls = page.locator('content-frame gtx-page-editor-controls');
        const noConstructs = controls.locator('gtx-construct-controls .no-constructs');
        await expect(noConstructs).toBeHidden();

        const constructCategories = controls.locator('gtx-construct-controls .groups-container .construct-category');
        await expect(constructCategories).toHaveCount(2);
    });
});
