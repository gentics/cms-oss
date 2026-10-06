import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useThemeStore } from '@/store/useThemeStore';

import { Topbar } from './Topbar';

import '@/i18n';

describe('Topbar', () => {
    beforeEach(() => {
        useThemeStore.setState({ theme: 'light' });
        delete document.documentElement.dataset.theme;
    });

    afterEach(() => {
        delete document.documentElement.dataset.theme;
    });

    it('shows the brand', () => {
        render(<Topbar isLeftColumnVisible onToggleLeftColumn={() => undefined} />);

        expect(screen.getByRole('banner')).toHaveTextContent('Gentics Workspace');
    });

    it('offers to hide the left column while it is visible, and to show it while it is hidden', async () => {
        const user = userEvent.setup();
        const onToggleLeftColumn = vi.fn<() => void>();
        const { rerender } = render(<Topbar isLeftColumnVisible onToggleLeftColumn={onToggleLeftColumn} />);

        await user.click(screen.getByRole('button', { name: 'Hide the left column' }));

        expect(onToggleLeftColumn).toHaveBeenCalledTimes(1);

        rerender(<Topbar isLeftColumnVisible={false} onToggleLeftColumn={onToggleLeftColumn} />);

        expect(screen.getByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Hide the left column' })).not.toBeInTheDocument();
    });

    it('has no left-column toggle without onToggleLeftColumn', () => {
        render(<Topbar />);

        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
    });

    it('switches between light and dark', async () => {
        const user = userEvent.setup();

        render(<Topbar isLeftColumnVisible onToggleLeftColumn={() => undefined} />);

        await user.click(screen.getByRole('button', { name: 'Dark mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'dark');

        await user.click(screen.getByRole('button', { name: 'Light mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'light');
        expect(screen.getByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
    });
});
