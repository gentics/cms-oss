import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSessions } from '@/hooks/useGenaixQueries';
import { useErrorNotificationStore } from '@/store/useErrorNotificationStore';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';
import { archivedIds, listQueries, sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { SessionList } from './SessionList';

/** The toast's close button. Found by its slot, as in `ErrorNotifications.test.tsx`. */
function toastCloseButton(title: string) {
    return screen.getByText(title).closest<HTMLElement>('[data-slot="toast"]')!.querySelector<HTMLElement>('[data-slot="toast-close"]')!;
}

function AllSessions({ maxItems }: { maxItems?: number }) {
    return <SessionList sessions={useSessions()} maxItems={maxItems} />;
}

const landingPage = sessionFixture({ id: 's-1', title: 'Landing page' });
const careers = sessionFixture({ id: 's-2', title: 'Careers page', created_at: '2025-08-26T09:00:00Z' });

describe('SessionList', () => {
    beforeEach(() => {
        useSessionArchiveStore.setState({ hiddenIds: [] });
        useErrorNotificationStore.setState({ errors: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('links every session to its page and shows the day it was started', async () => {
        stubSessionRoutes(() => ({ items: [landingPage, careers], next_cursor: null }));

        renderWithProviders(<AllSessions />);

        const link = await screen.findByRole('link', { name: /Landing page/ });

        expect(link).toHaveAttribute('href', '/sessions/s-1');
        expect(screen.getByRole('link', { name: /Careers page/ })).toHaveTextContent('2025');
    });

    it('marks the open session as current', async () => {
        stubSessionRoutes(() => ({ items: [landingPage, careers], next_cursor: null }));

        renderWithProviders(<AllSessions />, { path: '/sessions/s-2' });

        expect(await screen.findByRole('link', { name: /Careers page/ })).toHaveAttribute('aria-current', 'page');
        expect(screen.getByRole('link', { name: /Landing page/ })).not.toHaveAttribute('aria-current');
    });

    it('falls back to the intent, then to "Untitled session"', async () => {
        stubSessionRoutes(() => ({
            items: [sessionFixture({ id: 's-1', title: undefined, intent: 'Create a page' }), sessionFixture({ id: 's-2', title: undefined })],
            next_cursor: null,
        }));

        renderWithProviders(<AllSessions />);

        expect(await screen.findByRole('link', { name: /Create a page/ })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Untitled session/ })).toBeInTheDocument();
    });

    it('says so when there are no sessions', async () => {
        stubSessionRoutes(() => ({ items: [], next_cursor: null }));

        renderWithProviders(<AllSessions />);

        expect(await screen.findByText('No sessions yet')).toBeInTheDocument();
    });

    it('shows a failed load with its message and loads again on "Try again"', async () => {
        let fail = true;
        const fetchMock = vi.fn<typeof fetch>(async () => (fail
            ? Response.json({ type: 't', title: 't', status: 404, genaix_code: 'session_not_found' }, { status: 404 })
            : Response.json({ items: [landingPage], next_cursor: null })));

        vi.stubGlobal('fetch', fetchMock);

        renderWithProviders(<AllSessions />);

        const alert = await screen.findByRole('alert');

        expect(alert).toHaveTextContent('The session was not found.');

        fail = false;
        await userEvent.setup().click(within(alert).getByRole('button', { name: 'Try again' }));

        expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
    });

    it('loads the next page with "Show more" until there is none', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSessionRoutes((url) => (url.searchParams.get('cursor') === 'c2'
            ? { items: [careers], next_cursor: null }
            : { items: [landingPage], next_cursor: 'c2' }));

        renderWithProviders(<AllSessions />);

        await user.click(await screen.findByRole('button', { name: 'Show more' }));

        expect(await screen.findByRole('link', { name: /Careers page/ })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Landing page/ })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument();
        expect(listQueries(fetchMock)).toEqual(['', '?cursor=c2']);
    });

    it('shows at most maxItems rows and no "Show more"', async () => {
        stubSessionRoutes(() => ({ items: [landingPage, careers], next_cursor: 'c2' }));

        renderWithProviders(<AllSessions maxItems={1} />);

        expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /Careers page/ })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument();
    });

    describe('deleting', () => {
        it('hides the row at once and brings it back on "Undo", without archiving', async () => {
            const user = userEvent.setup();
            const fetchMock = stubSessionRoutes(() => ({ items: [landingPage, careers], next_cursor: null }));

            renderWithProviders(<AllSessions />);

            await user.click(await screen.findByRole('button', { name: 'Delete “Landing page”' }));

            expect(screen.queryByRole('link', { name: /Landing page/ })).not.toBeInTheDocument();
            expect(screen.getByText('“Landing page” deleted')).toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: 'Undo' }));

            expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
            await waitFor(() => expect(screen.queryByText('“Landing page” deleted')).not.toBeInTheDocument());
            expect(archivedIds(fetchMock)).toEqual([]);
        });

        it('archives the session once the toast closes without an undo, and the row stays away', async () => {
            const user = userEvent.setup();
            let archived = false;
            const fetchMock = stubSessionRoutes(
                () => ({ items: archived ? [careers] : [landingPage, careers], next_cursor: null }),
                () => {
                    archived = true;

                    return new Response(null, { status: 204 });
                },
            );

            renderWithProviders(<AllSessions />);

            await user.click(await screen.findByRole('button', { name: 'Delete “Landing page”' }));

            expect(archivedIds(fetchMock)).toEqual([]);

            await user.click(toastCloseButton('“Landing page” deleted'));

            await waitFor(() => expect(archivedIds(fetchMock)).toEqual(['s-1']));
            // The lists are refetched after the archive.
            await waitFor(() => expect(listQueries(fetchMock)).toEqual(['', '']));
            expect(screen.queryByRole('link', { name: /Landing page/ })).not.toBeInTheDocument();
            expect(screen.getByRole('link', { name: /Careers page/ })).toBeInTheDocument();
        });

        it('brings the row back with an error notification when the archive fails', async () => {
            const user = userEvent.setup();

            stubSessionRoutes(
                () => ({ items: [landingPage], next_cursor: null }),
                () => Response.json({ type: 't', title: 't', status: 403, genaix_code: 'session_forbidden' }, { status: 403 }),
            );

            renderWithProviders(<AllSessions />);

            await user.click(await screen.findByRole('button', { name: 'Delete “Landing page”' }));
            await user.click(toastCloseButton('“Landing page” deleted'));

            expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
            expect(useErrorNotificationStore.getState().errors).toMatchObject([
                { messageKey: 'sessions.archiveFailed', detailKey: 'errors.genaix.session_forbidden' },
            ]);
        });
    });
});
