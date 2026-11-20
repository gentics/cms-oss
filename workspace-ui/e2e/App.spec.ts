import { expect, test } from '@playwright/test';

test.describe('App', () => {
    test('renders the dashboard at /', async ({ page }) => {
        await page.goto('/');

        await expect(page.getByRole('textbox', { name: 'What would you like to do?' })).toBeVisible();
    });

});

test.describe('Routing', () => {
    test('opens a session review directly by its URL', async ({ page }) => {
        await page.goto('#/sessions/abc/review');

        await expect(page.getByRole('heading', { level: 1, name: 'Review & release' })).toBeVisible();
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

test.describe('Language', () => {
    test('takes the language of the CMS session, and a switch changes only the Workspace\'s language', async ({ page }) => {
        const requested: string[] = [];

        page.on('request', (request) => requested.push(new URL(request.url()).pathname));
        await page.route((url) => url.pathname === '/rest/i18n/get', (route) => route.fulfill({ json: { responseInfo: { responseCode: 'OK' }, code: 'de' } }));
        await page.goto('/');

        await expect(page.getByRole('textbox', { name: 'Was möchten Sie tun?' })).toBeVisible();
        await expect(page.locator('html')).toHaveAttribute('lang', 'de');

        await page.getByRole('button', { name: 'Zu English wechseln' }).click();

        await expect(page.getByRole('textbox', { name: 'What would you like to do?' })).toBeVisible();
        await expect(page.locator('html')).toHaveAttribute('lang', 'en');
        expect(requested).not.toContain('/rest/i18n/set');

        // A reload starts in the language of the CMS session again.
        await page.reload();
        await expect(page.getByRole('textbox', { name: 'Was möchten Sie tun?' })).toBeVisible();
    });

    test('is German before the CMS names a language', async ({ page }) => {
        await page.route((url) => url.pathname === '/rest/i18n/get', (route) => route.fulfill({ status: 500 }));
        await page.goto('/');

        await expect(page.getByRole('textbox', { name: 'Was möchten Sie tun?' })).toBeVisible();
        await expect(page.locator('html')).toHaveAttribute('lang', 'de');
    });
});
