import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import i18n from '@/i18n';
import { useThemeStore } from '@/store/useThemeStore';
import { renderWithProviders } from '@/test/renderWithProviders';

import { Topbar } from './Topbar';

describe('Topbar', () => {
    beforeEach(() => {
        useThemeStore.setState({ theme: 'light' });
        delete document.documentElement.dataset.theme;
    });

    afterEach(async () => {
        delete document.documentElement.dataset.theme;
        document.documentElement.lang = 'en';
        await i18n.changeLanguage('en');
    });

    it('shows the brand', async () => {
        renderWithProviders(<Topbar />);

        expect(await screen.findByRole('banner')).toHaveTextContent('Gentics Workspace');
    });

    it('leads home to the dashboard from the brand', async () => {
        const user = userEvent.setup();
        const { router } = renderWithProviders(<Topbar />, { path: '/sessions/s-1' });

        const home = await screen.findByRole('link', { name: 'Gentics Workspace' });

        expect(home).toHaveAttribute('href', '/');
        expect(home).toHaveAttribute('title', 'Back to the dashboard');

        await user.click(home);

        await waitFor(() => expect(router.state.location.pathname).toBe('/'));
    });

    it('has no column toggle without a left column', async () => {
        renderWithProviders(<Topbar />);

        expect(await screen.findByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /left column/ })).not.toBeInTheDocument();
    });

    it('hides and shows the left column from the button at its far left', async () => {
        const user = userEvent.setup();

        function WithLeftColumn() {
            const [isVisible, setIsVisible] = useState(true);

            return <Topbar isLeftColumnVisible={isVisible} onToggleLeftColumn={() => setIsVisible((visible) => !visible)} />;
        }

        renderWithProviders(<WithLeftColumn />);

        const hide = await screen.findByRole('button', { name: 'Hide the left column' });

        // Before the brand, the first control in the bar.
        expect(screen.getByRole('banner').firstElementChild).toContainElement(hide);

        await user.click(hide);

        expect(await screen.findByRole('button', { name: 'Show the left column' })).toBeInTheDocument();
    });

    it('has no right column toggle without a right column', async () => {
        renderWithProviders(<Topbar onToggleLeftColumn={() => {}} />);

        expect(await screen.findByRole('button', { name: 'Hide the left column' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /right column/ })).not.toBeInTheDocument();
    });

    it('hides and shows the right column from the button at its far right', async () => {
        const user = userEvent.setup();

        function WithRightColumn() {
            const [isVisible, setIsVisible] = useState(true);

            return <Topbar isRightColumnVisible={isVisible} onToggleRightColumn={() => setIsVisible((visible) => !visible)} />;
        }

        renderWithProviders(<WithRightColumn />);

        const hide = await screen.findByRole('button', { name: 'Hide the right column' });

        // After the light/dark toggle, the last control in the bar.
        expect(screen.getByRole('banner').lastElementChild).toContainElement(hide);

        await user.click(hide);

        expect(await screen.findByRole('button', { name: 'Show the right column' })).toBeInTheDocument();
    });

    it('switches between English and German', async () => {
        const user = userEvent.setup();

        renderWithProviders(<Topbar />);

        const toGerman = await screen.findByRole('button', { name: 'Switch to Deutsch' });

        expect(toGerman).toHaveTextContent('EN');

        await user.click(toGerman);

        const toEnglish = await screen.findByRole('button', { name: 'Zu English wechseln' });

        expect(toEnglish).toHaveTextContent('DE');
        expect(document.documentElement).toHaveAttribute('lang', 'de');
        expect(screen.getByRole('link', { name: 'Gentics Workspace' })).toHaveAttribute('title', 'Zurück zum Dashboard');

        await user.click(toEnglish);

        expect(await screen.findByRole('button', { name: 'Switch to Deutsch' })).toBeInTheDocument();
        expect(document.documentElement).toHaveAttribute('lang', 'en');
    });

    it('switches between light and dark', async () => {
        const user = userEvent.setup();

        renderWithProviders(<Topbar />);

        await user.click(await screen.findByRole('button', { name: 'Dark mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'dark');

        await user.click(screen.getByRole('button', { name: 'Light mode' }));

        expect(document.documentElement).toHaveAttribute('data-theme', 'light');
        expect(screen.getByRole('button', { name: 'Dark mode' })).toBeInTheDocument();
    });
});
