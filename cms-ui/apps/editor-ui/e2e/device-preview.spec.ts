import { Page as CmsPage } from '@gentics/cms-models';
import {
    EntityImporter,
    ITEM_TYPE_PAGE,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    openContext,
    PAGE_ONE,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import {
    editorAction,
    findItem,
    findList,
    itemAction,
    selectNode,
} from './helpers';

/**
 * E2E coverage for the responsive device-preview feature added next to the
 * BEARBEITEN button in the editor toolbar.
 *
 * The feature lets the user constrain the content-frame iframe to a fixed
 * viewport size (mobile / tablet / desktop) for responsive previews.
 * Implementation entry points:
 *   - `gtx-dropdown-list[data-action="device-preview"]` (toolbar trigger)
 *   - `[data-action="device-preview-{presetId}"]` (menu items)
 *   - `[data-action="device-preview-clear"]` (full-width entry)
 *   - `.frame-wrapper.device-preview-active` (active state on content-frame)
 */
test.describe('Device Preview', () => {

    const IMPORTER = new EntityImporter();

    /** CSS selector pointing to the toolbar's device-preview trigger. */
    const DEVICE_PREVIEW_TRIGGER =
        'content-frame gtx-editor-toolbar gtx-dropdown-list[data-action="device-preview"]';

    /** CSS selector pointing to the content-frame wrapper. */
    const FRAME_WRAPPER = 'content-frame .frame-wrapper';

    /** Built-in default presets that ship with the feature. */
    const PRESET_MOBILE = { id: 'mobile', width: 375, height: 667 };
    const PRESET_TABLET = { id: 'tablet', width: 768, height: 1024 };
    const PRESET_DESKTOP = { id: 'desktop', width: 1200, height: 840 };

    test.beforeAll(async ({ request }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await IMPORTER.clearClient();
        });

        await test.step('Test Bootstrapping', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });
    });

    test.beforeEach(async ({ page, request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        await test.step('Open Editor-UI', async () => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
            await selectNode(page, IMPORTER.get(NODE_MINIMAL).id);
        });
    });

    /* --------------------------------------------------------------------- *
     * Helpers
     * --------------------------------------------------------------------- */

    /**
     * Opens the page identified by `pageToOpen` in PREVIEW mode by clicking
     * its name in the page list. Resolves once the master-frame iframe is
     * fully loaded.
     */
    async function openPageInPreview(page: Page, pageToOpen: CmsPage): Promise<Locator> {
        const list = findList(page, ITEM_TYPE_PAGE);
        const row = findItem(list, pageToOpen.id);
        await row.locator('.item-primary .item-name-only').click();

        const iframe = page.locator('content-frame iframe.master-frame[loaded="true"]');
        await iframe.waitFor({ timeout: 60_000 });
        return iframe;
    }

    /**
     * Opens the device-preview dropdown and clicks the menu item with the
     * given preset id (or the "clear" item when `presetId === 'clear'`).
     */
    async function pickDevicePreset(page: Page, presetId: string): Promise<void> {
        const dropdown = await openContext(page.locator(DEVICE_PREVIEW_TRIGGER));
        const action = presetId === 'clear' ? 'device-preview-clear' : `device-preview-${presetId}`;
        await dropdown.locator(`[data-action="${action}"]`).click();
    }

    /**
     * Reads the live computed `--device-w` / `--device-h` CSS variables on the
     * `.frame-wrapper`. Returns `null` for either when not set (i.e. no
     * preset active).
     */
    async function readDeviceVariables(page: Page): Promise<{ width: number | null; height: number | null }> {
        return page.evaluate((sel) => {
            const el = document.querySelector(sel) as HTMLElement | null;
            if (!el) {
                return { width: null, height: null };
            }
            const styles = getComputedStyle(el);
            const parse = (v: string): number | null => {
                const trimmed = v.trim();
                if (!trimmed || trimmed === 'auto') {
                    return null;
                }
                const num = parseFloat(trimmed);
                return Number.isFinite(num) ? num : null;
            };
            return {
                width: parse(styles.getPropertyValue('--device-w')),
                height: parse(styles.getPropertyValue('--device-h')),
            };
        }, FRAME_WRAPPER);
    }

    /* --------------------------------------------------------------------- *
     * Toolbar visibility
     * --------------------------------------------------------------------- */

    test.describe('Toolbar', () => {

        test('renders the Vorschau button next to the toolbar primary actions', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            const trigger = page.locator(DEVICE_PREVIEW_TRIGGER);
            await expect(trigger).toBeVisible();
        });

        test('renders the Vorschau button while in EDIT mode as well', async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const row = findItem(list, IMPORTER.get(PAGE_ONE).id);
            await itemAction(row, 'edit');

            const iframe = page.locator('content-frame iframe.master-frame[loaded="true"]');
            await iframe.waitFor({ timeout: 60_000 });

            const trigger = page.locator(DEVICE_PREVIEW_TRIGGER);
            await expect(trigger).toBeVisible();
        });

        // The compare-mode visibility hide (Vorschau button suppressed when
        // editorState.compareWithId is set or editMode is one of the
        // COMPARE_VERSION_* values) is implemented in
        // EditorToolbarComponent.determineVisibleButtons() and should be
        // covered by either a Component-level unit test or the existing
        // language-compare e2e flows when those land. It is intentionally
        // not asserted here — driving the editor into compare-mode purely
        // from this spec would require multi-language test data plus a
        // brittle reach into Angular's injector from page.evaluate(),
        // which the rest of the test suite avoids.

        test('removes the legacy Vorschau entry from the kebab menu', async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const row = findItem(list, IMPORTER.get(PAGE_ONE).id);
            await itemAction(row, 'edit');

            const iframe = page.locator('content-frame iframe.master-frame[loaded="true"]');
            await iframe.waitFor({ timeout: 60_000 });

            // The kebab menu in the toolbar (data-context-id="item-editor")
            const kebab = page.locator(
                'content-frame gtx-editor-toolbar gtx-dropdown-list[data-context-id="item-editor"]',
            );
            const dropdown = await openContext(kebab);

            // The duplicate entry (label "Vorschau" → previewPage()) is gone.
            // The "Aktuelle Version" entry only shows when comparing versions, so it
            // must NOT be visible while editing the live page either.
            await expect(dropdown.locator('[data-action="preview"]')).toHaveCount(0);
            await expect(dropdown.locator('[data-action="preview-current-version"]')).toHaveCount(0);
        });
    });

    /* --------------------------------------------------------------------- *
     * Format selection
     * --------------------------------------------------------------------- */

    test.describe('Format selection', () => {

        test('constrains the iframe wrapper to mobile dimensions when Mobil is picked', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            await pickDevicePreset(page, PRESET_MOBILE.id);

            // device-preview-active class flipped on
            await expect(page.locator(FRAME_WRAPPER)).toHaveClass(/device-preview-active/);
            // data attribute reflects the active preset id
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_MOBILE.id);

            // CSS variables match the preset
            const vars = await readDeviceVariables(page);
            expect(vars.width).toBe(PRESET_MOBILE.width);
            expect(vars.height).toBe(PRESET_MOBILE.height);

            // The little "active preset" pill appears
            await expect(page.locator('content-frame .device-preview-info')).toBeVisible();
        });

        test('switches between presets without reloading the iframe', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            // Capture how many alohapage requests are issued during the
            // format switch — there should be none, because device-preview
            // is purely a CSS concern.
            let alohaPageRequests = 0;
            page.on('request', (req) => {
                if (req.url().includes('/alohapage')) {
                    alohaPageRequests++;
                }
            });

            await pickDevicePreset(page, PRESET_MOBILE.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_MOBILE.id);

            await pickDevicePreset(page, PRESET_TABLET.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_TABLET.id);

            await pickDevicePreset(page, PRESET_DESKTOP.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_DESKTOP.id);

            const vars = await readDeviceVariables(page);
            expect(vars.width).toBe(PRESET_DESKTOP.width);
            expect(vars.height).toBe(PRESET_DESKTOP.height);

            expect(alohaPageRequests).toBe(0);
        });

        test('clears the constraint when "Volle Breite" is picked', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            // Activate first
            await pickDevicePreset(page, PRESET_MOBILE.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveClass(/device-preview-active/);

            // Then clear
            await pickDevicePreset(page, 'clear');
            await expect(page.locator(FRAME_WRAPPER)).not.toHaveClass(/device-preview-active/);

            const vars = await readDeviceVariables(page);
            expect(vars.width).toBeNull();
            expect(vars.height).toBeNull();

            await expect(page.locator('content-frame .device-preview-info')).toHaveCount(0);
        });
    });

    /* --------------------------------------------------------------------- *
     * Mode synchronisation
     * --------------------------------------------------------------------- */

    test.describe('Mode synchronisation', () => {

        test('switches the editor from EDIT to PREVIEW when a preset is picked', async ({ page }) => {
            // Open in EDIT mode first
            const list = findList(page, ITEM_TYPE_PAGE);
            const row = findItem(list, IMPORTER.get(PAGE_ONE).id);
            await itemAction(row, 'edit');
            await page.locator('content-frame iframe.master-frame[loaded="true"]').waitFor({ timeout: 60_000 });

            // Picking a device preset should trigger a page load with the
            // PREVIEW (`real=newview`) variant of the alohapage URL.
            const previewLoad = page.waitForResponse((res) => {
                const url = res.url();
                return url.includes('/alohapage') && url.includes('real=newview');
            }, { timeout: 30_000 });

            await pickDevicePreset(page, PRESET_MOBILE.id);
            const response = await previewLoad;

            expect(response.ok()).toBe(true);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_MOBILE.id);
        });

        test('switches from EDIT to PREVIEW even when "Volle Breite" is picked', async ({ page }) => {
            const list = findList(page, ITEM_TYPE_PAGE);
            const row = findItem(list, IMPORTER.get(PAGE_ONE).id);
            await itemAction(row, 'edit');
            await page.locator('content-frame iframe.master-frame[loaded="true"]').waitFor({ timeout: 60_000 });

            const previewLoad = page.waitForResponse((res) => {
                const url = res.url();
                return url.includes('/alohapage') && url.includes('real=newview');
            }, { timeout: 30_000 });

            await pickDevicePreset(page, 'clear');
            await previewLoad;

            // No active preset, but we are now in preview mode — confirmed
            // by the alohapage request above.
            await expect(page.locator(FRAME_WRAPPER)).not.toHaveClass(/device-preview-active/);
        });

        test('auto-deactivates the active preset when leaving PREVIEW for EDIT', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            await pickDevicePreset(page, PRESET_MOBILE.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveClass(/device-preview-active/);

            // Click the regular "Bearbeiten" button — the existing
            // `editorAction` helper targets the edit button via its data-action.
            await editorAction(page, 'edit');

            // Wait for the edit-mode iframe to load
            await page.locator('content-frame iframe.master-frame[data-edit-mode="edit"][loaded="true"]')
                .waitFor({ timeout: 60_000 });

            await expect(page.locator(FRAME_WRAPPER)).not.toHaveClass(/device-preview-active/);
        });
    });

    /* --------------------------------------------------------------------- *
     * URL state
     * --------------------------------------------------------------------- */

    test.describe('URL state', () => {

        test('writes the active preset id to the URL as `?device=` query param', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            await pickDevicePreset(page, PRESET_TABLET.id);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_TABLET.id);

            // URL now carries `?device=tablet`
            await expect(page).toHaveURL(/[?&]device=tablet(&|$)/);
        });

        test('removes the query param when "Volle Breite" is picked', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            await pickDevicePreset(page, PRESET_MOBILE.id);
            await expect(page).toHaveURL(/[?&]device=mobile(&|$)/);

            await pickDevicePreset(page, 'clear');
            await expect(page).not.toHaveURL(/[?&]device=/);
        });

        test('restores the active preset when reloading a URL with `?device=`', async ({ page }) => {
            await openPageInPreview(page, IMPORTER.get(PAGE_ONE));

            await pickDevicePreset(page, PRESET_DESKTOP.id);
            const urlWithPreset = page.url();
            expect(urlWithPreset).toMatch(/[?&]device=desktop(&|$)/);

            // Hard-reload the URL to simulate the user opening a shared link.
            await page.goto(urlWithPreset);
            await page.locator('content-frame iframe.master-frame[loaded="true"]').waitFor({ timeout: 60_000 });

            await expect(page.locator(FRAME_WRAPPER)).toHaveClass(/device-preview-active/);
            await expect(page.locator(FRAME_WRAPPER)).toHaveAttribute('data-active-preset', PRESET_DESKTOP.id);
        });
    });
});
