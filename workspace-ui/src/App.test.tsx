import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import i18n from '@/i18n';

import App from './App';

// jsdom has no ResizeObserver; the AppShell's workspace layout measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

const tokenCreated = { token: 'cmstok_app', id: 1, userId: 3, name: 'genaix-workspace-1', cdate: 1_790_752_317, expires: 0, lastUsed: 0, valid: true };
const cmsUser = { id: 3, login: 'editor', firstName: 'Eddie', lastName: 'Tor' };

/**
 * Answers like a CMS: `GET /rest/user/me` with `me` (`401` without), no Keycloak, a login, the session's
 * UI language `language` if given, empty GenAIx lists, the rest with a token.
 */
function stubCms({ me, language }: { me?: typeof cmsUser; language?: string } = {}) {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((input) => {
        const url = String(input);

        if (url === '/rest/user/me') {
            return Promise.resolve(me ? Response.json({ user: me }) : new Response('', { status: 401 }));
        }

        if (url === '/rest/keycloak') {
            return Promise.resolve(new Response('', { status: 404 }));
        }

        if (url === '/rest/i18n/get' && language) {
            return Promise.resolve(Response.json({ responseInfo: { responseCode: 'OK' }, code: language }));
        }

        if (url === '/rest/auth/login') {
            return Promise.resolve(Response.json({ responseInfo: { responseCode: 'OK' }, user: cmsUser }));
        }

        // GenAIx lists (the dashboard's to-dos) are empty.
        if (url.startsWith('/rest/proxy/genaix/')) {
            return Promise.resolve(Response.json({ items: [] }));
        }

        return Promise.resolve(Response.json(tokenCreated));
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderApp() {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    return render(
        <QueryClientProvider client={queryClient}>
            <UiProvider>
                <App />
            </UiProvider>
        </QueryClientProvider>,
    );
}

describe('App', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(async () => {
        vi.unstubAllGlobals();
        document.documentElement.lang = 'en';
        await i18n.changeLanguage('en');
    });

    // The router resolves the route asynchronously, so the page is awaited with findByRole.
    it('renders the dashboard at / with a CMS session', async () => {
        stubCms({ me: cmsUser });
        renderApp();

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
    });

    it('shows the login without a CMS session and the dashboard once logged in', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms();

        renderApp();

        await user.type(await screen.findByLabelText('User name'), 'editor');
        await user.type(screen.getByLabelText('Password'), 'secret');
        await user.click(screen.getByRole('button', { name: 'Log in' }));

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
        expect(fetchMock).toHaveBeenCalledWith('/rest/auth/login', expect.objectContaining({ method: 'POST', body: JSON.stringify({ login: 'editor', password: 'secret' }) }));
    });

    it('takes the UI language of the CMS session once logged in', async () => {
        stubCms({ me: cmsUser, language: 'de' });
        renderApp();

        expect(await screen.findByRole('textbox', { name: 'Was möchten Sie tun?' })).toBeInTheDocument();
        expect(document.documentElement).toHaveAttribute('lang', 'de');
    });

    it('keeps a language the user switches to after taking the one of the CMS session, without telling the CMS', async () => {
        const user = userEvent.setup();
        const fetchMock = stubCms({ me: cmsUser, language: 'de' });

        renderApp();

        await user.click(await screen.findByRole('button', { name: 'Zu English wechseln' }));

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Was möchten Sie tun?' })).not.toBeInTheDocument();
        expect(i18n.language).toBe('en');
        expect(fetchMock).not.toHaveBeenCalledWith('/rest/i18n/set', expect.anything());
    });

    it('keeps the language when the CMS session has one the Workspace does not have', async () => {
        const fetchMock = stubCms({ me: cmsUser, language: 'fr' });

        renderApp();

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
        expect(fetchMock).toHaveBeenCalledWith('/rest/i18n/get', expect.anything());
        expect(document.documentElement).not.toHaveAttribute('lang', 'fr');
    });
});
