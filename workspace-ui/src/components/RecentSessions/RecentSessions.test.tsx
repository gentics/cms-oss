import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useSessionArchiveStore } from '@/store/useSessionArchiveStore';
import { sessionFixture, stubSessionRoutes } from '@/test/genaixSessions';
import { renderWithProviders } from '@/test/renderWithProviders';

import { RecentSessions } from './RecentSessions';

const sessions = ['Landing page', 'Careers page', 'Prices 2026', 'Imprint'].map((title, index) => sessionFixture({ id: `s-${index + 1}`, title }));

describe('RecentSessions', () => {
    beforeEach(() => {
        useSessionArchiveStore.setState({ hiddenIds: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('folds out the three newest sessions', async () => {
        const user = userEvent.setup();

        stubSessionRoutes(() => ({ items: sessions, next_cursor: null }));
        renderWithProviders(<RecentSessions />);

        const toggle = await screen.findByRole('button', { name: 'Recent sessions' });

        expect(toggle).toHaveAttribute('aria-expanded', 'false');
        expect(screen.queryByRole('link')).not.toBeInTheDocument();

        await user.click(toggle);

        expect(toggle).toHaveAttribute('aria-expanded', 'true');
        expect((await screen.findAllByRole('link')).map((link) => link.textContent)).toEqual([
            expect.stringContaining('Landing page'),
            expect.stringContaining('Careers page'),
            expect.stringContaining('Prices 2026'),
        ]);
    });

    it('opens the "All sessions" drawer from "Show all sessions"', async () => {
        const user = userEvent.setup();

        stubSessionRoutes(() => ({ items: sessions, next_cursor: null }));
        renderWithProviders(<RecentSessions />);

        await user.click(await screen.findByRole('button', { name: 'Recent sessions' }));
        await user.click(await screen.findByRole('button', { name: 'Show all sessions' }));

        const drawer = await screen.findByRole('dialog', { name: 'All sessions' });

        expect(await screen.findAllByRole('link', { name: /Imprint/ })).toHaveLength(1);
        expect(drawer).toContainElement(screen.getByRole('link', { name: /Imprint/ }));
    });

    it('offers no "Show all sessions" when the three are all there is', async () => {
        const user = userEvent.setup();

        stubSessionRoutes(() => ({ items: sessions.slice(0, 3), next_cursor: null }));
        renderWithProviders(<RecentSessions />);

        await user.click(await screen.findByRole('button', { name: 'Recent sessions' }));

        expect(await screen.findAllByRole('link')).toHaveLength(3);
        expect(screen.queryByRole('button', { name: 'Show all sessions' })).not.toBeInTheDocument();
    });
});
