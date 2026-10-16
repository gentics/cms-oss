import { createHashHistory, createRootRoute, createRoute, createRouter, Outlet } from '@tanstack/react-router';

import { DashboardPage } from '@/pages/DashboardPage/DashboardPage';
import { ReviewPage } from '@/pages/ReviewPage/ReviewPage';
import { SessionPage } from '@/pages/SessionPage/SessionPage';

const rootRoute = createRootRoute({
    component: Outlet,
});

const homeRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/',
    component: DashboardPage,
});

// The session route shows the SessionPage, the three-column workspace; its review route the
// ReviewPage. They are siblings, not nested: the review page takes the place of the session workspace
// instead of sitting inside it.
const sessionRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id',
    component: SessionPage,
});

// `?plan=<plan id>` opens that plan; without it the review page shows the session's newest plan.
// `?page=<page id>` shows that one of the plan's pages, when it wrote several.
const sessionReviewRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id/review',
    validateSearch: (search: Record<string, unknown>): { plan?: string; page?: number } => {
        const page = Number(search.page);

        return {
            ...(typeof search.plan === 'string' && search.plan ? { plan: search.plan } : {}),
            ...(Number.isSafeInteger(page) && page > 0 ? { page } : {}),
        };
    },
    component: ReviewPage,
});

export const routeTree = rootRoute.addChildren([homeRoute, sessionRoute, sessionReviewRoute]);

// Hash history, like the CMS's own UIs: the route lives after `#`, so the app works under whatever
// path the CMS serves it from, and a reload never asks the CMS for a path it has no file for.
export const router = createRouter({ routeTree, history: createHashHistory() });

declare module '@tanstack/react-router' {
    interface Register {
        router: typeof router;
    }
}
