import { ResponseCode, UserDataResponse } from '@gentics/cms-models';
import { GCMSRestClient } from '@gentics/cms-rest-client';
import test, { expect, Locator, Page, Request, Response, Route } from '@playwright/test';
import {
    ATTR_CONTEXT_ID,
    ATTR_MULTIPLE,
    ButtonClickOptions,
    DEFAULT_E2E_KEYCLOAK_URL,
    LoginInformation,
    UserImportData,
} from './common';
import {
    ENV_E2E_APP_PATH,
    ENV_E2E_KEYCLOAK_URL,
} from './config';
import { hasMatchingParams, matchesPath } from './utils';

const VISIBLE_TOAST = 'gtx-toast .gtx-toast:not(.dismissing)';
const TOAST_CLOSE_BUTTON = '.gtx-toast-btn_close:not([hidden])';
const SIMPLE_TOAST = `${VISIBLE_TOAST} ${TOAST_CLOSE_BUTTON}`;
const ACTION_TOAST = `${VISIBLE_TOAST} .gtx-toast-btn_close[hidden] + .action > button`;

function isResponse(input: any): input is Response {
    return typeof (input as Response).request === 'function';
}

interface RequestMatchOptions {
    skipStatus?: boolean;
    params?: Record<string, string>;
}

type GenericObject<T> = T extends object ? T : never;
export function mockResponse<T>(data: GenericObject<T>, responseCode?: number): (route: Route) => Promise<void>;
export function mockResponse<T>(method: string, data: GenericObject<T>, responseCode?: number): (route: Route) => Promise<void>;
export function mockResponse<T>(
    methodOrData: string | T,
    dataOrRes?: T | number,
    responseCode?: number,
): (route: Route) => Promise<void> {
    let method: string;
    let data: T;

    if (typeof methodOrData === 'string') {
        method = methodOrData;
        data = dataOrRes as T;
    } else {
        data = methodOrData;
        if (typeof dataOrRes === 'number') {
            responseCode = dataOrRes;
        }
    }

    if (!responseCode) {
        responseCode = 200;
    }

    return (route) => {
        if (typeof method === 'string' && route.request().method() !== method) {
            return route.continue();
        }

        return route.fulfill({
            status: responseCode,
            json: data,
        });
    };
}

