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

// A page as the CMS renders it with tagmarkers=true: marked parts (a text inside a section) and
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

        await expect(page.getByText('Selected: Link · Text')).toBeVisible();
        // Still the page: a followed link would have replaced it with whatever /elsewhere serves.
        await expect(page.frameLocator(frameSelector).getByText('Ab dem')).toBeVisible();
        await expect(page.getByTitle('Preview of the page in the CMS')).toHaveJSProperty('contentWindow.location.pathname', '/alohapage');

        // The first Escape closes the element menu and returns to the page, the second clears the selection.
        await page.keyboard.press('Escape');
        await expect(page.getByRole('menu')).toBeHidden();
        await expect(page.getByText('Selected: Link · Text')).toBeVisible();
        await page.keyboard.press('Escape');
        await expect(page.getByText('Selected: Link · Text')).toBeHidden();
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

    test('closes the element menu and clears the selection with a click into the page outside the parts', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await expect(page.getByRole('menuitem', { name: 'Give feedback' })).toBeVisible();

        await page.frameLocator(frameSelector).getByText('Kopfzeile').click();

        await expect(page.getByRole('menu')).toBeHidden();
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

    test('closes the feedback card and clears the selection with a click into the page outside the parts, without sending', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Give feedback' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'What should happen to this spot?' }).fill('shorter');
        await page.frameLocator(frameSelector).getByText('Kopfzeile').click();

        await expect(card).toBeHidden();
        await expect(page.locator('[data-dim]')).toHaveCount(0);
        await expect(page.getByText('Selected: Text')).toBeHidden();
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

    test('closes the edit card and clears the selection with a click into the page outside the parts, without sending', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Ab dem').click();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();

        const card = page.getByRole('dialog', { name: 'Text' });

        await card.getByRole('textbox', { name: 'New text' }).fill('Ab dem 1. August gilt');
        await page.frameLocator(frameSelector).getByText('Kopfzeile').click();

        await expect(card).toBeHidden();
        await expect(page.getByText('Selected: Text')).toBeHidden();
        expect(posted).toHaveLength(0);
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

// A page as GenAIx creates it: the headline and the paragraphs in one Aloha text, i.e. in one marked part
// (`pagecontent`). `headline` sets the headline's text; `null` leaves it out, as if it had been removed.
function blockPage(headline: string | null = 'headline 1'): string {
    const heading = headline === null ? '' : `<h1>${headline}</h1>`;

    return `<!doctype html><body style="margin:0;font:16px sans-serif">
        <section class="docs-content">
            <div data-gcn-pageid="9142" data-gcn-tagid="932" data-gcn-tagname="pagecontent" data-gcn-i18n-constructname="Aloha Text (lang)" style="padding:30px">${heading}
<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.</p><p>Second paragraph</p></div>
        </section></body>`;
}

test.describe('Selecting a block of a part', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    test('outlines the block under the mouse, not the whole part, and names its kind', async ({ page }) => {
        await stubS1Session(page, [blockPage()]);
        // Also stubs the session, so that the page doesn't wait for a GenAIx mock that isn't running.
        await stubSending(page);
        await page.goto(`#/sessions/${SESSION}`);

        const headline = page.frameLocator(frameSelector).getByRole('heading', { name: 'headline 1' });

        await headline.hover();

        const label = page.getByText('Heading 1 · Aloha Text (lang)', { exact: true });

        await expect(label).toBeVisible();

        // The outline's box is the headline's box (the label sits at its top left), not the part's.
        const headlineBox = (await headline.boundingBox())!;
        const labelBox = (await label.boundingBox())!;

        expect(Math.abs(labelBox.x - headlineBox.x)).toBeLessThan(2);
        expect(Math.abs(labelBox.y - headlineBox.y)).toBeLessThan(2);

        await page.frameLocator(frameSelector).getByText('Lorem ipsum').hover();
        await expect(page.getByText('Paragraph · Aloha Text (lang)', { exact: true })).toBeVisible();
        await expect(label).toBeHidden();
    });

    test('writes the headline alone and sends it verbatim, the block named by its place, kind and text', async ({ page }) => {
        await stubS1Session(page, [blockPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('heading', { name: 'headline 1' }).click();
        await expect(page.getByText('Selected: Heading 1 · Aloha Text (lang)')).toBeVisible();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();

        const card = page.getByRole('dialog', { name: 'Heading 1 · Aloha Text (lang)' });
        const field = card.getByRole('textbox', { name: 'New text' });

        await expect(card.getByText('Aloha Text (lang) · pagecontent · Heading 1')).toBeVisible();
        await expect(field).toHaveValue('headline 1');

        await field.fill('A new headline');
        await field.press('Control+Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "pagecontent" (Aloha Text (lang)), block 1 of 3 (Heading 1 "headline 1"), replace its text with: ' },
                { type: 'verbatim', text: 'A new headline', source: 'user', label: 'Aloha Text (lang) · pagecontent · Heading 1' },
            ],
        });
    });

    test('references a paragraph in the chat as the block of its part', async ({ page }) => {
        await stubS1Session(page, [blockPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Second paragraph').click();
        await page.getByRole('menuitem', { name: 'Reference in the chat' }).click();

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(field.getByText('Aloha Text (lang) · pagecontent · Paragraph')).toBeVisible();

        await page.keyboard.type('make this shorter');
        await page.keyboard.press('Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "pagecontent" (Aloha Text (lang)), block 3 of 3 (Paragraph "Second paragraph"): make this shorter' },
            ],
        });
    });

    test('names the block by its text as it is after a reload', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page, [blockPage(), blockPage('headline 2')]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('heading', { name: 'headline 1' }).click();
        await expect(page.getByRole('menuitem', { name: 'Give feedback' })).toBeVisible();

        // The headline is rewritten while its menu is open: the selection stays, and what is sent names the new text.
        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(2);
        await expect(page.frameLocator(frameSelector).getByRole('heading', { name: 'headline 2' })).toBeVisible();
        await expect(page.getByText('Selected: Heading 1 · Aloha Text (lang)')).toBeVisible();

        await page.getByRole('menuitem', { name: 'Give feedback' }).click();
        await page.getByRole('dialog', { name: 'Heading 1 · Aloha Text (lang)' }).getByRole('textbox').fill('shorter');
        await page.getByRole('dialog', { name: 'Heading 1 · Aloha Text (lang)' }).getByRole('button', { name: 'Send' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "pagecontent" (Aloha Text (lang)), block 1 of 3 (Heading 1 "headline 2"): shorter' },
            ],
        });
    });

    test('selects a heading of a text that holds an image block, and the image block as a part of its own', async ({ page }) => {
        // As the CMS renders a new page: an image block between the paragraphs, with empty paragraphs around it.
        await stubS1Session(page, [`<!doctype html><body style="margin:0;font:16px sans-serif">
            <div data-gcn-pageid="9142" data-gcn-tagid="967" data-gcn-tagname="pagecontent" data-gcn-i18n-constructname="Aloha Text (lang)" style="padding:30px"><p>Die Gemeinde Bruchtal unterstützt Kleinbetriebe.</p><p></p><div data-gcn-pageid="9142" data-gcn-tagid="970" data-gcn-tagname="manimage_1" data-gcn-i18n-constructname="[Handbuch] Bild" class="aloha-block">
                <figure><img src="data:image/gif;base64,R0lGODlhAQABAAAAACw=" alt="Infografik" width="200" height="80"></figure>
            </div><p></p><h2>Wer kann einen Antrag stellen?</h2><ul><li>Betriebe mit Sitz in Bruchtal</li></ul></div></body>`]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('heading', { name: 'Wer kann einen Antrag stellen?' }).click();
        await expect(page.getByText('Selected: Heading 2 · Aloha Text (lang)')).toBeVisible();
        await page.getByRole('menuitem', { name: 'Write it yourself' }).click();
        await expect(page.getByRole('dialog', { name: 'Heading 2 · Aloha Text (lang)' }).getByRole('textbox', { name: 'New text' })).toHaveValue('Wer kann einen Antrag stellen?');
        await page.keyboard.press('Escape');

        await page.frameLocator(frameSelector).getByRole('img', { name: 'Infografik' }).click();
        await expect(page.getByText('Selected: [Handbuch] Bild')).toBeVisible();
        await expect(page.getByRole('menuitem', { name: 'Swap the image' })).toBeVisible();

        await page.getByRole('menuitem', { name: 'Reference in the chat' }).click();
        await page.keyboard.type('kleiner');
        await page.keyboard.press('Enter');

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "manimage_1" ([Handbuch] Bild): kleiner' },
            ],
        });
    });

    test('clears the selected block with a note when a reload no longer has it', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page, [blockPage(), blockPage(null)]);

        await stubSending(page);
        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('heading', { name: 'headline 1' }).click();
        await expect(page.getByText('Selected: Heading 1 · Aloha Text (lang)')).toBeVisible();

        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(2);
        await expect(page.getByText('The selected part is no longer on the page')).toBeVisible();
        await expect(page.getByText('Selected: Heading 1 · Aloha Text (lang)')).toBeHidden();
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

    test('uploads an image from the device into the session and sends it as the page, the part named in the text and the file', async ({ page }) => {
        const FILE_ID = '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02';

        await stubS1Session(page, [imagePage()]);
        await stubCmsImages(page);
        const posted = await stubSending(page);
        const uploads: string[] = [];

        await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/files`, (route) => {
            if (route.request().method() !== 'POST') {
                return route.fulfill({ json: { items: [], next_cursor: null } });
            }

            uploads.push(route.request().postDataBuffer()?.includes('hero-new.png') ? 'hero-new.png' : 'other');

            return route.fulfill({
                status: 201,
                json: {
                    id: FILE_ID, kind: 'upload', name: 'hero-new.png', media_type: 'image/png', size: PNG.length, mode: 'source',
                    sha256: '0'.repeat(64), created_at: '2026-10-16T10:00:00Z', created_by: 'user', download_url: `/api/v1/sessions/${SESSION}/files/${FILE_ID}/content`,
                },
            });
        });

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('img', { name: 'Hero' }).click();
        await page.getByRole('menuitem', { name: 'Swap the image' }).click();

        const card = page.getByRole('dialog', { name: 'Image' });

        await card.getByLabel('Upload image').setInputFiles({ name: 'hero-new.png', mimeType: 'image/png', buffer: PNG });
        await expect(card.getByRole('radio', { name: 'hero-new.png' })).toBeChecked();
        await card.getByRole('button', { name: 'Use image' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(uploads).toEqual(['hero-new.png']);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_1" (Image), replace its image with: ' },
                { type: 'file_ref', file_id: FILE_ID, mode: 'source' },
            ],
        });
        await expect(card).toBeHidden();
    });

    test('lays the upload tile and the image tiles out side by side, none over another, also when the grid scrolls', async ({ page }) => {
        await stubS1Session(page, [imagePage()]);
        await stubCmsImages(page);
        // The session itself, which is otherwise asked of the dev server's GenAIx proxy.
        await stubSending(page);
        // More images than the grid shows at once.
        await page.route((url) => url.pathname === '/rest/folder/getItems/7', (route) => route.fulfill({
            json: { items: Array.from({ length: 12 }, (_, index) => ({ id: 5000 + index, name: `image-${index}.jpg`, type: 'image' })) },
        }));

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('img', { name: 'Hero' }).click();
        await page.getByRole('menuitem', { name: 'Swap the image' }).click();

        const card = page.getByRole('dialog', { name: 'Image' });

        await card.getByLabel('Upload image').setInputFiles({ name: 'hero-new.png', mimeType: 'image/png', buffer: PNG });
        await expect(card.getByRole('img', { name: 'image-11.jpg' })).toBeAttached();

        // Every pair of tiles that share more than a pixel, as "a/b", and the tiles' heights.
        const layout = await card.locator('label:has(img), label:has(input[type="file"])').evaluateAll((elements) => {
            const boxes = elements.map((element) => element.getBoundingClientRect());
            const overlapping = boxes.flatMap((box, index) => boxes.slice(index + 1).flatMap((other, offset) => (
                box.left < other.right - 1 && other.left < box.right - 1 && box.top < other.bottom - 1 && other.top < box.bottom - 1
                    ? [`${index}/${index + 1 + offset}`]
                    : []
            )));

            return { count: boxes.length, overlapping, minHeight: Math.min(...boxes.map(({ height }) => height)) };
        });

        expect(layout.count).toBe(14);
        expect(layout.overlapping).toEqual([]);
        expect(layout.minHeight).toBeGreaterThan(20);
    });

    test('closes the image card and clears the selection with a click into the page outside the parts, without sending', async ({ page }) => {
        await stubS1Session(page, [imagePage()]);
        await stubCmsImages(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('img', { name: 'Hero' }).click();
        await page.getByRole('menuitem', { name: 'Swap the image' }).click();

        const card = page.getByRole('dialog', { name: 'Image' });

        await card.getByRole('radio', { name: 'editorial-desk.jpg' }).click();

        // The empty page below the parts.
        const frameBox = (await page.locator(frameSelector).boundingBox())!;

        await page.mouse.click(frameBox.x + 10, Math.min(frameBox.y + frameBox.height, 800) - 10);

        await expect(card).toBeHidden();
        await expect(page.getByText('Selected: Image')).toBeHidden();
        expect(posted).toHaveLength(0);
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

test.describe('Editing a link', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    // A text part with a link to a CMS page, as the CMS renders it for the preview (`DynamicUrlFactory`), and a link to another site.
    function linkPage(): string {
        return `<!doctype html><body style="margin:0;font:16px sans-serif">
            <p data-gcn-pageid="9142" data-gcn-tagid="12" data-gcn-tagname="content_2" data-gcn-i18n-constructname="Text" style="padding:30px">
                Es gelten die <a href="/alohapage?nodeid=3&amp;language=1&amp;real=newview&amp;realid=9150">AGB</a> und die <a href="https://example.com/regeln">Regeln</a>.
            </p></body>`;
    }

    // The CMS behind the link card: node 3 with root folder 7; the page AGB, which the link points to,
    // in folder 8 next to Impressum, the page the link is on (9142) in the root folder.
    async function stubCmsPages(page: Page) {
        await page.route((url) => url.pathname === '/rest/node', (route) => route.fulfill({ json: { items: [{ id: 3, name: 'Corporate Website', folderId: 7 }] } }));
        await page.route((url) => url.pathname === '/rest/page/load/9150', (route) => route.fulfill({
            json: { page: { id: 9150, name: 'AGB', folderId: 8, cdate: 0, edate: 0 } },
        }));
        await page.route((url) => url.pathname === '/rest/page/load/9142', (route) => route.fulfill({
            json: { page: { id: 9142, name: 'Neue Nutzungsbedingungen', folderId: 7, cdate: 0, edate: 0 } },
        }));
        await page.route((url) => url.pathname === '/rest/folder/getItems/8', (route) => route.fulfill({
            json: { items: [{ id: 9150, name: 'AGB', type: 'page' }, { id: 9151, name: 'Impressum', type: 'page' }] },
        }));
        await page.route((url) => url.pathname === '/rest/folder/getItems/7', (route) => route.fulfill({
            json: { items: [{ id: 9100, name: 'Willkommen', type: 'page' }] },
        }));
    }

    test('selects a link on its own and sends its new text and URL word for word, the link named by its place and text', async ({ page }) => {
        await stubS1Session(page, [markedPage()]);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);

        const link = page.frameLocator(frameSelector).getByRole('link', { name: '1. Juli' });

        await link.hover();

        // The outline is the link's, not the part's: its label sits at the link's top left.
        const label = page.getByText('Link · Text', { exact: true });

        await expect(label).toBeVisible();

        const linkBox = (await link.boundingBox())!;
        const labelBox = (await label.boundingBox())!;

        expect(Math.abs(labelBox.x - linkBox.x)).toBeLessThan(2);
        expect(Math.abs(labelBox.y - linkBox.y)).toBeLessThan(2);

        await link.click();
        await expect(page.getByText('Selected: Link · Text')).toBeVisible();
        await expect(page.getByRole('menuitem', { name: 'Write it yourself' })).toHaveCount(0);
        await page.getByRole('menuitem', { name: 'Edit link' }).click();

        const card = page.getByRole('dialog', { name: 'Link · Text' });
        const text = card.getByRole('textbox', { name: 'Link text' });

        await expect(card.getByText('Text · content_2 · Link')).toBeVisible();
        await expect(text).toBeFocused();
        await expect(text).toHaveValue('1. Juli');
        await expect(card.getByRole('radio', { name: 'URL' })).toBeChecked();
        await expect(card.getByRole('textbox', { name: 'URL' })).toHaveValue('/elsewhere');

        await text.fill('1. August');
        await card.getByRole('textbox', { name: 'URL' }).fill('https://example.com/agb');
        await card.getByRole('button', { name: 'Apply' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_2" (Text), link 1 of 1 ("1. Juli"), change it:' },
                { type: 'text', text: '\nLink text: ' },
                { type: 'verbatim', text: '1. August', source: 'user', label: 'Link text' },
                { type: 'text', text: '\nTarget: ' },
                { type: 'verbatim', text: 'https://example.com/agb', source: 'user', label: 'Target' },
            ],
        });
        await expect(card).toBeHidden();
    });

    test('shows the CMS page a link leads to and sends another page picked as its reference', async ({ page }) => {
        await stubS1Session(page, [linkPage()]);
        await stubCmsPages(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('link', { name: 'AGB' }).click();
        await page.getByRole('menuitem', { name: 'Edit link' }).click();

        const card = page.getByRole('dialog', { name: 'Link · Text' });

        await expect(card.getByRole('radio', { name: 'CMS page' })).toBeChecked();
        await expect(card.getByRole('radio', { name: 'AGB' })).toBeChecked();
        // The list is the folder of the page the link points to, not the node's root folder.
        await expect(card.getByRole('radio', { name: 'Impressum' })).toBeVisible();
        await expect(card.getByText('Willkommen')).toHaveCount(0);

        await card.getByRole('radio', { name: 'Impressum' }).click();
        await card.getByRole('button', { name: 'Apply' }).click();

        await expect.poll(() => posted.length).toBe(1);
        expect(posted[0]).toEqual({
            parts: [
                { type: 'reference', ref: { type: 'page', id: '9142', node_id: 3 } },
                { type: 'text', text: ' – part "content_2" (Text), link 1 of 2 ("AGB"), change it:' },
                { type: 'text', text: '\nTarget: ' },
                { type: 'reference', ref: { type: 'page', id: '9151', node_id: 3, label: 'Impressum' } },
            ],
        });
    });

    test('sends nothing for an unchanged link and says so', async ({ page }) => {
        await stubS1Session(page, [linkPage()]);
        await stubCmsPages(page);
        const posted = await stubSending(page);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByRole('link', { name: 'Regeln' }).click();
        await page.getByRole('menuitem', { name: 'Edit link' }).click();

        const card = page.getByRole('dialog', { name: 'Link · Text' });

        await expect(card.getByRole('textbox', { name: 'URL' })).toHaveValue('https://example.com/regeln');
        await card.getByRole('textbox', { name: 'Link text' }).fill(' Regeln ');
        await card.getByRole('button', { name: 'Apply' }).click();

        await expect(page.getByText('Nothing changed, so nothing was sent.')).toBeVisible();
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

test.describe('The preview header in a narrow column', () => {
    // Two pages, so the header has its page select too: the session wrote them one after the other.
    async function stubTwoPages(page: Page) {
        await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/events`, (route) => {
            const after = Number(new URL(route.request().url()).searchParams.get('after') ?? 0);
            const update = (seq: number, id: number, name: string) => `id: ${seq}\nevent: preview.updated\ndata: ${JSON.stringify({
                seq,
                ts: '2026-10-07T09:00:00Z',
                session_id: SESSION,
                type: 'preview.updated',
                cms_ref: { type: 'page', id, node_id: 3, name },
                preview_url: `http://127.0.0.1:5173/alohapage?realid=${id}&nodeid=3&mode=view`,
                mode: 'view',
                changed_elements: [],
            })}\n\n`;

            return route.fulfill({
                status: 200,
                contentType: 'text/event-stream',
                body: after === 0 ? update(1, 9142, 'Neue Nutzungsbedingungen ab Q3') + update(2, 9150, 'Allgemeine Geschäftsbedingungen') : '',
            });
        });
        await page.route((url) => url.pathname === '/alohapage', (route) => route.fulfill({ contentType: 'text/html', body: '<p>Seite</p>' }));
    }

    type Box = { x: number; y: number; width: number; height: number };

    // The names of the controls that overlap another one, as "first × second".
    function overlappingPairs(boxes: Record<string, Box>): string[] {
        const entries = Object.entries(boxes);

        return entries.flatMap(([name, box], i) => entries.slice(i + 1)
            .filter(([, other]) => box.x < other.x + other.width - 0.5 && other.x < box.x + box.width - 0.5
                && box.y < other.y + other.height - 0.5 && other.y < box.y + box.height - 0.5)
            .map(([otherName]) => `${name} × ${otherName}`));
    }

    // The names of the controls that stick out of the header on the left or the right.
    function outsideOf(header: Box, boxes: Record<string, Box>): string[] {
        return Object.entries(boxes)
            .filter(([, box]) => box.x < header.x - 0.5 || box.x + box.width > header.x + header.width + 0.5)
            .map(([name]) => name);
    }

    for (const width of [1100, 900]) {
        test(`keeps the title, the page select and the buttons apart and inside the header at ${width} px`, async ({ page }) => {
            await page.setViewportSize({ width, height: 800 });
            await stubTwoPages(page);
            await page.goto(`#/sessions/${SESSION}`);

            const region = page.getByRole('region', { name: 'Preview' });

            await expect(region.getByRole('combobox', { name: 'Pages' })).toBeVisible();

            const header = (await region.locator('header').first().boundingBox())!;
            const controls = {
                Title: region.getByRole('heading', { name: 'Preview' }),
                Pages: region.getByRole('combobox', { name: 'Pages' }),
                Open: region.getByRole('link', { name: 'Open' }),
                'Page settings': region.getByRole('button', { name: 'Page settings' }),
                Desktop: region.getByRole('button', { name: 'Desktop' }),
                Mobile: region.getByRole('button', { name: 'Mobile · 390 px' }),
                'Full screen': region.getByRole('button', { name: 'Full screen' }),
            };
            const boxes = Object.fromEntries(await Promise.all(Object.entries(controls).map(async ([name, control]) => [name, (await control.boundingBox())!] as const)));

            expect(overlappingPairs(boxes)).toEqual([]);
            expect(outsideOf(header, boxes)).toEqual([]);
        });
    }
});

