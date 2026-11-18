import { expect, type Page, test } from '@playwright/test';

const SESSION = 's-1';
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64');
const IMAGES = ['team-workshop-2026.jpg', 'editorial-desk.jpg', 'summer-stage.jpg', 'office-entrance.jpg'];

/**
 * A session whose agent asks for a header image: a `form` question with an rc.6 reference field
 * (`format: context-reference`, `x-reference-types`), in node 3, whose root folder 7 holds `IMAGES`.
 */
async function stubImageQuestion(page: Page) {
    const interaction = {
        id: 'i-1',
        kind: 'form',
        prompt: 'Choose the header image for the new page.',
        schema: {
            type: 'object',
            required: ['headerImage'],
            properties: { headerImage: { type: 'object', format: 'context-reference', 'x-reference-types': ['image', 'session_file'], title: 'Header image' } },
        },
        multi: false,
        other_allowed: false,
        blocking: true,
        requested_at: new Date().toISOString(),
        expires_at: new Date(Date.now() + 600_000).toISOString(),
        status: 'pending',
        run_id: 'r-1',
    };
    const event = { seq: 1, ts: interaction.requested_at, session_id: SESSION, run_id: 'r-1', type: 'interaction.requested', interaction };

    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}`, (route) => route.fulfill({
        json: { id: SESSION, title: 'Summer event', status: 'waiting_for_input', active_run_id: 'r-1', context: { node_id: 3, references: [], guidelines: [], connection_ids: [] }, authorizations: [] },
    }));
    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/messages`, (route) => route.fulfill({ json: { items: [] } }));
    await page.route((url) => url.pathname === `/rest/proxy/genaix/sessions/${SESSION}/events`, (route) => {
        const after = Number(new URL(route.request().url()).searchParams.get('after') ?? 0);

        return route.fulfill({
            status: 200,
            contentType: 'text/event-stream',
            body: after < 1 ? `id: 1\nevent: interaction.requested\ndata: ${JSON.stringify(event)}\n\n` : '',
        });
    });
    await page.route((url) => url.pathname.startsWith('/rest/file/content/load/'), (route) => route.fulfill({ contentType: 'image/png', body: PNG }));
    await page.route((url) => url.pathname === '/rest/node', (route) => route.fulfill({ json: { items: [{ id: 3, name: 'Corporate Website', folderId: 7 }] } }));
    await page.route((url) => url.pathname === '/rest/folder/getItems/7', (route) => route.fulfill({
        json: { items: IMAGES.map((name, index) => ({ id: 4411 + index, name, type: 'image' })) },
    }));
}

test.describe('Image field of a question card', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('fills each column of the image grid with its tile, also in the wide question card', async ({ page }) => {
        await stubImageQuestion(page);
        await page.goto(`#/sessions/${SESSION}`);

        const tile = page.locator('label', { has: page.getByRole('img', { name: IMAGES[0] }) });

        await expect(tile).toBeVisible();

        const grid = tile.locator('..');
        const column = await grid.evaluate((element) => Number.parseFloat(getComputedStyle(element).gridTemplateColumns.split(' ')[0]!));
        const width = (await tile.boundingBox())!.width;

        // Wide enough that the gap would show: the card's columns are far wider than the old 128 px tiles.
        expect(column).toBeGreaterThan(150);
        expect(width).toBeCloseTo(column, 0);
    });
});
