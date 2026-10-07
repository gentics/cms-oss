import { expect, type Locator, type Page, test } from '@playwright/test';

// Drops files onto `target` as the browser does: a DataTransfer with `Files`. `sizes` in bytes.
async function dropFiles(page: Page, target: Locator, files: { name: string; type: string; size: number }[]) {
    const dataTransfer = await page.evaluateHandle((list) => {
        const transfer = new DataTransfer();

        for (const { name, type, size } of list) {
            transfer.items.add(new File([new Uint8Array(size)], name, { type }));
        }

        return transfer;
    }, files);

    await target.dispatchEvent('dragover', { dataTransfer });
    await target.dispatchEvent('drop', { dataTransfer });
}

// Upload progress is not tested here: Chromium reports no `upload.onprogress` for a request
// intercepted with `page.route`, so a held upload shows none. It is covered by the unit tests
// (`src/test/stubUploads.ts`, `AttachmentList.test.tsx`).
test.describe('Composer attachments', () => {
    test('attaches files dropped onto the text field', async ({ page }) => {
        await page.goto('/');

        const field = page.getByRole('textbox', { name: 'What would you like to do?' });

        await dropFiles(page, field, [{ name: 'brief.pdf', type: 'application/pdf', size: 1000 }]);

        await expect(page.getByRole('button', { name: 'Remove: brief.pdf' })).toBeVisible();
        // The field kept no trace of the drop.
        await expect(field).toHaveText('');
    });

    test('does not attach a dropped file over the limit, and says why', async ({ page }) => {
        await page.goto('/');

        await dropFiles(page, page.getByRole('textbox', { name: 'What would you like to do?' }), [
            { name: 'huge.pdf', type: 'application/pdf', size: 26_214_401 },
        ]);

        await expect(page.getByText('huge.pdf was not attached')).toBeVisible();
        await expect(page.getByText('It is 25 MB; files can be up to 25 MB.')).toBeVisible();
        await expect(page.getByRole('button', { name: 'Remove: huge.pdf' })).toHaveCount(0);
    });
});
