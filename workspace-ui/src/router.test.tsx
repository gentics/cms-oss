import { createMemoryHistory, createRouter, RouterProvider } from '@tanstack/react-router';
import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { routeTree } from './router';

import '@/i18n';

// jsdom has no ResizeObserver; the home page's AppShell measures its columns with one.
class ResizeObserverStub {
    observe() {}

    disconnect() {}
}

function renderAt(path: string) {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });

    return render(<RouterProvider router={router} />);
}

describe('router', () => {
    beforeEach(() => {
        vi.stubGlobal('ResizeObserver', ResizeObserverStub);
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the home page at /', async () => {
        renderAt('/');

        expect(await screen.findByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    });

    // For now the session routes show the home page too.
    it('shows the home page at /sessions/:id', async () => {
        renderAt('/sessions/abc');

        expect(await screen.findByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    });

    it('shows the home page at /sessions/:id/review', async () => {
        renderAt('/sessions/abc/review');

        expect(await screen.findByRole('heading', { name: 'Get started' })).toBeInTheDocument();
    });
});
