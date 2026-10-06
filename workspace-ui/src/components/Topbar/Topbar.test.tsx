import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

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
        render(<Topbar />);

        expect(screen.getByRole('banner')).toHaveTextContent('Gentics Workspace');
    });

    // The column toggles sit in the columns (WorkspaceLayout), not in the topbar.
    it('has no column toggle', () => {
        render(<Topbar />);

        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
    });

    it('switches between light and dark', async () => {
        const user = userEvent.setup();

        render(<Topbar />);

        await user.click(screen.getByRole('button', { name: 'Dark mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'dark');

        await user.click(screen.getByRole('button', { name: 'Light mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'light');
        expect(screen.getByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
    });
});