test.describe('A tag with several editables in the preview', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    const frameSelector = 'iframe[title="Preview of the page in the CMS"]';

    // As the CMS renders a tag with two editable parts with tagmarkers=true: both carry the tag's markers,
    // each has the id of its own stored value.
    function twoEditablesPage(): string {
        return `<!doctype html><body style="margin:0;font:16px sans-serif">
            <div data-gcn-pageid="9142" data-gcn-tagid="20" data-gcn-tagname="teaser" data-gcn-i18n-constructname="Teaser" id="GENTICS_EDITABLE_101" style="padding:20px"><h2>Erste Überschrift</h2></div>
            <div data-gcn-pageid="9142" data-gcn-tagid="20" data-gcn-tagname="teaser" data-gcn-i18n-constructname="Teaser" id="GENTICS_EDITABLE_102" style="padding:20px"><p>Zweiter Teil, Absatz eins</p><p>Absatz zwei</p></div>
        </body>`;
    }

    test('selects a paragraph in the second editable and keeps it across a reload', async ({ page }) => {
        const { loadedUrls, sendNextUpdate } = await stubS1Session(page, [twoEditablesPage()]);

        await page.goto(`#/sessions/${SESSION}`);
        await page.frameLocator(frameSelector).getByText('Absatz zwei').click();

        await expect(page.getByText('Selected: Paragraph · Teaser')).toBeVisible();
        await expect(page.getByText('The selected part is no longer on the page')).toHaveCount(0);

        sendNextUpdate();
        await expect.poll(() => loadedUrls.length).toBe(2);
        await expect(page.frameLocator(frameSelector).getByText('Absatz zwei')).toBeVisible();
        await expect(page.getByText('Selected: Paragraph · Teaser')).toBeVisible();
        await expect(page.getByText('The selected part is no longer on the page')).toHaveCount(0);
    });

    test('tints every editable of a changed tag', async ({ page }) => {
        await stubS1Session(page, [twoEditablesPage()], [['teaser']]);
        await page.goto(`#/sessions/${SESSION}`);

        await expect(page.frameLocator(frameSelector).getByText('Absatz zwei')).toBeVisible();
        await expect(page.locator('[data-changed-part="teaser"]')).toHaveCount(2);
    });
});
