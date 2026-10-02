import { expect, test } from '@playwright/test';

test.describe('App', () => {
    test('renders the application', async ({ page }) => {
        await page.goto('/');

        await expect(page.getByRole('heading', { name: 'Get started' })).toBeVisible();

        await expect(page.getByRole('button', { name: 'Count is 0' })).toBeVisible();
    });

    test('increments the counter', async ({ page }) => {
        await page.goto('/');

        const counter = page.getByRole('button', { name: 'Count is 0' });

        await counter.click();

        await expect(page.getByRole('button', { name: 'Count is 1' })).toBeVisible();
    });
});

test.describe('Routing', () => {
    // For now the session routes show the home page.
    test('opens a session review directly by its URL', async ({ page }) => {
        await page.goto('/sessions/abc/review');

        await expect(page.getByRole('heading', { name: 'Get started' })).toBeVisible();
    });
});

test.describe('Theme', () => {
    test.use({ colorScheme: 'dark' });

    test('starts light, even when the system is dark', async ({ page }) => {
        await page.goto('/');

        await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

        const surface = await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--surface').trim());

        expect(surface).toBe('#fff');
    });
});
