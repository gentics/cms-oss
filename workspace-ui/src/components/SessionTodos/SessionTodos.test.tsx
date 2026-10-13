import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { Session, SessionPage } from '@/services/apiService/genaix/types';
import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';
import { listQueries, sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { SessionTodos } from './SessionTodos';

// One query per group (rc.4): releasing and review are a plan's status, waiting for an answer the session's.
const RELEASED_QUERY = '?plan_status=released';
const REVIEW_QUERY = '?plan_status=review_requested';
const WAITING_QUERY = '?status=waiting_for_input';

const released = sessionFixture({ id: 's-1', title: 'Year update', status: 'active' });
const inReview = sessionFixture({ id: 's-2', title: 'Newsletter sign-up', status: 'active' });
const waiting = sessionFixture({ id: 's-3', title: 'Careers intro', status: 'waiting_for_input' });
const releasedLater = sessionFixture({ id: 's-4', title: 'Price list', status: 'active' });

// Answers each group's query with its sessions, in one page.
function byGroup(groups: { released?: Session[]; inReview?: Session[]; waiting?: Session[] }) {
    return (url: URL): SessionPage => {
        const planStatus = url.searchParams.get('plan_status');
        const items = planStatus === 'released' ? groups.released : planStatus === 'review_requested' ? groups.inReview : groups.waiting;

        return { items: items ?? [], next_cursor: null };
    };
}

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
        const fetchMock = stubSessionRoutes(byGroup({ released: [released] }));

        renderWithProviders(<SessionTodos />);

        await screen.findByRole('region', { name: 'Open to-dos' });
        expect(listQueries(fetchMock).toSorted()).toEqual([RELEASED_QUERY, REVIEW_QUERY, WAITING_QUERY].toSorted());
    });

    it('puts each status in its own group, in a fixed order', async () => {
        stubSessionRoutes(byGroup({ released: [released], inReview: [inReview], waiting: [waiting] }));

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
        stubSessionRoutes(byGroup({ released: [released] }));

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

    it('loads the next page of a group with "Show more" into that group', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSessionRoutes((url) => {
            if (url.searchParams.get('plan_status') !== 'released') {
                return { items: [], next_cursor: null };
            }

            return url.searchParams.get('cursor') === 'c2' ? { items: [releasedLater], next_cursor: null } : { items: [released], next_cursor: 'c2' };
        });

        renderWithProviders(<SessionTodos />);

        await user.click(await screen.findByRole('button', { name: 'Show more' }));

        await waitFor(() => expect(group('Ready to publish').getAllByRole('link').map((link) => link.textContent)).toEqual([
            expect.stringContaining('Year update'),
            expect.stringContaining('Price list'),
        ]));
        expect(screen.queryByRole('button', { name: 'Show more' })).not.toBeInTheDocument();
        expect(listQueries(fetchMock)).toContain(`${RELEASED_QUERY}&cursor=c2`);
    });

    it('hides a deleted session', async () => {
        useSessionArchiveStore.setState({ hiddenIds: ['s-2'] });
        stubSessionRoutes(byGroup({ released: [released], inReview: [inReview] }));

        renderWithProviders(<SessionTodos />);

        await screen.findByRole('region', { name: 'Ready to publish' });
        expect(screen.queryByRole('region', { name: 'Waiting for review' })).not.toBeInTheDocument();
    });
});
