import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';
import { listQueries, sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { SessionTodos } from './SessionTodos';

const TODO_QUERY = '?status=released&status=review_requested&status=waiting_for_input';

const released = sessionFixture({ id: 's-1', title: 'Year update', status: 'released' });
const inReview = sessionFixture({ id: 's-2', title: 'Newsletter sign-up', status: 'review_requested' });
const waiting = sessionFixture({ id: 's-3', title: 'Careers intro', status: 'waiting_for_input' });

function group(name: string) {
    return within(screen.getByRole('region', { name }));
}

describe('SessionTodos', () => {
    beforeEach(() => {
        useSessionArchiveStore.setState({ hiddenIds: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('asks only for the user\'s sessions in released, review_requested and waiting_for_input', async () => {
        const fetchMock = stubSessionRoutes(() => ({ items: [released], next_cursor: null }));

        renderWithProviders(<SessionTodos />);

        await screen.findByRole('region', { name: 'Open to-dos' });
        expect(listQueries(fetchMock)).toEqual([TODO_QUERY]);
    });

    it('puts each status in its own group, in a fixed order', async () => {
        // Newest activity first from the server, so the order mixes the statuses.
        stubSessionRoutes(() => ({ items: [waiting, released, inReview], next_cursor: null }));

        renderWithProviders(<SessionTodos />);

        const card = await screen.findByRole('region', { name: 'Open to-dos' });

        expect(within(card).getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent)).toEqual([
            'Ready to publish',
            'Waiting for review',
            'Waiting for your answer',
        ]);
        expect(group('Ready to publish').getByRole('link')).toHaveTextContent('Year update');
        expect(group('Waiting for review').getByRole('link')).toHaveTextContent('Newsletter sign-up');
        expect(group('Waiting for your answer').getByRole('link')).toHaveTextContent('Careers intro');
        expect(group('Ready to publish').getByRole('link')).toHaveAttribute('href', '/sessions/s-1');
    });

    it('leaves out a group without to-dos', async () => {
        stubSessionRoutes(() => ({ items: [released], next_cursor: null }));

        renderWithProviders(<SessionTodos />);

        await screen.findByRole('region', { name: 'Ready to publish' });
        expect(screen.queryByRole('region', { name: 'Waiting for review' })).not.toBeInTheDocument();
        expect(screen.queryByRole('region', { name: 'Waiting for your answer' })).not.toBeInTheDocument();
    });

    it('renders nothing when there are no to-dos', async () => {
        const fetchMock = stubSessionRoutes(() => ({ items: [], next_cursor: null }));

        const { router } = renderWithProviders(<SessionTodos />);

        await waitFor(() => expect(fetchMock).toHaveBeenCalled());
        await waitFor(() => expect(router.state.status).toBe('idle'));
        expect(screen.queryByRole('region', { name: 'Open to-dos' })).not.toBeInTheDocument();
    });

    it('loads the next page with "Show more" and sorts its sessions into their groups', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSessionRoutes((url) => (url.searchParams.get('cursor') === 'c2'
            ? { items: [waiting], next_cursor: null }
            : { items: [released], next_cursor: 'c2' }));

        renderWithProviders(<SessionTodos />);

        await user.click(await screen.findByRole('button', { name: 'Show more' }));

        expect(await screen.findByRole('region', { name: 'Waiting for your answer' })).toBeInTheDocument();
        expect(group('Ready to publish').getByRole('link')).toHaveTextContent('Year update');
        expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument();
        expect(listQueries(fetchMock)).toEqual([TODO_QUERY, `${TODO_QUERY}&cursor=c2`]);
    });

    it('hides a deleted session', async () => {
        useSessionArchiveStore.setState({ hiddenIds: ['s-2'] });
        stubSessionRoutes(() => ({ items: [released, inReview], next_cursor: null }));

        renderWithProviders(<SessionTodos />);

        await screen.findByRole('region', { name: 'Ready to publish' });
        expect(screen.queryByRole('region', { name: 'Waiting for review' })).not.toBeInTheDocument();
    });
});
