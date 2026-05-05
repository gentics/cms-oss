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
 * The Content Copilot is feature-flagged via a YAML file the customer drops
 * into `{ui-conf}/config/copilot.yml`. The CI image obviously does not ship
 * one, so every test here intercepts the request and serves a tailored YAML
 * (or a 404 when the test wants the disabled state).
 *
 * Crucial: the route MUST be installed BEFORE `navigateToApp`, because the
 * fetch is kicked off by `AppComponent.ngOnInit()` right after bootstrap.
 * That is why the navigation/login/select-node trio is wrapped in
 * `openEditorWithCopilot()` rather than living in a generic `beforeEach`
 * the way other suites (e.g. page-editing.spec.ts) handle it.
 */

const COPILOT_CONFIG_URL_PATTERN = /\/ui-conf\/config\/copilot\.yml/;

const YAML_DISABLED = 'enabled: false\nactions: []\n';
const YAML_ENABLED_NO_ACTIONS = 'enabled: true\nactions: []\n';
const YAML_ENABLED_WITH_ACTIONS = `enabled: true
actions:
    - id: summarize
      label: Zusammenfassen
      icon: summarize
      description: Eine kurze Zusammenfassung der Seite erstellen
    - id: rewrite-tone
      label: Tonalität anpassen
      icon: edit_note
`;

/** Stubs the customer copilot.yml endpoint for the lifetime of the page. */
async function stubCopilotConfig(page: Page, yaml: string | null): Promise<void> {
    await page.route(COPILOT_CONFIG_URL_PATTERN, (route) => {
        if (yaml === null) {
            return route.fulfill({ status: 404, body: 'Not Found' });
        }
        return route.fulfill({
            status: 200,
            contentType: 'text/yaml',
            body: yaml,
        });
    });
}

/** Navigation + login + node selection — all the steps that other suites do
 *  in `beforeEach`, but invoked AFTER the YAML stub so the Copilot bootstrap
 *  fetch sees the per-test response. */
async function openEditorWithCopilot(
    page: Page,
    importer: EntityImporter,
    yaml: string | null,
): Promise<void> {
    await stubCopilotConfig(page, yaml);
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

        test('button stays hidden when copilot.yml is missing (404)', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, null);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            // Wait for the toolbar to be present so we don't race against the
            // initial render — toBeHidden / toHaveCount(0) are the defining
            // assertions.
            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button stays hidden when copilot.yml has enabled: false', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_DISABLED);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button appears when copilot.yml has enabled: true and a page is in edit mode', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(copilotButton(page)).toBeVisible();
        });

        test('button stays hidden as long as no page is opened in edit mode', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            // Deliberately NOT opening a page — we should still be on the
            // folder list, where there is no editor toolbar at all.
            await expect(copilotButton(page)).toHaveCount(0);
        });

        test('button stays hidden when an invalid copilot.yml is served', async ({ page }) => {
            // Missing required `id` on the action — the parser falls back to
            // the disabled default, so the button must NOT appear.
            const invalid = 'enabled: true\nactions:\n    - label: incomplete\n';
            await openEditorWithCopilot(page, IMPORTER, invalid);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await expect(page.locator('content-frame gtx-editor-toolbar')).toBeVisible();
            await expect(copilotButton(page)).toHaveCount(0);
        });
    });

    test.describe('Sidebar interaction', () => {

        test.beforeEach(async ({ page }) => {
            // Default scenario for every interaction test: feature enabled,
            // no actions configured. Tests that need actions install their
            // own stub before this navigation by overriding via the helper.
        });

        test('clicking the toolbar button opens the sidebar', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            // Closed by default — verifying the precondition guards us against
            // a bug where the sidebar mounts in the open state.
            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);

            await copilotButton(page).click();

            await expect(copilotSidebar(page)).toHaveClass(/is-open/);
        });

        test('clicking the close icon collapses the sidebar again', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();
            await expect(copilotSidebar(page)).toHaveClass(/is-open/);

            await copilotSidebar(page).locator('[data-action="copilot-close"]').click();

            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);
        });

        test('clicking the toolbar button a second time toggles the sidebar closed', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();
            await expect(copilotSidebar(page)).toHaveClass(/is-open/);

            await copilotButton(page).click();

            await expect(copilotSidebar(page)).not.toHaveClass(/is-open/);
        });
    });

    test.describe('Sidebar contents', () => {

        test('shows the empty-state when actions: []', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            await expect(copilotEmptyState(page)).toBeVisible();
            await expect(copilotSidebar(page).locator('.copilot-action-item')).toHaveCount(0);
        });

        test('renders one card per configured action with id, label, icon, description', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_WITH_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            // Empty state must NOT be on the page once we have actions.
            await expect(copilotEmptyState(page)).toHaveCount(0);

            const items = copilotSidebar(page).locator('.copilot-action-item');
            await expect(items).toHaveCount(2);

            const summarize = copilotActionItem(page, 'summarize');
            await expect(summarize).toBeVisible();
            await expect(summarize.locator('.copilot-action-label')).toHaveText('Zusammenfassen');
            await expect(summarize.locator('.copilot-action-description'))
                .toHaveText('Eine kurze Zusammenfassung der Seite erstellen');

            const rewrite = copilotActionItem(page, 'rewrite-tone');
            await expect(rewrite).toBeVisible();
            await expect(rewrite.locator('.copilot-action-label')).toHaveText('Tonalität anpassen');
        });

        test('chat input is rendered but disabled in this UI iteration', async ({ page }) => {
            await openEditorWithCopilot(page, IMPORTER, YAML_ENABLED_NO_ACTIONS);
            await openPageForEditing(page, IMPORTER.get(PAGE_ONE) as CmsPage);

            await copilotButton(page).click();

            const textarea = copilotSidebar(page).locator('.copilot-chat-textarea');
            await expect(textarea).toBeVisible();
            await expect(textarea).toBeDisabled();
        });
    });
});
