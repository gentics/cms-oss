import { expect, type Page, test } from '@playwright/test';

// The preview of an S1 session (content creation): GenAIx sends `preview.updated` after every CMS
// write, and the right column reloads the page each time. The events follow the S1 example of the
// integration guide (09-integration-guide.md, "A compact annotated run"); the frame's page is
// stubbed, as the dev server has no CMS.

const SESSION = 's-1';
const PREVIEW_URL = 'http://127.0.0.1:5173/alohapage?realid=9142&nodeid=3&mode=view';

function previewUpdated(seq: number, changedElements: string[]): string {
    const data = {
        seq,
        ts: '2026-10-07T09:00:00Z',
        session_id: SESSION,
        type: 'preview.updated',
        cms_ref: { type: 'page', id: 9142, node_id: 3 },
        preview_url: PREVIEW_URL,
        mode: 'view',
        changed_elements: changedElements,
    };

    return `id: ${seq}\nevent: preview.updated\ndata: ${JSON.stringify(data)}\n\n`;
}

/**
 * Stubs the session's event stream and the CMS page in the frame. The stream sends the first
 * `preview.updated` at once and each further one after `sendNextUpdate()`; in between, each request
 * ends without events and the client reconnects. The frame's n-th load gets `pages[n]` (the last one
 * repeats), the n-th update `changed[n]` as `changed_elements` (none when missing). Returns the URLs the
 * frame loaded.
 */
async function stubS1Session(page: Page, pages: string[] = ['<p>Neue Nutzungsbedingungen ab Q3</p>'], changed: string[][] = []) {
    const loadedUrls: string[] = [];
    let updatesDue = 1;

    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/events`, (route) => {
        const after = Number(new URL(route.request().url()).searchParams.get('after') ?? 0);
        let body = '';

        if (after < updatesDue) {
            body = previewUpdated(after + 1, changed[after] ?? []);
        }

        return route.fulfill({ status: 200, contentType: 'text/event-stream', body });
    });
    await page.route((url) => url.pathname === '/alohapage', (route) => {
        const body = pages[Math.min(loadedUrls.length, pages.length - 1)]!;

        loadedUrls.push(route.request().url());

        return route.fulfill({ contentType: 'text/html', body });
    });

    return {
        loadedUrls,
        sendNextUpdate: () => {
            updatesDue += 1;
        },
    };
}

test.describe('Preview of an S1 session', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('shows the page read-only once GenAIx sends a preview, and reloads it on each update', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page);

        await page.goto(`#/sessions/${SESSION}`);

        const frame = page.getByTitle('Preview of the page in the CMS');

        await expect(page.getByRole('region', { name: 'Preview' })).toBeVisible();
        await expect(frame).toHaveAttribute('src', `${PREVIEW_URL}&real=view&tagmarkers=true`);
        await expect(page.frameLocator('iframe[title="Preview of the page in the CMS"]').getByText('Neue Nutzungsbedingungen ab Q3')).toBeVisible();
        expect(loadedUrls).toEqual([`${PREVIEW_URL}&real=view&tagmarkers=true`]);

        sendNextUpdate();

        await expect.poll(() => loadedUrls.length).toBe(2);
        expect(loadedUrls[1]).toBe(`${PREVIEW_URL}&real=view&tagmarkers=true`);
    });
});

// A page as the CMS renders it read-only with COPY_TAGS: marked parts (a text inside a section) and
// unmarked structure around them. `withText` false leaves the text part out, as after the AI removed it.
function markedPage(withText = true): string {
    const text = withText
        ? '<p data-gcn-pageid="9142" data-gcn-tagid="12" data-gcn-tagname="content_2" data-gcn-i18n-constructname="Text">Ab dem <a href="/elsewhere">1. Juli</a> gilt</p>'
        : '';

    return `<!doctype html><body style="margin:0;font:16px sans-serif">
        <header style="padding:20px">Kopfzeile</header>
        <div data-gcn-pageid="9142" data-gcn-tagid="11" data-gcn-tagname="content_1" data-gcn-i18n-constructname="Section" style="padding:30px">
            ${text}<p>Ohne Markierung</p>
        </div></body>`;
}

