import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { useCmsTokenStore } from '@/store/useCmsTokenStore';

import App from './App';

// jsdom has no ResizeObserver; the AppShell's workspace layout measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

const tokenCreated = { token: 'cmstok_app', id: 1, userId: 3, name: 'genaix-workspace-1', cdate: 1_790_752_317, expires: 0, lastUsed: 0, valid: true };

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
        useCmsTokenStore.getState().clearCmsToken();
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(Response.json(tokenCreated))));
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // The router resolves the route asynchronously, so the page is awaited with findByRole.
    it('renders the main content', async () => {
        renderApp();

        expect(await screen.findByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    });

    it('increments the counter when clicked', async () => {
        const user = userEvent.setup();

        renderApp();

        const counter = await screen.findByRole('button', { name: 'Count is 0' });

        expect(counter).toBeInTheDocument();

        await user.click(counter);

        expect(screen.getByRole('button', { name: 'Count is 1' })).toBeInTheDocument();
    });

    // Skipped while useCmsToken() is commented out in App.tsx.
    it.skip('posts for the CMS token when the app starts', async () => {
        renderApp();

        await waitFor(() => expect(useCmsTokenStore.getState().cmsToken?.token).toBe('cmstok_app'));

        const [url, init] = vi.mocked(fetch).mock.calls[0]!;

        expect(fetch).toHaveBeenCalledTimes(1);
        expect(url).toBe('/rest/admin/token');
        expect(init?.method).toBe('POST');
        expect(JSON.parse(init?.body as string)).toEqual({ name: expect.stringMatching(/^genaix-workspace-/) });
    });
});
