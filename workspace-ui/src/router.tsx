import { createRootRoute, createRoute, createRouter, Outlet } from '@tanstack/react-router';

import { HomePage } from '@/pages/HomePage/HomePage';

const rootRoute = createRootRoute({
    component: Outlet,
});

const homeRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/',
    component: HomePage,
});

// For now every route shows the HomePage. The session routes are siblings, not nested: the review
// page takes the place of the session workspace instead of sitting inside it.
const sessionRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id',
    component: HomePage,
});

const sessionReviewRoute = createRoute({
    getParentRoute: () => rootRoute,
    path: '/sessions/$id/review',
    component: HomePage,
});

export const routeTree = rootRoute.addChildren([homeRoute, sessionRoute, sessionReviewRoute]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
    interface Register {
        router: typeof router;
    }
}
