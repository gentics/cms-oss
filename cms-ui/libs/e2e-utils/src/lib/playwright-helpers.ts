import { ResponseCode, UserDataResponse } from '@gentics/cms-models';
import { expect, Locator, Page, Request, Response } from '@playwright/test';
import { ATTR_CONTEXT_ID, ATTR_MULTIPLE, DEFAULT_KEYCLOAK_URL, ENV_KEYCLOAK_URL, LoginInformation } from './common';
import { hasMatchingParams, matchesPath } from './utils';

function isResponse(input: any): input is Response {
    return typeof (input as Response).request === 'function';
}

interface RequestMatchOptions {
    skipStatus?: boolean;
    params?: Record<string, string>;
}

export function matchRequest(method: string, path: string | RegExp, options?: RequestMatchOptions): (response: Response | Request) => boolean {
    return (input: Request | Response) => {
        let request: Request;
        // If we're matching against a request, we have to assume for now that it's valid
        let isOk = true;

        if (isResponse(input)) {
            request = input.request();
            isOk = input.ok();
        } else {
            request = input;
        }

        return (options?.skipStatus || isOk)
            && request.method() === method
            && matchesPath(request.url(), path)
            && (!options?.params || hasMatchingParams(request.url(), options.params));
    };
}

export function waitForKeycloakAuthPage(page: Page): Promise<void> {
    const kcUrl = process.env[ENV_KEYCLOAK_URL] || DEFAULT_KEYCLOAK_URL;
    const parsedUrl = new URL(kcUrl);

    return page.waitForURL(url =>
        url.host === parsedUrl.host
        && matchesPath(url, '/realms/*/protocol/openid-connect/auth'),
    );
}

export async function navigateToApp(page: Page, path: string = '', withSSO: boolean = false): Promise<void> {
    if (path.startsWith('/')) {
        path = path.substring(1);
    }

    const fullPath = `./${!withSSO ? '?skip-sso' : ''}#/${path}`;
    await page.goto(fullPath);
}

export async function loginWithForm(source: Page | Locator, loginData: LoginInformation): Promise<void> {
    await source.locator('gtx-input[formcontrolname="username"] input:not([disabled]), input[name="username"]')
        .first()
        .fill(loginData.username);
    await source.locator('gtx-input[formcontrolname="password"] input:not([disabled]), input[name="password"]')
        .first()
        .fill(loginData.password);
    await source.locator('button[type="submit"]:not([disabled]), input[type="submit"]:not([disabled])')
        .first()
        .click();
}

export function findContextContent(page: Page, id: string): Locator {
    return page.locator(`gtx-dropdown-content[${ATTR_CONTEXT_ID}="${id}"]`);
}

export async function openContext(element: Locator): Promise<Locator> {
    await element.waitFor({ state: 'visible' });
    await expect(element).toHaveAttribute(ATTR_CONTEXT_ID);

    const id = await element.getAttribute(ATTR_CONTEXT_ID);
    await element.locator('gtx-dropdown-trigger [data-context-trigger]').click();

    return findContextContent(element.page(), id);
}

export async function pickSelectValue(select: Locator, values: string | string[]): Promise<void> {
    const dropdown = select.locator('gtx-dropdown-list');

    let multi = false;
    if (Array.isArray(values)) {
        await expect(dropdown).toHaveAttribute(ATTR_MULTIPLE);
        multi = true;
    } else {
        values = [values];
    }

    const context = await openContext(dropdown);

    for (const val of values) {
        await context.locator(`.select-option[data-id="${val}"]`).click();
    }

    // If we have a multi-select, we need to close the dropdown by clicking the scroll mask
    if (multi) {
        const page = select.page();
        const scrollMask = page.locator('gtx-scroll-mask');
        // Click has to contain the position, as on default it'ld try to click into the center.
        // In the center there's however usually the content of the dropdown, which prevents the
        // scroll-mask to be actually clicked, which prevents it from actually closing.
        await scrollMask.click({ position: { x: 0, y: 0 } });
        await scrollMask.waitFor({ state: 'detached' });
    }
}

/**
 * Overrides the call for the user-data, to always have a clean setup,
 * without accidently loading some user-data which would alter the test setup.
 *
 * @param page Page reference to where the route will be redirected
 * @param dataProvider Optional provider which will get the data for the user.
 * @returns Promise from `page.route`.
 */
export function setupUserDataRerouting(page: Page, dataProvider?: () => any): Promise<void> {
    return page.route(url => matchesPath(url, '/rest/user/me/data'), (route, req) => {
        // Only re-route requests to load user-data
        if (req.method() !== 'GET') {
            return route.continue();
        }

        const data = typeof dataProvider === 'function'
            ? (dataProvider() || {})
            : {};

        const res: UserDataResponse = {
            responseInfo: {
                responseCode: ResponseCode.OK,
            },
            data,
        };

        return route.fulfill({
            status: 200,
            json: res,
        });
    });
}
