import { Page as CmsPage } from '@gentics/cms-models';
import {
    EntityImporter,
    loginWithForm,
    navigateToApp,
    NODE_MINIMAL,
    PAGE_ONE,
    TestSize,
} from '@gentics/e2e-utils';
import { expect, Locator, Page, test } from '@playwright/test';
import { AUTH } from './common';
import { openPageForEditing, selectNode } from './helpers';

/*
 * The Content Copilot is feature-flagged via a JSON file the customer drops
 * into `{ui-conf}/copilot.json`. The CI image obviously does not ship one,
 * so every test here intercepts the request and serves a tailored payload
 * (or a 404 when the test wants the disabled state).
 *
 * Crucial: the route MUST be installed BEFORE `navigateToApp`, because the
 * fetch is kicked off by `AppComponent.ngOnInit()` right after bootstrap.
 * That is why the navigation/login/select-node trio is wrapped in
 * `openEditorWithCopilot()` rather than living in a generic `beforeEach`
 * the way other suites (e.g. page-editing.spec.ts) handle it.
 *
 * We deliberately do NOT assert on translated UI strings — UI language is
 * initially derived from the browser/system, so the actual text could be
 * either German or English. Tests assert against stable hooks instead:
 * `data-action`, `data-id`, structural classes.
 */

const COPILOT_CONFIG_URL_PATTERN = /\/ui-conf\/copilot\.json/;

const JSON_DISABLED = JSON.stringify({ enabled: false, actions: [] });
const JSON_ENABLED_NO_ACTIONS = JSON.stringify({ enabled: true, actions: [] });
const JSON_ENABLED_WITH_ACTIONS = JSON.stringify({
    enabled: true,
    actions: [
        {
            id: 'summarize',
            labelI18n: { de: 'Zusammenfassen', en: 'Summarise' },
            icon: 'lightbulb',
            descriptionI18n: { de: 'Eine kurze Zusammenfassung', en: 'A short summary' },
        },
        {
            id: 'rewrite-tone',
            labelI18n: { de: 'Tonalität anpassen', en: 'Adjust tone' },
            icon: 'edit_note',
        },
    ],
});

/** Stubs the customer copilot.json endpoint for the lifetime of the page. */
async function stubCopilotConfig(page: Page, body: string | null): Promise<void> {
    await page.route(COPILOT_CONFIG_URL_PATTERN, (route) => {
        if (body === null) {
            return route.fulfill({ status: 404, body: 'Not Found' });
        }
        return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body,
        });
    });
}

/** Navigation + login + node selection — all the steps that other suites do
 *  in `beforeEach`, but invoked AFTER the JSON stub so the Copilot bootstrap
 *  fetch sees the per-test response. */
async function openEditorWithCopilot(
    page: Page,
    importer: EntityImporter,
    body: string | null,
): Promise<void> {
    await stubCopilotConfig(page, body);
    await navigateToApp(page);
    await loginWithForm(page, AUTH.admin);
    await selectNode(page, importer.get(NODE_MINIMAL).id);
}

function copilotButton(page: Page): Locator {
    return page.locator('content-frame gtx-editor-toolbar [data-action="copilot"]');
}

function copilotSidebar(page: Page): Locator {
    return page.locator('gtx-copilot-sidebar .copilot-sidebar');
}

function copilotEmptyState(page: Page): Locator {
    return copilotSidebar(page).locator('.copilot-empty-state');
}

function copilotActionItem(page: Page, id: string): Locator {
    return copilotSidebar(page).locator(`.copilot-action-item[data-id="${id}"]`);
}

test.describe('Content Copilot', () => {

    const IMPORTER = new EntityImporter();

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

    test.beforeEach(async ({ request, context }) => {
        await test.step('Client Setup', async () => {
            IMPORTER.setApiContext(request);
            await context.clearCookies();
            await IMPORTER.clearClient();
        });

        await test.step('Common Test Setup', async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });
    });

    test.describe('Toolbar button visibility', () => {

        test('button stays hidden when copilot.json is missing (404)', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, null);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button stays hidden when copilot.json has enabled: false', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_DISABLED);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button appears when copilot.json has enabled: true and a page is in edit mode', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(copilotButton(page)).toBeVisible();
        });

        test('button stays hidden as long as no page is opened in edit mode', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            // Deliberately NOT opening a page — we should still be on the
            // folder list, where there is no editor toolbar at all.
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button stays hidden when invalid JSON is served', async ({ page }) => {
            // Missing required `id` on the action — the loader falls back to
            // the disabled default, so the button must NOT appear.
            const invalid = JSON.stringify({
                enabled: true,
                actions: [{ labelI18n: { en: 'incomplete' } }],
            });
            await openEditorWithCopilot(page, IMPORTER, invalid);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });
    });

    test.describe('Sidebar interaction', () => {

        test('clicking the toolbar button opens the sidebar', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            // Closed by default — verifying the precondition guards us against
            // a bug where the sidebar mounts in the open state.
            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);

            await copilotButton(page).click();

            await expect(copilotSidebar(page)).toHaveClass(/is-open/);
        });

        test('clicking the close icon collapses the sidebar again', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();
            await expect(copilotSidebar(page)).toHaveClass(/is-open/);

            await copilotSidebar(page).locator('[data-action="copilot-close"]').click();

            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);
        });

        test('clicking the toolbar button a second time toggles the sidebar closed', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();
            await expect(copilotSidebar(page)).toHaveClass(/is-open/);

            await copilotButton(page).click();

            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);
        });
    });

    test.describe('Sidebar contents', () => {

        test('shows the empty-state when actions: []', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            await expect(copilotEmptyState(page)).toBeVisible();
            await expect(copilotSidebar(page).locator('.copilot-action-item')).toHaveCount(0);
        });

        test('renders one card per configured action with correct id and label slot', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_WITH_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            // Empty state must NOT be on the page once we have actions.
            await expect(copilotEmptyState(page)).toHaveCount(0);

            const items = copilotSidebar(page).locator('.copilot-action-item');
            await expect(items).toHaveCount(2);

            // Identify cards by their stable `data-id` and assert the
            // label-container is rendered with non-empty content. We do
            // NOT assert on the literal text — the actual UI language
            // depends on the browser locale of the runner, which would
            // make tests flaky in CI.
            const summarize = copilotActionItem(page, 'summarize');
            await expect(summarize).toBeVisible();
            await expect(summarize.locator('.copilot-action-label')).not.toBeEmpty();
            await expect(summarize.locator('.copilot-action-description')).not.toBeEmpty();

            const rewrite = copilotActionItem(page, 'rewrite-tone');
            await expect(rewrite).toBeVisible();
            await expect(rewrite.locator('.copilot-action-label')).not.toBeEmpty();
            // No descriptionI18n on this one — the description span must
            // therefore not be rendered at all.
            await expect(rewrite.locator('.copilot-action-description')).toHaveCount(0);
        });

        test('chat input is rendered but disabled in this UI iteration', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, JSON_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            const textarea = copilotSidebar(page).locator('gtx-textarea.copilot-chat-textarea');
            await expect(textarea).toBeVisible();
            // gtx-textarea wraps a native <textarea> — the `disabled` attribute
            // ends up on that inner element, hence the descendant lookup.
            await expect(textarea.locator('textarea')).toBeDisabled();
        });
    });
});
