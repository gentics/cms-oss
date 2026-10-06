import { AccessControlledType, GcmsPermission } from '@gentics/cms-models';
import { clickModalAction, findNotification, matchesPath, waitForResponseFrom } from '@gentics/e2e-utils';
import { expect, Locator, Page } from '@playwright/test';

export async function navigateToModule(page: Page, moduleId: string): Promise<Locator> {
    // Click the module item
    const moduleItem = page.locator(`gtx-dashboard-item[data-id="${moduleId}"] > .item:not(.disabled)`).locator('..');
    await moduleItem.click();

    // Wait for the module content to be visible
    const splitOutlet = page.locator('gtx-generic-router-outlet > gtx-split-view-router-outlet .master-route-wrapper > *:not(router-outlet)');
    const genericOutlet = page.locator('gtx-generic-router-outlet > gtx-generic-router-outlet > *:not(router-outlet)');

    const module = splitOutlet.or(genericOutlet);

    await module.waitFor({ state: 'visible' });

    return module;
}

/**
 * Logs out from the mesh management interface
 */
export async function logoutMeshManagement(page: Page): Promise<void> {
    const req = page.waitForResponse(response =>
        response.ok() && matchesPath(response.url(), '/rest/contentrepositories/*/proxy/api/v2/auth/logout'),
    );
    await page.locator('.management-container .logout-button').click();
    await req;
}

/**
 * Clicks the CR login button and waits for management to be visible
 */
export async function loginWithCR(page: Page, shouldBeLoggedIn: boolean = true): Promise<void> {
    await page.locator('.cr-login-button').click();

    if (shouldBeLoggedIn) {
        await page.locator('.management-container').waitFor({ state: 'visible' });
    }
}

export function findEntityTableActionButton(source: Page | Locator, action: string): Locator {
    return source.locator(`.entity-table-actions-bar .table-action-button[data-action="${action}"] button`);
}

export interface PermissionChanges {
    /** The permissions which should be changed, and to which value. */
    perms: Partial<Record<GcmsPermission, boolean>>;
    /** If the "Apply to sub-objects/sub-folders" option should be checked. */
    subObjects?: boolean;
    /** If the "Apply to sub-groups" option should be checked. */
    subGroups?: boolean;
}

/**
 * Finds the rendered permission icon of the specified permission type in a permissions/group trable row.
 * The icon has a `data-value` attribute, which reflects if the permission is granted (`"true"`) or not (`"false"`).
 */
export function findPermissionIcon(row: Locator, perm: GcmsPermission): Locator {
    return row.locator(`gtx-permission-icon .permission-state[data-type="${perm}"]`);
}

/**
 * Asserts that the permission icons of the row reflect the expected values.
 */
export async function expectRowPermissions(row: Locator, perms: Partial<Record<GcmsPermission, boolean>>): Promise<void> {
    // Wait for any (re-)loading of the row to be done first
    await expect(row).not.toContainClass('loading');
    for (const [perm, value] of Object.entries(perms)) {
        await expect(findPermissionIcon(row, perm as GcmsPermission)).toHaveAttribute('data-value', `${value}`);
    }
}

async function setCheckbox(checkbox: Locator, value: boolean): Promise<void> {
    const input = checkbox.locator('input[type="checkbox"]');
    if ((await input.isChecked()) !== value) {
        await checkbox.locator('label').click();
    }
    await expect(input).toBeChecked({ checked: value });
}

/**
 * Opens the edit-permissions modal by clicking the row, applies the changes and saves them.
 * @param page The page.
 * @param row The trable row of which the permissions should be edited.
 * @param nameColumn The ID of the column which contains the name of the row element.
 * @param changes The changes to apply in the modal.
 * @param request The request path that is expected to be sent when saving.
 */
export async function editRowPermissions(
    page: Page,
    row: Locator,
    nameColumn: string,
    changes: PermissionChanges,
    request: string,
): Promise<void> {
    await row.locator(`.data-column[data-id="${nameColumn}"] .name`).click();

    const modal = page.locator('gtx-edit-permissions-modal');
    await expect(modal).toBeVisible();

    if (changes.subObjects != null) {
        await setCheckbox(modal.locator('gtx-checkbox[data-id="apply-to-sub-objects"]'), changes.subObjects);
    }
    if (changes.subGroups != null) {
        await setCheckbox(modal.locator('gtx-checkbox[data-id="apply-to-sub-groups"]'), changes.subGroups);
    }
    for (const [perm, value] of Object.entries(changes.perms)) {
        await setCheckbox(modal.locator(`gtx-checkbox.permission-checkbox[data-id="${perm}"]`), value);
    }

    const req = waitForResponseFrom(page, 'POST', request);
    await clickModalAction(modal, 'confirm');
    const res = await req;
    expect(res.ok()).toBe(true);

    await expect(modal).toBeHidden();
}

/**
 * Asserts that the success notification with the specified ID is displayed, and that no other notification
 * which isn't a success notification (i.E. an error message) is being displayed.
 */
export async function expectOnlySuccessNotification(page: Page, id: string): Promise<void> {
    const toast = findNotification(page, id);
    await expect(toast).toBeVisible();
    await expect(toast).toContainClass('success');
    await expect(page.locator('gtx-toast .gtx-toast:not(.success)')).toHaveCount(0);
}

export function groupTypePermissionNotificationId(groupId: number): string {
    return `group.type-permission-change.${groupId}`;
}

export function groupInstancePermissionNotificationId(groupId: number): string {
    return `group.instance-permission-change.${groupId}`;
}

export function groupTypePermissionPath(groupId: number, type: AccessControlledType): string {
    return `/rest/group/${groupId}/perms/${type}`;
}

export function groupInstancePermissionPath(groupId: number, type: AccessControlledType, instanceId: number): string {
    return `/rest/group/${groupId}/perms/${type}/${instanceId}`;
}
