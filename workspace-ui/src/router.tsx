import { createHashHistory, createRootRoute, createRoute, createRouter, Outlet } from '@tanstack/react-router';

import { DashboardPage } from '@/pages/DashboardPage/DashboardPage';
import { PublishedPage } from '@/pages/PublishedPage/PublishedPage';
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

// After a plan was published from the review page: what went out, and the way to the next session.
// `?plan=<plan id>` names the plan; without it the page has nothing to show.
const sessionPublishedRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id/published',
    validateSearch: (search: Record<string, unknown>): { plan?: string } => (
        typeof search.plan === 'string' && search.plan ? { plan: search.plan } : {}
    ),
    component: PublishedPage,
});

export const routeTree = rootRoute.addChildren([homeRoute, sessionRoute, sessionReviewRoute, sessionPublishedRoute]);

// Hash history, like the CMS's own UIs: the route lives after `#`, so the app works under whatever
// path the CMS serves it from, and a reload never asks the CMS for a path it has no file for.
export const router = createRouter({ routeTree, history: createHashHistory() });

declare module '@tanstack/react-router' {
    interface Register {
        router: typeof router;
    }
}
