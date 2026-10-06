import { expect, type Page, test } from '@playwright/test';

// Up to 640 px the chat composer puts voice first, like the dashboard (Composer.module.css). Whether
// the browser has speech recognition is set here, so the test does not depend on the Chromium build.
async function withSpeechRecognition(page: Page, available: boolean) {
    await page.addInitScript((isAvailable) => {
        const speechWindow = window as unknown as Record<string, unknown>;

        delete speechWindow.SpeechRecognition;
        delete speechWindow.webkitSpeechRecognition;

        if (isAvailable) {
            speechWindow.webkitSpeechRecognition = class {
                start() {}

                stop() {}

                abort() {}
            };
        }
    }, available);
}

test.describe('Session chat on a phone', () => {
    test.use({ viewport: { width: 390, height: 844 } });

    test('puts the big mic first and opens the text prompt on request', async ({ page }) => {
        await withSpeechRecognition(page, true);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(page.getByRole('button', { name: 'Tap to speak' })).toBeVisible();
        await expect(field).toBeHidden();
        await expect(page.getByRole('button', { name: 'File' })).toBeHidden();

        await page.getByRole('button', { name: 'Type instead' }).click();

        await expect(field).toBeVisible();
        await expect(field).toBeFocused();
        await expect(page.getByRole('button', { name: 'File' })).toBeVisible();
        await expect(page.getByRole('button', { name: 'Type instead' })).toBeHidden();
        await expect(page.getByRole('button', { name: 'Tap to speak' })).toBeVisible();
    });

    test('shows the text prompt without speech recognition', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        await expect(page.getByRole('textbox', { name: 'What should happen?' })).toBeVisible();
        await expect(page.getByRole('button', { name: 'Tap to speak' })).toHaveCount(0);
    });

    test('gives the chat the full width', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await expect(field).toBeVisible();

        const box = (await field.boundingBox())!;

        // The composer sits in the dock's 18 px padding, inside the 390 px viewport.
        expect(box.x).toBeLessThan(60);
        expect(box.x + box.width).toBeLessThanOrEqual(390);
    });

    test('shows no splitters and no side column on a phone', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        await expect(page.getByRole('textbox', { name: 'What should happen?' })).toBeVisible();
        await expect(page.getByRole('separator')).toHaveCount(0);
        await expect(page.getByRole('button', { name: 'Hide the left column' })).toHaveCount(0);
        await expect(page.getByRole('button', { name: 'Show the left column' })).toHaveCount(1);
    });

    test('swaps the chat for the left column with the buttons in the columns', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await page.getByRole('button', { name: 'Show the left column' }).click();

        await expect(field).toBeHidden();

        await page.getByRole('button', { name: 'Hide the left column' }).click();

        await expect(field).toBeVisible();
    });

    test('shows the chat of a session chosen in the left column', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.route((url) => url.pathname === '/genaix/api/v1/sessions', (route) => route.fulfill({
            json: { items: [{ id: 'def', title: 'Careers page', status: 'active', created_at: '2026-10-05T10:00:00Z', last_activity_at: '2026-10-05T10:00:00Z' }], next_cursor: null },
        }));
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });

        await page.getByRole('button', { name: 'Show the left column' }).click();
        await expect(field).toBeHidden();

        await page.getByRole('link', { name: /Careers page/ }).click();

        await expect(page).toHaveURL(/\/sessions\/def$/);
        await expect(field).toBeVisible();
    });
});

test.describe('Session chat on a wide screen', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('hides and shows the left column with the button at the far left of the topbar', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });
        const separator = page.getByRole('separator', { name: 'Width of the left column' });
        const brand = page.getByRole('link', { name: 'Gentics Workspace' });
        const hide = page.getByRole('button', { name: 'Hide the left column' });

        await expect(separator).toBeVisible();
        // The only one: none in the columns.
        await expect(hide).toHaveCount(1);

        // In the topbar, left of the brand.
        const hideBox = (await hide.boundingBox())!;
        const brandBox = (await brand.boundingBox())!;

        expect(hideBox.x).toBeLessThan(40);
        expect(hideBox.y + hideBox.height).toBeLessThanOrEqual(48);
        expect(hideBox.x + hideBox.width).toBeLessThanOrEqual(brandBox.x);

        await hide.click();

        await expect(separator).toBeHidden();
        await expect(field).toBeVisible();

        const show = page.getByRole('button', { name: 'Show the left column' });

        await expect(show).toHaveCount(1);
        expect((await show.boundingBox())!.x).toBe(hideBox.x);

        await show.click();

        await expect(separator).toBeVisible();
    });

    test('keeps mic and send in the bottom right corner of the text prompt', async ({ page }) => {
        await withSpeechRecognition(page, true);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });
        const file = page.getByRole('button', { name: 'File' });
        const mic = page.getByRole('button', { name: 'Dictate' });
        const send = page.getByRole('button', { name: 'Send' });

        await expect(field).toBeVisible();
        await expect(page.getByRole('button', { name: 'Tap to speak' })).toBeHidden();

        const [fieldBox, fileBox, micBox, sendBox] = await Promise.all([field.boundingBox(), file.boundingBox(), mic.boundingBox(), send.boundingBox()]);

        // Below the field, in the row of the actions: file on the left, then mic, then send at the right end.
        expect(micBox!.y).toBeGreaterThan(fieldBox!.y + fieldBox!.height - 1);
        expect(Math.abs(micBox!.y + micBox!.height / 2 - (fileBox!.y + fileBox!.height / 2))).toBeLessThan(4);
        expect(micBox!.x).toBeGreaterThan(fileBox!.x + fileBox!.width);
        expect(sendBox!.x).toBeGreaterThan(micBox!.x + micBox!.width - 1);
        expect(Math.abs(sendBox!.x + sendBox!.width - (fieldBox!.x + fieldBox!.width))).toBeLessThan(6);
    });
});
