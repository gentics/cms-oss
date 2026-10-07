import { expect, test } from '@playwright/test';

test.describe('App', () => {
    test('renders the dashboard at /', async ({ page }) => {
        await page.goto('/');

        await expect(page.getByRole('textbox', { name: 'What would you like to do?' })).toBeVisible();
    });

});

test.describe('Routing', () => {
    // For now the review route shows the session page.
    test('opens a session review directly by its URL', async ({ page }) => {
        await page.goto('#/sessions/abc/review');

        await expect(page.getByRole('textbox', { name: 'What should happen?' })).toBeVisible();
    });
});

test.describe('Theme', () => {
    test.use({ colorScheme: 'dark' });

    test('starts light, even when the system is dark', async ({ page }) => {
        await page.goto('/');

        await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

        const surface = await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--bg-surface').trim());

        expect(surface).toBe('#fff');
    });
});
