import { LoginResponse, User } from '@gentics/cms-models';
import { createClient, EntityImporter, loginWithForm, matchRequest, navigateToApp, pickSelectValue, selectTab, TestSize } from '@gentics/e2e-utils';
import { randomId } from '@gentics/ui-core/utils/random-id';
import { Locator, test } from '@playwright/test';
import { AUTH } from './common';

test.describe('Messages', () => {

    const IMPORTER = new EntityImporter();

    test.beforeAll(async ({ request }) => {
        IMPORTER.setApiContext(request);
        await IMPORTER.clearClient();
        await IMPORTER.cleanupTest();

        // We need multiple nodes for the tests with the management setup

        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    test.beforeEach(async ({ request, context }) => {
        IMPORTER.setApiContext(request);
        await context.clearCookies();
        await IMPORTER.clearClient();

        await IMPORTER.cleanupTest();
        await IMPORTER.setupTest(TestSize.MINIMAL);
    });

    test('should be able to send, receive, and delete messages', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18935',
        }],
    }, async ({ page }) => {
        // Mark as slow, since we have to wait for a poll cycle which may take longer than 30sec
        test.slow();

        const MESSAGE_CONTENT = `Hello #${randomId()}!`;
        let login: LoginResponse;
        let messagesTab: Locator;
        let receivedMessage: Locator;

        await test.step('Login', async () => {
            await navigateToApp(page);
            const loginReq = page.waitForResponse(matchRequest('POST', '/rest/auth/login'));
            await loginWithForm(page, AUTH.admin);
            login = await (await loginReq).json();
        });

        await test.step('Clear all messages', async () => {
            const client = await createClient({
                context: page.request,
                connection: {
                    absolute: false,
                    basePath: '/rest',
                },
            });
            client.sid = login.sid;
            const res = await client.message.list().send();
            for (const msg of res.messages) {
                await client.message.delete(msg.id).send();
            }
        });

        await test.step('Open new message modal', async () => {
            const userMenuToggle = page.locator('gtx-user-menu gtx-side-menu gtx-side-menu-toggle');
            const userMenu = page.locator('gtx-user-menu gtx-side-menu .menu .menu-content');

            await userMenuToggle.click();

            const tabs = userMenu.locator('gtx-tabs');
            messagesTab = await selectTab(tabs, 'messages');

            const userLoad = page.waitForResponse(matchRequest('GET', '/rest/user'));
            await messagesTab.locator('.new-message-button button').click();
            await userLoad;
        });

        await test.step('Send Message to myself', async () => {
            const modal = page.locator('send-message-modal');
            const form = modal.locator('.modal-content gtx-message-properties');

            await pickSelectValue(form.locator('[formcontrolname="recipientIds"]'), [`toUserId_${login.user.id}`]);
            await form.locator('[formcontrolname="message"] textarea').fill(MESSAGE_CONTENT);

            const sendReq = page.waitForResponse(matchRequest('POST', '/rest/msg/send'));
            await modal.locator('.modal-footer [data-action="confirm"] button').click();
            await sendReq;
        });

        await test.step('Receive the Message', async () => {
            // The UI polls every 30 seconds, so we wait a bit longer in case we missed the cycle
            const messageLoad = page.waitForResponse(matchRequest('GET', '/rest/msg/list'), {
                timeout: 40_000,
            });
            await messageLoad;

            receivedMessage = messagesTab.locator('.unread-messages .message').filter({
                hasText: MESSAGE_CONTENT,
            });
            // Should be rendered in the messages list
            await receivedMessage.waitFor({ state: 'visible' });
        });

        await test.step('Delete the Message', async () => {
            const deleteReq = page.waitForResponse(matchRequest('DELETE', '/rest/msg/*'));
            await receivedMessage.locator('.message-actions [data-action="delete"]').dblclick({
                delay: 200,
            });
            await deleteReq;
        });
    });

    test.skip('should not be see users, therefore can not send messages', {
        annotation: [{
            type: 'ticket',
            description: 'SUP-18935',
        }],
    }, async ({ page }) => {

    });
});