test.describe('Selecting a part in the preview', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('outlines the innermost part under the mouse and names its type', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);

        const text = page.frameLocator(frameSelector).getByText('Ab dem');

        await text.hover();

        const label = page.getByText('Text', { exact: true });

        await expect(label).toBeVisible();

        // The outline's box is the text part's box (the label sits at its top left).
        const partBox = (await text.boundingBox())!;
        const labelBox = (await label.boundingBox())!;

        expect(Math.abs(labelBox.x - partBox.x)).toBeLessThan(2);
        expect(Math.abs(labelBox.y - partBox.y)).toBeLessThan(2);

        await page.frameLocator(frameSelector).getByText('Ohne Markierung').hover();
        await expect(page.getByText('Section', { exact: true })).toBeVisible();
        await expect(label).toBeHidden();
    });

    test('selects a part by clicking it, without following a link in it, and clears it with Escape', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('link', { name: '1. Juli' }).click();

        await expect(page.getByText('Selected: Text')).toBeVisible();
        // Still the page: a followed link would have replaced it with whatever /elsewhere serves.
        await expect(page.frameLocator(frameSelector).getByText('Ab dem')).toBeVisible();
        await expect(page.getByTitle('Preview of the page in the CMS')).toHaveJSProperty('contentWindow.location.pathname', '/alohapage');

        // The first Escape closes the element menu and returns to the page, the second clears the selection.
        await page.keyboard.press('Escape');
        await expect(page.getByRole('menu')).toBeHidden();
        await expect(page.getByText('Selected: Text')).toBeVisible();
        await page.keyboard.press('Escape');
        await expect(page.getByText('Selected: Text')).toBeHidden();
    });

    test('keeps the selection across a reload, and clears it with a note when the part is gone', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page, [markedPage(), markedPage(), markedPage(false)]);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await expect(page.getByText('Selected: Text')).toBeVisible();

        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(2);
        await expect(page.frameLocator(frameSelector).getByText('Ab dem')).toBeVisible();
        await expect(page.getByText('Selected: Text')).toBeVisible();

        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(3);
        await expect(page.getByText('The selected part is no longer on the page')).toBeVisible();
        await expect(page.getByText('Selected: Text')).toBeHidden();
    });

    test('clears the selection with the button in the header', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();

        await page.getByRole('button', { name: 'Clear selection' }).click();

        await expect(page.getByText('Selected: Text')).toBeHidden();
    });
});

// Sending a turn: the default cms connection, a session already authorized for it (so no new CMS
// token is needed), and the message endpoint. Returns the bodies posted to it.
async function stubSending(page: Page) {
    const posted: unknown[] = [];

    await page.route((url) => url.pathname === '/rest/proxy/genaix/mcp/connections', (route) => route.fulfill({
        json: { items: [{ id: 'c-1', connector: 'cms', default: true }] },
    }));
    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}`, (route) => route.fulfill({
        json: { id: SESSION, status: 'active', title: 'Neue Nutzungsbedingungen', authorizations: [{ connection_id: 'c-1', status: 'authorized' }] },
    }));
    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/messages`, (route) => {
        if (route.request().method() !== 'POST') {
            return route.fulfill({ json: { items: [], next_cursor: null } });
        }

        posted.push(route.request().postDataJSON());

        return route.fulfill({ status: 202, json: { message_id: 'm-1', run_id: 'r-1', session_id: SESSION } });
    });

    return posted;
}

test.describe('Referencing a part of the preview in the chat', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('adds the clicked part to the chat from its element menu and sends it as the page plus the part named in the text', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Reference in the chat' }).click();

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(field.getByText('Text · content_2')).toBeVisible();
        await expect(page.getByText('Added as a reference — now write what should happen.')).toBeVisible();
        await expect(field).toBeFocused();

        await page.keyboard.type('make this shorter');
        await page.keyboard.press('Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_2" (Text): make this shorter' },
            ],
        });
    });

    test('closes the element menu with Escape and keeps the selection', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();

        await expect(page.getByRole('menuitem', { name: 'Reference in the chat' })).toBeVisible();
        await page.keyboard.press('Escape');

        await expect(page.getByRole('menu')).toBeHidden();
        await expect(page.getByText('Selected: Text')).toBeVisible();
    });
});

test.describe('Changed parts in the preview', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('tints the parts the last update changed, until the next update', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page, [markedPage()], [['content_2', 'not_on_the_page'], []]);

        await page.goto(`#/sessions/${SESSION}`);

        const text = page.frameLocator(frameSelector).getByText('Ab dem');
        const tint = page.locator('[data-changed-part]');

        await expect(text).toBeVisible();
        await expect(tint).toHaveCount(1);
        await expect(tint).toHaveAttribute('data-changed-part', 'content_2');

        // The tint covers the changed part.
        const partBox = (await page.frameLocator(frameSelector).locator('[data-gcn-tagname="content_2"]').boundingBox())!;
        const tintBox = (await tint.boundingBox())!;

        for (const key of ['x', 'y', 'width', 'height'] as const) {
            expect(Math.abs(tintBox[key] - partBox[key])).toBeLessThan(2);
        }

        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(2);
        await expect(text).toBeVisible();
        await expect(tint).toHaveCount(0);
    });
});

