import { createHashHistory, createRootRoute, createRoute, createRouter, Outlet } from '@tanstack/react-router';

import { DashboardPage } from '@/pages/DashboardPage/DashboardPage';
import { SessionPage } from '@/pages/SessionPage/SessionPage';

const rootRoute = createRootRoute({
    component: Outlet,
});

const homeRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/',
    component: DashboardPage,
});

// The session routes show the SessionPage, the three-column workspace. They are siblings, not
// nested: the review page takes the place of the session workspace instead of sitting inside it.
const sessionRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id',
    component: SessionPage,
});

const sessionReviewRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id/review',
    component: SessionPage,
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
