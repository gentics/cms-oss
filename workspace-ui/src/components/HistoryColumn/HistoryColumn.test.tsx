import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { listQueries, sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { HistoryColumn } from './HistoryColumn';

const landingPage = sessionFixture({ id: 's-1', title: 'Landing page' });
const careers = sessionFixture({ id: 's-2', title: 'Careers page' });

function stubSearch() {
    return stubSessionRoutes((url) => {
        const q = url.searchParams.get('q');

        return { items: [landingPage, careers].filter((session) => !q || session.title!.toLowerCase().includes(q)), next_cursor: null };
    });
}

describe('HistoryColumn', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the user\'s sessions under "Sessions"', async () => {
        stubSearch();

        renderWithProviders(<HistoryColumn />);

        expect(await screen.findByRole('region', { name: 'Sessions' })).toBeInTheDocument();
        expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /Careers page/ })).toBeInTheDocument();
    });

    it('searches on the server once typing pauses', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSearch();

        renderWithProviders(<HistoryColumn />);

        await screen.findByRole('link', { name: /Landing page/ });
        await user.type(screen.getByRole('searchbox', { name: 'Find a session' }), 'careers');

        // One request for the whole word, not one per key.
        await waitFor(() => expect(listQueries(fetchMock)).toEqual(['', '?q=careers']));
        await waitFor(() => expect(screen.queryByRole('link', { name: /Landing page/ })).not.toBeInTheDocument());
        expect(await screen.findByRole('link', { name: /Careers page/ })).toBeInTheDocument();
    });

    it('says when no session matches, and Escape clears the search', async () => {
        const user = userEvent.setup();

        stubSearch();
        renderWithProviders(<HistoryColumn />);

        const search = await screen.findByRole('searchbox', { name: 'Find a session' });

        await user.type(search, 'imprint');

        expect(await screen.findByText('No session matches “imprint”.')).toBeInTheDocument();

        await user.keyboard('{Escape}');

        expect(search).toHaveValue('');
        expect(await screen.findByRole('link', { name: /Landing page/ })).toBeInTheDocument();
    });
});