export function reroute(method: string, path: string): (route: Route) => Promise<void> {
    return async route => {
        const res = await route.fetch({
            method: method,
            url: path,
        });
        return route.fulfill({
            response: res,
        });
    };
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

export function onRequest(
    page: Page,
    matcher: (req: Request) => boolean,
    handler: (req: Request) => any,
): void {
    page.on('request', req => {
        if (matcher(req)) {
            handler(req);
        }
    });
}

/**
 * Simple wrapper function for `page.waitForResponse` and {@link matchRequest}, but with an error-handler
 * to tell which request actually failed, because otherwise you have to guess.
 *
 * @param page The playwright page object
 * @param method The method of the request
 * @param path The path of the request
 * @param options Options for request matching and timeout settings
 * @returns The Response object from the matched request.
 */
export function waitForResponseFrom(
    page: Page,
    method: string,
    path: string | RegExp,
    options?: RequestMatchOptions & { timeout?: number },
): Promise<Response> {
    const timeout = options?.timeout ?? 5_000;

    return page.waitForResponse(matchRequest(method, path, options), { timeout })
        .catch(err => {
            // The actual class isn't publicly available, which is why we have to do this hacky workaround.
            if (err instanceof Error && (err.constructor.name === 'TargetClosedError' || err.constructor.name === 'TimeoutError')) {
                const timeoutStr = timeout >= 1000 ? (timeout / 1000) + 's' : (timeout + 'ms');
                if (path instanceof RegExp) {
                    err.message = `Reached timeout (${timeoutStr}) for request "${method}" matching "${path.source}"`;
                } else {
                    err.message = `Reached timeout (${timeoutStr}) for request "${method} ${path}"`;
                }
            }

            throw err;
        });
}

export function waitForKeycloakAuthPage(page: Page): Promise<void> {
    const kcUrl = process.env[ENV_E2E_KEYCLOAK_URL] || DEFAULT_E2E_KEYCLOAK_URL;
    const parsedUrl = new URL(kcUrl);

    return page.waitForURL((url) =>
        url.host === parsedUrl.host
        && matchesPath(url, '/realms/*/protocol/openid-connect/auth'),
    );
}

export async function navigateToApp(page: Page, path: string = '', withSSO: boolean = false): Promise<void> {
    await test.step(`Navigating to "${path}"`, async () => {
        if (path.startsWith('/')) {
            path = path.substring(1);
        }

        let appPath = process.env[ENV_E2E_APP_PATH];
        if (appPath === '/') {
            appPath = '';
        }

        const fullPath = `${appPath}/${!withSSO ? '?skip-sso' : ''}#/${path}`;
        await page.goto(fullPath);
    });
}

export async function loginWithForm(source: Page | Locator, loginData: LoginInformation | UserImportData): Promise<void> {
    const username = (loginData as UserImportData).login ?? (loginData as LoginInformation).username;

    await test.step(`Logging in as "${username}"`, async () => {
        await source.locator('gtx-input[formcontrolname="username"] input:not([disabled]), input[name="username"]')
            .first()
            .fill(username);
        await source.locator('gtx-input[formcontrolname="password"] input:not([disabled]), input[name="password"]')
            .first()
            .fill(loginData.password);
        await source.locator('button[type="submit"]:not([disabled]), input[type="submit"]:not([disabled])')
            .first()
            .click();
    });
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

export async function pickSelectValue(select: Locator, values: string | number | (string | number)[]): Promise<void> {
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
 * @param page Page reference to where the route will be redirected
 * @param dataProvider Optional provider which will get the data for the user.
 * @returns Promise from `page.route`.
 */
export function setupUserDataRerouting(page: Page, dataProvider?: () => any): Promise<void> {
    return page.route((url) => matchesPath(url, '/rest/user/me/data'), (route, req) => {
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

export async function getSourceLocator(source: Page | Locator, nodeName: string): Promise<Locator> {
    if (
        typeof (source as Page).reload === 'function'
        || await (source as Locator).evaluate(
            (el, args) => el == null
              || typeof el !== 'object'
              || el.nodeName.toLowerCase() !== args.nodeName.toLowerCase(),
            { nodeName },
        )
    ) {
        return source.locator(nodeName);
    }

    return source as Locator;
}

/**
 * Clicks a modal button by its action
 */
export async function clickModalAction(source: Locator, action: string): Promise<void> {
    await source.locator(`.modal-footer [data-action="${action}"] button`).click();
}

export async function selectTab(source: Page | Locator, id: number | string): Promise<Locator> {
    const tabs = await getSourceLocator(source, 'gtx-tabs');
    await tabs.locator(`.tab-link[data-id="${id}"]`).click();
    return tabs.locator(`gtx-tab[data-id="${id}"]`);
}

export function findNotification(page: Page, id?: string): Locator {
    if (id) {
        return page.locator(`gtx-toast .gtx-toast[data-id="${id}"]`);
    } else {
        return page.locator('gtx-toast .gtx-toast');
    }
}

export function closeNotification(toast: Locator): Promise<void> {
    return toast.locator(TOAST_CLOSE_BUTTON).click();
}

export function clickNotificationAction(toast: Locator): Promise<void> {
    return toast.locator('.action button[data-action="primary"]').click();
}

export async function dismissNotifications(page: Page): Promise<void> {
    function getNotifications(): Promise<Locator[]> {
        return page.locator(`${SIMPLE_TOAST}, ${ACTION_TOAST}`).all();
    }

    let notifs = await getNotifications();
    while (notifs.length > 0) {
        for (const toast of notifs) {
            await toast.click();
        }
        await page.waitForTimeout(300);
        notifs = await getNotifications();
    }
}

export async function openSidebar(page: Page): Promise<Locator> {
    const sideMenu = page.locator('gtx-user-menu gtx-side-menu');
    const userMenuToggle = sideMenu.locator('gtx-user-menu-toggle');
    const userMenu = sideMenu.locator('.menu .menu-content');

    const isOpen = await userMenu.isVisible();
    if (!isOpen) {
        await userMenuToggle.click();
    }

    return userMenu;
}

export async function logout(page: Page): Promise<void> {
    const userMenu = await openSidebar(page);

    const req = page.waitForResponse(matchRequest('POST', '/rest/auth/logout/*'));
    const logoutButton = userMenu.locator('.user-details [data-action="logout"] button');
    await logoutButton.click();
    await req;

    await page.context().clearCookies();
}

export async function waitForPublishDone(page: Page, client: GCMSRestClient): Promise<void> {
    let info = await client.admin.getPublishInfo().send();
    while (info.totalWork !== info.totalWorkDone) {
        await page.waitForTimeout(1_000);
        info = await client.admin.getPublishInfo().send();
    }
}

export async function clickButton(source: Locator, options?: ButtonClickOptions): Promise<void> {
    const nodeType = await source.evaluate(el => el.nodeName);

    // For a simple button, simply click it without any other stuff
    if (nodeType === 'BUTTON') {
        await source.click(options);
        return;
    }

    const action = options?.action || 'primary';
    const btn = source.locator(`[data-action="${action}"]`);
    await btn.click(options);
}

export async function selectDateInPicker(source: Locator, date: Date): Promise<void> {
    const picker = await getSourceLocator(source, 'gtx-date-time-picker-controls');

    const content = picker.locator('.controls-content');
    const header = picker.locator('.controls-header');
    const previousMonth = content.locator('.rd-month .rd-back');
    const nextMonth = content.locator('.rd-month .rd-next');

    // Get the current state
    let year: number;
    let month: number;

    async function refreshState(): Promise<void> {
        year = parseInt(await content.getAttribute('data-value-year'), 10);
        month = parseInt(await content.getAttribute('data-value-month'), 10);
    }
    await refreshState();

    if (year !== date.getFullYear()) {
        const yearSelector = header.locator('.year-selector');

        if (await yearSelector.isVisible()) {
            await pickSelectValue(yearSelector, date.getFullYear());
        } else if (year > date.getFullYear()) {
            const diff = ((year - date.getFullYear() - 1) * 12) + month;
            for (let i = 0; i < diff; i++) {
                await previousMonth.click();
            }
            await refreshState();
        } else {
            const diff = ((date.getFullYear() - year) * 12) - month;
            for (let i = 0; i < diff; i++) {
                await nextMonth.click();
            }
            await refreshState();
        }
    }

    if (month > (date.getMonth() - 1)) {
        const diff = month - date.getMonth() - 1;
        for (let i = 0; i < diff; i++) {
            await previousMonth.click();
        }
    } else if (month < (date.getMonth() - 1)) {
        const diff = date.getMonth() - 1 - month;
        for (let i = 0; i < diff; i++) {
            await nextMonth.click();
        }
    }

    const timeSelect = content.locator('.time-picker');
    if (await timeSelect.isVisible()) {
        await timeSelect.locator('.hours gtx-input input').fill(`${date.getHours()}`);
        await timeSelect.locator('.minutes gtx-input input').fill(`${date.getMinutes()}`);

        const seconds = timeSelect.locator('.seconds');
        if (await seconds.isVisible()) {
            await seconds.locator('gtx-input input').fill(`${date.getSeconds()}`);
        }
    }

    let day = `${date.getDate()}`;
    if (day.length === 1) {
        day = `0${day}`;
    }
    await content.locator('.rd-days .rd-days-body .rd-day-body:not(.rd-day-prev-month):not(.rd-day-next-month):not(.rd-day-disabled)').filter({
        hasText: day,
    }).click();
}

export async function pickDate(source: Locator, date?: Date): Promise<void> {
    const dateTimePicker = await getSourceLocator(source, 'gtx-date-time-picker');
    const input = dateTimePicker.locator('gtx-input');
    await input.click();

    const modal = source.page().locator('gtx-date-time-picker-modal');

    // If we have a date, try to select it
    if (date) {
        await selectDateInPicker(modal, date);
    }

    await clickModalAction(modal, 'confirm');
}
