import { expect, test } from '@playwright/test';

// The catalogue runs on its own dev server (`npm run catalogue`), see playwright.config.ts.
test.use({ baseURL: 'http://127.0.0.1:5174' });

for (const theme of ['Light', 'Dark'] as const) {
    test.describe(`Catalogue, ${theme.toLowerCase()} theme`, () => {
        test.beforeEach(async ({ page }) => {
            await page.goto('/');
            await page.getByRole('group', { name: 'Theme' }).getByRole('button', { name: theme }).click();
            await expect(page.locator('html')).toHaveAttribute('data-theme', theme.toLowerCase());
        });

        test('shows every component', async ({ page }) => {
            for (const name of ['Buttons', 'Checkbox', 'Select', 'Tabs', 'Tooltip', 'Menu', 'Dialog', 'Drawer', 'Toast']) {
                await expect(page.getByRole('heading', { level: 2, name })).toBeVisible();
            }
        });
    });
}

test('opens and closes the dialog with the keyboard only', async ({ page }) => {
    await page.goto('/');

    const trigger = page.getByRole('button', { name: 'Open dialog' });

    await trigger.focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('dialog', { name: 'Publish page' })).toBeVisible();

    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog')).toBeHidden();
    await expect(trigger).toBeFocused();
});

// design.md §8 "Gedrückt (Button)": a held-down button scales to `--press-scale`, a disabled one does not.
test('presses a button in while it is held down', async ({ page }) => {
    await page.goto('/');

    const edit = page.getByRole('button', { name: 'Edit' });

    await edit.hover();
    await page.mouse.down();
    await expect(edit).toHaveCSS('scale', /^0\.97( 0\.97)?$/);

    await page.mouse.up();
    await expect(edit).toHaveCSS('scale', 'none');

    const disabled = page.getByRole('button', { name: 'Add' }).and(page.locator('[data-disabled]'));

    await disabled.hover();
    await page.mouse.down();
    expect(await disabled.evaluate((element) => element.getAnimations().length)).toBe(0);
    await expect(disabled).toHaveCSS('scale', 'none');
    await page.mouse.up();
});

// design.md §11 "Overlays": toasts sit at the top right.
test('shows toasts at the top right', async ({ page }) => {
    await page.goto('/');
    await page.getByRole('button', { name: 'Show info' }).click();

    const toast = page.locator('[data-slot="toast"]');

    await expect(toast).toBeVisible();

    const box = await toast.boundingBox();
    const viewport = page.viewportSize();

    expect(box && viewport).toBeTruthy();
    expect(box!.y).toBeLessThanOrEqual(16);
    expect(viewport!.width - (box!.x + box!.width)).toBeLessThanOrEqual(16);
});

// Base UI shows at most 3 toasts (`limit`); an older one comes back when a newer one closes.
test('shows at most three toasts at once', async ({ page }) => {
    await page.goto('/');

    const showError = page.getByRole('button', { name: 'Show error' });
    const toasts = page.locator('[data-slot="toast"]');
    const limited = page.locator('[data-slot="toast"][data-limited]');

    // With the keyboard: the toasts cover the button.
    await showError.focus();

    for (let count = 0; count < 4; count++) {
        await page.keyboard.press('Enter');
    }

    await expect(toasts).toHaveCount(4);
    await expect(limited).toHaveCount(1);
    await expect(limited).toBeHidden();

    // Base UI exposes the close buttons once the toasts are expanded by hover or focus.
    const newest = page.locator('[data-slot="toast"]:not([data-limited])').first();

    await newest.hover();
    await newest.getByRole('button', { name: 'Dismiss' }).click();

    await expect(toasts).toHaveCount(3);
    await expect(limited).toHaveCount(0);
    for (const toast of await toasts.all()) {
        await expect(toast).toBeVisible();
    }
});
