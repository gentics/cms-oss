import { FileUploadResponse, NodeFeature } from '@gentics/cms-models';
import { EntityImporter, fullNode, loginWithForm, matchesPath, minimalNode, navigateToApp, TestSize } from '@gentics/e2e-utils';
import { expect, test } from '@playwright/test';
import { AUTH } from './common';
import { findItem, findList, openContext, selectNode } from './helpers';

test.describe('Asset Management', () => {
    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();

        // We need multiple nodes for the tests with the management setup

        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        await IMPORTER.bootstrapSuite(TestSize.FULL);

        await IMPORTER.setupFeatures(TestSize.MINIMAL, {
            [NodeFeature.ASSET_MANAGEMENT]: true,
        });
        await IMPORTER.setupFeatures(TestSize.FULL, {
            [NodeFeature.ASSET_MANAGEMENT]: true,
        });
    });

    test.beforeEach(async ({ page, request, context }) => {
        await context.clearCookies();
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
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
            await selectNode(page, IMPORTER.get(minimalNode)!.id);
            const list = findList(page, 'image');
            const picker = list.locator('[data-action-id="upload-item"] gtx-dropdown-list');
            const context = await openContext(picker);
            expect(context.locator('gtx-dropdown-item')).toHaveCount(2);
        });

        await test.step('open asset-management in full node', async () => {
            await selectNode(page, IMPORTER.get(fullNode)!.id);
            const list = findList(page, 'image');
            const picker = list.locator('[data-action-id="upload-item"] gtx-dropdown-list');
            const context = await openContext(picker);
            expect(context.locator('gtx-dropdown-item')).toHaveCount(2);
        });
    });

    test('should be able to use the custom asset manager', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18778',
        }],
    }, async ({ page }) => {
        await selectNode(page, IMPORTER.get(minimalNode)!.id);
        const list = findList(page, 'image');
        const picker = list.locator('[data-action-id="upload-item"] gtx-dropdown-list');
        const context = await openContext(picker);

        const saveReq = page.waitForResponse(data =>
            matchesPath(data.request().url(), '/rest/file/create') && data.request().method() === 'POST',
        );
        await context.locator('[data-provider-id="custom"] gtx-button button').click();
        const assetManager = page.frameLocator('external-assets-modal iframe');
        await assetManager.locator('button').click();

        const res = await saveReq;
        const body: FileUploadResponse = await res.json();

        await expect(findItem(list, body.file.id)).toBeAttached();
    });
});
