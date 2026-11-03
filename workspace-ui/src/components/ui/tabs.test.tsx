import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from './provider';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './tabs';

import '@/i18n';

function PreviewTabs() {
    return (
        <Tabs defaultValue="preview">
            <TabsList aria-label="View">
                <TabsTrigger value="preview">Preview</TabsTrigger>
                <TabsTrigger value="history">History</TabsTrigger>
                <TabsTrigger value="settings" disabled>
                    Settings
                </TabsTrigger>
            </TabsList>
            <TabsContent value="preview">Preview panel</TabsContent>
            <TabsContent value="history">History panel</TabsContent>
            <TabsContent value="settings">Settings panel</TabsContent>
        </Tabs>
    );
}

describe('Tabs', () => {
    it('moves focus with the arrow keys and activates a tab with Enter', async () => {
        const user = userEvent.setup();

        render(<PreviewTabs />, { wrapper: UiProvider });

        await user.tab();
        expect(screen.getByRole('tab', { name: 'Preview' })).toHaveFocus();
        expect(screen.getByRole('tabpanel')).toHaveTextContent('Preview panel');

        await user.keyboard('{ArrowRight}');
        const history = screen.getByRole('tab', { name: 'History' });
        expect(history).toHaveFocus();

        await user.keyboard('{Enter}');
        expect(history).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByRole('tabpanel')).toHaveTextContent('History panel');
    });

    // Disabled tabs stay focusable (aria-disabled) so screen reader users can discover them, as the
    // WAI-ARIA APG recommends ("Focusability of disabled controls"); they cannot be activated.
    it('keeps the disabled tab focusable but does not activate it', async () => {
        const user = userEvent.setup();

        render(<PreviewTabs />, { wrapper: UiProvider });

        await user.tab();
        await user.keyboard('{ArrowRight}{ArrowRight}');

        const settings = screen.getByRole('tab', { name: 'Settings' });
        expect(settings).toHaveFocus();
        expect(settings).toHaveAttribute('aria-disabled', 'true');

        await user.keyboard('{Enter}');
        expect(settings).toHaveAttribute('aria-selected', 'false');
        expect(screen.getByRole('tabpanel')).toHaveTextContent('Preview panel');
    });
});
