import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Label } from './label';
import { UiProvider } from './provider';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './select';

import '@/i18n';

const items = { home: 'Home', news: 'News', contact: 'Contact' };

function PageSelect() {
    return (
        <>
            <Label htmlFor="page">Page</Label>
            <Select items={items}>
                <SelectTrigger id="page">
                    <SelectValue placeholder="Choose a page" />
                </SelectTrigger>
                <SelectContent>
                    {Object.entries(items).map(([value, label]) => (
                        <SelectItem key={value} value={value}>
                            {label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </>
    );
}

describe('Select', () => {
    it('is named by its label and shows the placeholder', () => {
        render(<PageSelect />, { wrapper: UiProvider });

        expect(screen.getByRole('combobox', { name: 'Page' })).toHaveTextContent('Choose a page');
    });

    it('opens with ArrowDown and selects an option with the keyboard', async () => {
        const user = userEvent.setup();

        render(<PageSelect />, { wrapper: UiProvider });

        const trigger = screen.getByRole('combobox', { name: 'Page' });

        await user.tab();
        expect(trigger).toHaveFocus();

        await user.keyboard('{ArrowDown}');
        expect(await screen.findByRole('listbox')).toBeInTheDocument();

        // Which option is highlighted first is up to Base UI; walk down to "News".
        const news = screen.getByRole('option', { name: 'News' });
        for (let step = 0; step < Object.keys(items).length && document.activeElement !== news; step++) {
            await user.keyboard('{ArrowDown}');
        }
        expect(news).toHaveFocus();

        await user.keyboard('{Enter}');

        expect(trigger).toHaveTextContent('News');
        await waitFor(() => expect(screen.queryByRole('listbox')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();
    });

    it('closes with Escape and returns focus to the trigger', async () => {
        const user = userEvent.setup();

        render(<PageSelect />, { wrapper: UiProvider });

        const trigger = screen.getByRole('combobox', { name: 'Page' });

        await user.tab();
        await user.keyboard('{ArrowDown}');
        expect(await screen.findByRole('listbox')).toBeInTheDocument();

        await user.keyboard('{Escape}');

        await waitFor(() => expect(screen.queryByRole('listbox')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();
    });
});
