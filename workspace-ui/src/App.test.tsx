import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

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
        vi.stubGlobal('fetch', vi.fn<typeof fetch>().mockImplementation(() => Promise.resolve(Response.json(tokenCreated))));
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    // The router resolves the route asynchronously, so the page is awaited with findByRole.
    it('renders the dashboard at /', async () => {
        renderApp();

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
    });
});
