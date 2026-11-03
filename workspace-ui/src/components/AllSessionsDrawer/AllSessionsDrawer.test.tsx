import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { AllSessionsDrawer } from './AllSessionsDrawer';

describe('AllSessionsDrawer', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows every session with the search focused, and closes when one is opened', async () => {
        const user = userEvent.setup();
        const onOpenChange = vi.fn<(open: boolean) => void>();

        stubSessionRoutes(() => ({ items: [sessionFixture({ id: 's-1', title: 'Landing page' })], next_cursor: null }));

        const { router } = renderWithProviders(<AllSessionsDrawer open onOpenChange={onOpenChange} />);

        expect(await screen.findByRole('dialog', { name: 'All sessions' })).toBeInTheDocument();
        await waitFor(() => expect(screen.getByRole('searchbox', { name: 'Find a session' })).toHaveFocus());

        await user.click(await screen.findByRole('link', { name: /Landing page/ }));

        await waitFor(() => expect(router.state.location.pathname).toBe('/sessions/s-1'));
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('renders nothing while closed', async () => {
        stubSessionRoutes(() => ({ items: [], next_cursor: null }));

        const { router } = renderWithProviders(<AllSessionsDrawer open={false} onOpenChange={() => undefined} />);

        await waitFor(() => expect(router.state.status).toBe('idle'));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
