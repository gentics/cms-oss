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
});

test.describe('Session chat on a wide screen', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

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