test.describe('Feedback on a part of the preview', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('sends the feedback as the page plus the part named in the text, with the rest of the page dimmed meanwhile', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Give feedback' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });
        const field = card.getByRole('textbox', { name: 'What should happen to this spot?' });

        await expect(card.getByText('Text · content_2')).toBeVisible();
        await expect(field).toBeFocused();
        await expect(page.locator('[data-dim]')).toHaveCount(4);
        await expect(card.getByRole('button', { name: 'Send' })).toBeDisabled();

        await field.fill('shorter');
        await field.press('Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_2" (Text): shorter' },
            ],
        });
        await expect(card).toBeHidden();
        await expect(page.locator('[data-dim]')).toHaveCount(0);
    });

    test('moves the part and the typed feedback into the chat instead', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Give feedback' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'What should happen to this spot?' }).fill('more active');
        await card.getByRole('button', { name: 'In the chat' }).click();

        const composer = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(card).toBeHidden();
        await expect(composer).toContainText('Text · content_2');
        await expect(composer).toContainText('more active');
        await expect(composer).toBeFocused();
    });

    test('closes the card with its cancel button, without sending', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Give feedback' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'What should happen to this spot?' }).fill('shorter');
        await card.getByRole('button', { name: 'Cancel' }).click();

        await expect(card).toBeHidden();
        await expect(page.locator('[data-dim]')).toHaveCount(0);
        expect(posted).toHaveLength(0);
    });
});

test.describe('Writing a part yourself', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('starts from the part\'s text and sends the new text verbatim, as the page plus the part named in the text', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });
        const field = card.getByRole('textbox', { name: 'New text' });

        await expect(card.getByText('Text · content_2')).toBeVisible();
        await expect(field).toBeFocused();
        await expect(field).toHaveValue('Ab dem 1. Juli gilt');

        await field.fill('Ab dem 1. August\ngilt');
        await field.press('Control+Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_2" (Text), replace its text with: ' },
                { type: 'verbatim', text: 'Ab dem 1. August\ngilt', source: 'user', label: 'Text · content_2' },
            ],
        });
        await expect(card).toBeHidden();
    });

    test('sends nothing for an unchanged text and says so', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'New text' }).fill('Ab dem  1. Juli gilt ');
        await card.getByRole('button', { name: 'Apply' }).click();

        await expect(page.getByText('Nothing changed, so nothing was sent.')).toBeVisible();
        await expect(card).toBeHidden();
        expect(posted).toHaveLength(0);
    });

    test('cannot apply an empty text, and closes with its cancel button without sending', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'New text' }).fill(' ');
        await expect(card.getByRole('button', { name: 'Apply' })).toBeDisabled();

        await card.getByRole('button', { name: 'Cancel' }).click();

        await expect(card).toBeHidden();
        expect(posted).toHaveLength(0);
    });
});

test.describe('Swapping an image', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';
    // A 1×1 PNG, for the frame's image and the tiles.
    const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=', 'base64');

    // A page with an image part (a figure holding an image) above a text part.
    function imagePage(): string {
        return `<!doctype html><body style="margin:0;font:16px sans-serif">
            <div data-gcn-pageid="9142" data-gcn-tagid="10" data-gcn-tagname="content_1" data-gcn-i18n-constructname="Image" style="padding:20px">
                <figure style="margin:0"><img src="/hero.png" alt="Hero" style="width:300px;height:120px"></figure>
            </div>
            <p data-gcn-pageid="9142" data-gcn-tagid="12" data-gcn-tagname="content_2" data-gcn-i18n-constructname="Text">Ab dem 1. Juli gilt</p>
        </body>`;
    }

    // The CMS behind the image card: node 3 with root folder 7 and two images.
    async function stubCmsImages(page: Page) {
        await page.route((url) => url.pathname === '/hero.png' || url.pathname.startsWith('/rest/file/content/load/'), (route) => (
            route.fulfill({ contentType: 'image/png', body: PNG })
        ));
        await page.route((url) => url.pathname === '/rest/node', (route) => route.fulfill({ json: { items: [{ id: 3, name: 'Corporate Website', folderId: 7 }] } }));
        await page.route((url) => url.pathname === '/rest/folder/getItems/7', (route) => route.fulfill({
            json: { items: [{ id: 4411, name: 'team-workshop-2026.jpg', type: 'image' }, { id: 4412, name: 'editorial-desk.jpg', type: 'image' }] },
        }));
    }

    test('offers another image only for an image part, and sends the picked one as the page plus the part named in the text', async ({ page }) => {
        await stubS1Session(page, [imagePage()]);
        await stubCmsImages(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);

        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await expect(page.getByRole('menuitem', { name: 'Write it yourself' })).toBeVisible();
        await expect(page.getByRole('menuitem', { name: 'Swap the image' })).toHaveCount(0);
        await page.keyboard.press('Escape');

        await page.frameLocator(frameSelector).getByRole('img', { name: 'Hero' }).click();
        await expect(page.getByRole('menuitem', { name: 'Write it yourself' })).toHaveCount(0);
        await page.getByRole('menuitem', { name: 'Swap the image' }).click();

        const card = page.getByRole('dialog', { name: 'Image' });

        await expect(card.getByText('Image · content_1')).toBeVisible();
        await expect(card.getByRole('img', { name: 'team-workshop-2026.jpg' })).toHaveAttribute('src', '/rest/file/content/load/4411?nodeId=3');
        await expect(card.getByRole('button', { name: 'Use image' })).toBeDisabled();

        await card.getByRole('radio', { name: 'team-workshop-2026.jpg' }).click();
        await card.getByRole('button', { name: 'Use image' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_1" (Image), replace its image with: ' },
                { type: 'reference', ref: { type: 'image', id: '4411', node_id: 3, label: 'team-workshop-2026.jpg' } },
            ],
        });
        await expect(card).toBeHidden();
    });

    test('closes the image card with its cancel button, without sending', async ({ page }) => {
        await stubS1Session(page, [imagePage()]);
        await stubCmsImages(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('img', { name: 'Hero' }).click();
        await page.getByRole('menuitem', { name: 'Swap the image' }).click();

        const card = page.getByRole('dialog', { name: 'Image' });

        await card.getByRole('radio', { name: 'editorial-desk.jpg' }).click();
        await card.getByRole('button', { name: 'Cancel' }).click();

        await expect(card).toBeHidden();
        expect(posted).toHaveLength(0);
    });
});

