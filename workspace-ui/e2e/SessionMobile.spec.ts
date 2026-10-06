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
});

test.describe('Session chat on a wide screen', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    test('hides the left column from its top and shows it again from the top of the chat', async ({ page }) => {
        await withSpeechRecognition(page, false);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });
        const separator = page.getByRole('separator', { name: 'Width of the left column' });
        const hide = page.getByRole('button', { name: 'Hide the left column' });

        await expect(separator).toBeVisible();

        // At the top of the left column, left of its splitter.
        const hideBox = (await hide.boundingBox())!;
        const separatorBox = (await separator.boundingBox())!;

        expect(hideBox.x + hideBox.width).toBeLessThanOrEqual(separatorBox.x);
        expect(hideBox.y).toBeLessThan(120);

        await hide.click();

        await expect(separator).toBeHidden();

        // At the top left of the chat, which now starts at the window's edge.
        const show = page.getByRole('button', { name: 'Show the left column' });
        const showBox = (await show.boundingBox())!;

        expect(showBox.x).toBeLessThan(40);
        expect(showBox.y).toBeLessThan(120);
        await expect(field).toBeVisible();

        await show.click();

        await expect(separator).toBeVisible();
    });

    test('keeps the text prompt with the mic between the field and send', async ({ page }) => {
        await withSpeechRecognition(page, true);
        await page.goto('/sessions/abc');

        const field = page.getByRole('textbox', { name: 'What should happen?' });
        const mic = page.getByRole('button', { name: 'Dictate' });
        const send = page.getByRole('button', { name: 'Send' });

        await expect(field).toBeVisible();
        await expect(page.getByRole('button', { name: 'Tap to speak' })).toBeHidden();

        const [fieldBox, micBox, sendBox] = await Promise.all([field.boundingBox(), mic.boundingBox(), send.boundingBox()]);

        // One line: field, then mic, then send.
        expect(micBox!.x).toBeGreaterThan(fieldBox!.x + fieldBox!.width - 1);
        expect(sendBox!.x).toBeGreaterThan(micBox!.x + micBox!.width - 1);
        expect(Math.abs(micBox!.y + micBox!.height - (sendBox!.y + sendBox!.height))).toBeLessThan(6);
    });
});
