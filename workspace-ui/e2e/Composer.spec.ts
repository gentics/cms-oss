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

test.describe('Composer on the dashboard with a long text', () => {
    test.use({ viewport: { width: 1280, height: 800 } });

    // Types `count` lines, more than the composer shows, so its text scrolls.
    async function typeLines(page: Page, count: number) {
        const field = page.getByRole('textbox', { name: 'What would you like to do?' });

        await field.click();

        for (let line = 1; line <= count; line++) {
            await page.keyboard.type(`Line ${line} of a longer request for the new campaign page`);
            await page.keyboard.press('Shift+Enter');
        }
    }

    // The element that scrolls the composer's text, and where the send button is in its visible area.
    function scrollingOf(page: Page) {
        return page.getByRole('button', { name: 'Start a session' }).evaluate((send) => {
            let scroller: Element | null = document.querySelector('[role="textbox"]');

            while (scroller && !(scroller.scrollHeight > scroller.clientHeight + 1 && getComputedStyle(scroller).overflowY !== 'visible')) {
                scroller = scroller.parentElement;
            }

            const area = scroller?.getBoundingClientRect();
            const button = send.getBoundingClientRect();

            return {
                holdsSend: scroller?.contains(send) ?? false,
                gapBelowSend: area ? Math.round(area.bottom - button.bottom) : -1,
                // Room for the scrollbar at the area's right edge, so it does not run over the buttons.
                roomRightOfSend: area ? Math.round(area.right - button.right) : -1,
                scrollTop: scroller?.scrollTop ?? -1,
            };
        });
    }

    test('scrolls the text with the buttons beside it, the scrollbar at the edge, and keeps send and voice at the bottom', async ({ page }) => {
        await page.goto('/');
        await typeLines(page, 9);

        const atEnd = await scrollingOf(page);

        expect(atEnd.holdsSend).toBe(true);
        expect(atEnd.gapBelowSend).toBeLessThanOrEqual(8);
        expect(atEnd.roomRightOfSend).toBeGreaterThanOrEqual(8);

        // Scrolled up to the first line: send stays at the bottom.
        await page.getByRole('button', { name: 'Start a session' }).evaluate((send) => {
            let scroller: Element | null = send;

            while (scroller && !(scroller.scrollHeight > scroller.clientHeight + 1 && getComputedStyle(scroller).overflowY !== 'visible')) {
                scroller = scroller.parentElement;
            }

            scroller?.scrollTo({ top: 0 });
        });

        const atStart = await scrollingOf(page);

        expect(atStart.scrollTop).toBe(0);
        expect(atStart.gapBelowSend).toBeLessThanOrEqual(8);
    });
});