test.describe('Page settings', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    // The CMS behind the page settings: page 9142 in folder 42, which links two templates.
    async function stubCmsPage(page: Page) {
        await page.route((url) => url.pathname === '/rest/page/load/9142', (route) => route.fulfill({
            json: {
                page: {
                    id: 9142, name: 'Neue Nutzungsbedingungen ab Q3', fileName: 'neue-nutzungsbedingungen.html', description: '', niceUrl: '/nutzungsbedingungen',
                    priority: 1, templateId: 17, folderId: 42, language: 'de', cdate: 1_710_244_800, edate: 1_790_000_000,
                },
            },
        }));
        await page.route((url) => url.pathname === '/rest/folder/getTemplates/42', (route) => route.fulfill({
            json: { templates: [{ id: 17, name: 'Kampagnen-Landingpage' }, { id: 21, name: 'Themenseite' }] },
        }));
    }

    test('drops the page settings over the preview and sends the changed ones as the page plus each new value', async ({ page }) => {
        await stubS1Session(page);
        await stubCmsPage(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        const button = page.getByRole('button', { name: 'Page settings' });

        await button.click();
        await expect(button).toHaveAttribute('aria-expanded', 'true');

        const sheet = page.getByRole('region', { name: 'Page settings' });
        const name = sheet.getByRole('textbox', { name: 'Name', exact: true });

        await expect(name).toHaveValue('Neue Nutzungsbedingungen ab Q3');
        await name.fill('Nutzungsbedingungen');

        const template = sheet.getByRole('combobox', { name: 'Template' });

        await expect(template).toHaveText('Kampagnen-Landingpage');
        await template.click();
        await page.getByRole('option', { name: 'Themenseite' }).click();
        await sheet.getByRole('button', { name: 'Apply' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – change its page settings:' },
                { type: 'text', text: '\nName: ' },
                { type: 'verbatim', text: 'Nutzungsbedingungen', source: 'user', label: 'Name' },
                { type: 'text', text: '\nTemplate: ' },
                { type: 'reference', ref: { type: 'template', id: '21', node_id: 3, label: 'Themenseite' } },
            ],
        });
        await expect(sheet).toBeHidden();
        await expect(button).toBeFocused();
    });

    test('closes with Escape, without sending; Escape in the open template list closes only the list', async ({ page }) => {
        await stubS1Session(page);
        await stubCmsPage(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.getByRole('button', { name: 'Page settings' }).click();

        const sheet = page.getByRole('region', { name: 'Page settings' });
        const template = sheet.getByRole('combobox', { name: 'Template' });

        await expect(template).toHaveText('Kampagnen-Landingpage');
        await template.click();
        await expect(page.getByRole('listbox')).toBeVisible();
        await page.keyboard.press('Escape');
        await expect(page.getByRole('listbox')).toBeHidden();
        await expect(sheet).toBeVisible();

        await sheet.getByRole('textbox', { name: 'Name', exact: true }).fill('Etwas anderes');
        await page.keyboard.press('Escape');

        await expect(sheet).toBeHidden();
        expect(posted).toHaveLength(0);
    });
});
