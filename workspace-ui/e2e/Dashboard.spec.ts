import { expect, test } from '@playwright/test';

// Against the GenAIx mock (`npm run mock:genaix`, or the hand-out's docker compose stack, on :8123),
// which the Vite dev server proxies /genaix/api/v1 to. Playwright does not start the mock, so these
// tests are skipped for now. Remove the `skip` once the mock is part of the E2E run.
// eslint-disable-next-line playwright/no-skipped-test -- needs the GenAIx mock, see above
test.describe.skip('Dashboard', () => {
    test('starts a session from the prompt and opens its page', async ({ page }) => {
        await page.goto('/');

        const field = page.getByRole('textbox', { name: 'What would you like to do?' });

        await field.click();
        await field.pressSequentially('Which pages mention the terms of service?');
        await field.press('Enter');

        await expect(page).toHaveURL(/\/sessions\/[0-9a-f-]{36}$/);
        // The session page is, for now, the workspace; without a preview yet, it has no
        // left-column toggle.
        await expect(page.getByRole('separator', { name: 'Width of the left column' })).toBeVisible();
        await expect(page.getByRole('button', { name: /left column/ })).toHaveCount(0);
    });

    test('creates the session, uploads the file and posts the prompt', async ({ page }) => {
        const requests: string[] = [];

        page.on('request', (request) => {
            if (request.url().includes('/genaix/api/v1/sessions')) {
                requests.push(`${request.method()} ${new URL(request.url()).pathname.replace(/[0-9a-f-]{36}/, '{id}')}`);
            }
        });

        await page.goto('/');
        await page.getByLabel('File').setInputFiles({ name: 'brief.txt', mimeType: 'text/plain', buffer: Buffer.from('Terms of service, version 3') });
        await expect(page.getByText('brief.txt')).toBeVisible();

        const field = page.getByRole('textbox', { name: 'What would you like to do?' });

        await field.click();
        await field.pressSequentially('Summarise the brief');
        await page.getByRole('button', { name: 'Start a session' }).click();

        await expect(page).toHaveURL(/\/sessions\/[0-9a-f-]{36}$/);
        expect(requests).toEqual([
            'POST /genaix/api/v1/sessions',
            'POST /genaix/api/v1/sessions/{id}/files',
            'POST /genaix/api/v1/sessions/{id}/messages',
        ]);
    });
});
