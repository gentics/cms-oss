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
