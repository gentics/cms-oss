import { expect, type Page, test } from '@playwright/test';

// The sessions in the left column, with GenAIx stubbed: the list, deleting (`DELETE /sessions/{id}`) and the open
// session, so that no GenAIx mock is needed.

// A prompt as users write them: several lines, a long URL, far more than a toast can show.
const PROMPT = `Please rework the landing page for the autumn campaign: new headline, three teasers for the products from
https://www.example.com/campaigns/autumn-2026/landing-page-with-a-very-long-address-that-does-not-break and a
new call to action at the end. Keep the tone of the existing pages, check the links and translate everything into
German and French afterwards, then send it to review.`;

async function stubSessions(page: Page) {
    await page.route((url) => url.pathname === '/rest/proxy/genaix/sessions', (route) => route.fulfill({
        json: { items: [{ id: 's-long', intent: PROMPT, status: 'active', created_at: '2026-10-05T10:00:00Z', last_activity_at: '2026-10-05T10:00:00Z' }], next_cursor: null },
    }));
    await page.route((url) => url.pathname === '/rest/proxy/genaix/sessions/s-long', (route) => (
        route.request().method() === 'DELETE' ? route.fulfill({ status: 204 }) : route.fulfill({ json: { id: 's-long', intent: PROMPT, status: 'active' } })
    ));
}

test.describe('Deleting a session', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('names a session with a long prompt in the toast on one line, the name cut off and "deleted" still shown', async ({ page }) => {
        await stubSessions(page);
        await page.goto('#/sessions/s-long');

        const row = page.getByRole('link', { name: /Please rework the landing page/ });

        await row.hover();
        await page.getByRole('button', { name: /^Delete “Please rework/ }).click();

        const toast = page.getByRole('region', { name: 'Notifications' }).locator('[data-slot="toast"]');
        // The name is a flex item of its own, so Chromium sets it off from the quotes with a space in the accessible name.
        const title = toast.getByRole('heading', { name: /^“\s?Please rework the landing page.*send it to review\.\s?” deleted$/ });

        await expect(title).toBeVisible();

        const titleBox = (await title.boundingBox())!;
        const toastBox = (await toast.boundingBox())!;
        const lineHeight = await title.evaluate((element) => parseFloat(getComputedStyle(element).lineHeight));

        expect(titleBox.height).toBeLessThanOrEqual(lineHeight + 1);

        // The name is cut off: its element is narrower than its text.
        const isNameCut = await title.locator('span span').evaluate((element) => element.scrollWidth > element.clientWidth);
        // "deleted", the text after the name, ends inside the title.
        const deletedRight = await title.evaluate((element) => {
            const range = document.createRange();
            const last = element.querySelector('span')!.lastChild!;

            range.selectNodeContents(last);

            return range.getBoundingClientRect().right;
        });

        expect(isNameCut).toBe(true);
        expect(deletedRight).toBeLessThanOrEqual(titleBox.x + titleBox.width + 1);
        expect(toastBox.x + toastBox.width).toBeLessThanOrEqual(1280);
        await expect(toast.getByRole('button', { name: 'Undo' })).toBeVisible();
    });
});
