import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryHistory, createRootRoute, createRoute, createRouter, Outlet, RouterProvider } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import type { ReactElement, ReactNode } from 'react';

import { UiProvider } from '@/components/ui/provider';

import '@/i18n';

/** Query client and `UiProvider` (toasts), for `renderHook(…, { wrapper: createWrapper() })`. */
export function createWrapper(queryClient = new QueryClient()) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return (
            <QueryClientProvider client={queryClient}>
                <UiProvider>{children}</UiProvider>
            </QueryClientProvider>
        );
    };
}

/**
 * Renders `ui` with a query client, `UiProvider` and a memory router at `path` that knows the
 * session routes, so `Link`s work and `router.state.location` shows where one led. The router
 * renders asynchronously: query with `findBy…` first.
 */
export function renderWithProviders(ui: ReactElement, { path = '/' }: { path?: string } = {}) {
    const queryClient = new QueryClient();
    const rootRoute = createRootRoute({ component: () => <>{ui}<Outlet /></> });
    const routeTree = rootRoute.addChildren(['/', '/sessions/$id', '/sessions/$id/review'].map((routePath) => (
        createRoute({ getParentRoute: () => rootRoute, path: routePath, component: () => null })
    )));
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: [path] }) });
    const Wrapper = createWrapper(queryClient);

    return { router, queryClient, ...render(<Wrapper><RouterProvider router={router} /></Wrapper>) };
}
