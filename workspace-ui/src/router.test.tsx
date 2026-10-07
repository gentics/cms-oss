import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { router, routeTree } from './router';

import '@/i18n';

// jsdom has no ResizeObserver; the home page's AppShell measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function renderAt(path: string) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });

    // The dashboard at / starts sessions through TanStack Query and shows toasts.
    return render(
        <QueryClientProvider client={new QueryClient()}>
            <UiProvider>
                <RouterProvider router={router} />
            </UiProvider>
        </QueryClientProvider>,
    );
}

describe('router', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the dashboard at /', async () => {
        renderAt('/');

        expect(await screen.findByRole('textbox', { name: 'What would you like to do?' })).toBeInTheDocument();
    });

    // For now the review route shows the session page too.
    it('shows the session page with its chat composer at /sessions/:id', async () => {
        renderAt('/sessions/abc');

        expect(await screen.findByRole('textbox', { name: 'What should happen?' })).toBeInTheDocument();
    });

    it('shows the session page at /sessions/:id/review', async () => {
        renderAt('/sessions/abc/review');

        expect(await screen.findByRole('textbox', { name: 'What should happen?' })).toBeInTheDocument();
    });

    // The CMS serves the app under a path of its own, for example /tools/workspace/.
    it('keeps the route in the URL hash and leaves the page path alone', async () => {
        const pathname = window.location.pathname;

        await router.navigate({ to: '/sessions/$id', params: { id: 'abc' } });

        expect(window.location.hash).toBe('#/sessions/abc');
        expect(window.location.pathname).toBe(pathname);

        await router.navigate({ to: '/' });
    });
});
