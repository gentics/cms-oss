import { FileUploadResponse, NodeFeature } from '@gentics/cms-models';
import { EntityImporter, NODE_FULL, loginWithForm, matchesPath, NODE_MINIMAL, navigateToApp, openContext, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, selectNode } from './helpers';

test.describe('Asset Management', () => {
    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();

        // We need multiple nodes for the tests with the management setup

        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        await IMPORTER.bootstrapSuite(TestSize.FULL);
    });

    test.beforeEach(async ({ request, context }) => {
        IMPORTER.setApiContext(request);
        await context.clearCookies();
        await IMPORTER.clearClient();

        await IMPORTER.cleanupTest();

        await IMPORTER.setupTest(TestSize.MINIMAL);
        await IMPORTER.setupFeatures(TestSize.MINIMAL, {
            [NodeFeature.ASSET_MANAGEMENT]: true,
        });
    });

    test.describe('Multiple Nodes required', () => {
        test.beforeEach(async ({ page }) => {
            await IMPORTER.setupTest(TestSize.FULL);
            await IMPORTER.setupFeatures(TestSize.FULL, {
                [NodeFeature.ASSET_MANAGEMENT]: true,
            });

            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
        });

        test('should have asset-management available in both nodes', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18778',
            }],
        }, async ({ page }) => {
            await test.step('open asset-management in minimal node', async () => {
                await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
                const list = findList(page, 'image');
                const picker = list.locator('[data-action="upload-item"] gtx-dropdown-list');
                const context = await openContext(picker);
                expect(context.locator('gtx-dropdown-item')).toHaveCount(2);

                // Close the dropdown
                await page.click('gtx-scroll-mask');
            });

            await test.step('open asset-management in full node', async () => {
                await selectNode(page, IMPORTER.get(NODE_FULL)!.id);
                const list = findList(page, 'image');
                const picker = list.locator('[data-action="upload-item"] gtx-dropdown-list');
                const context = await openContext(picker);
                expect(context.locator('gtx-dropdown-item')).toHaveCount(2);

                // Close the dropdown
                await page.click('gtx-scroll-mask');
            });
        });
    });

    test.describe('Single Node', () => {
        test.beforeEach(async ({ page }) => {
            await navigateToApp(page);
            await loginWithForm(page, AUTH.admin);
        });

        test('should be able to use the custom asset manager', {
            annotation: [{
                type: 'ticket',
                description: 'SUP-18778',
            }],
        }, async ({ page }) => {
            await selectNode(page, IMPORTER.get(NODE_MINIMAL)!.id);
            const list = findList(page, 'image');
            const picker = list.locator('[data-action="upload-item"] gtx-dropdown-list');
            const context = await openContext(picker);

            const saveReq = page.waitForResponse(data =>
                data.request().method() === 'POST' && matchesPath(data.request().url(), '/rest/file/create'),
            );
            const loadReq = page.waitForResponse(data =>
                data.request().method() === 'GET' && matchesPath(data.request().url(), '/rest/image/load/*'),
            );
            await context.locator('[data-provider-id="custom"] gtx-button button').click();
            const assetManager = page.frameLocator('external-assets-modal iframe');
            await assetManager.locator('button').click();

            const res = await saveReq;
            const body: FileUploadResponse = await res.json();

            await loadReq;

            await findItem(list, body.file.id).waitFor();
        });
    });
});
