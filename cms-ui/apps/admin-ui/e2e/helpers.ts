import { Page } from '@playwright/test';

export async function navigateToApp(page: Page, path: string = '/', withSSO?: boolean): Promise<void> {
    const fullPath = `/admin/${!withSSO ? '?skip-sso' : ''}#/${path}`;
    await page.goto(fullPath);
}
