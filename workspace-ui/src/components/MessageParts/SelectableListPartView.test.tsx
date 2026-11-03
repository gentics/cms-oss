import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { SelectableListPart } from '@/services/apiService/genaix/types';
import { renderWithProviders } from '@/test/renderWithProviders';

import { SelectableListPartView } from './SelectableListPartView';

import '@/i18n';

const items = [
    { id: 'tpl-17', label: 'Kampagnen-Landingpage', ref: { type: 'template' as const, id: '17', node_id: 3 } },
    { id: 'tpl-21', label: 'Themenseite' },
];

function renderList(part: Partial<SelectableListPart>) {
    return render(
        <SelectableListPartView part={{ type: 'selectable_list', label: 'Which template?', items, multi: false, ...part }} sessionId="s-1" />,
        { wrapper: UiProvider },
    );
}

describe('SelectableListPartView', () => {
    it('picks exactly one item when not multi, starting from `selected`', async () => {
        const user = userEvent.setup();

        renderList({ multi: false, selected: ['tpl-17'] });

        expect(screen.getByRole('radiogroup', { name: 'Which template?' })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Kampagnen-Landingpage/ })).toBeChecked();

        await user.click(screen.getByText('Themenseite'));

        expect(screen.getByRole('radio', { name: /Themenseite/ })).toBeChecked();
        expect(screen.getByRole('radio', { name: /Kampagnen-Landingpage/ })).not.toBeChecked();
        expect(screen.getByText('1 selected')).toBeInTheDocument();
    });

    it('picks several items when multi', async () => {
        const user = userEvent.setup();

        renderList({ multi: true });

        expect(screen.getByText('0 selected')).toBeInTheDocument();

        await user.click(screen.getByRole('checkbox', { name: /Kampagnen-Landingpage/ }));
        await user.click(screen.getByRole('checkbox', { name: /Themenseite/ }));

        expect(screen.getByRole('checkbox', { name: /Kampagnen-Landingpage/ })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: /Themenseite/ })).toBeChecked();
        expect(screen.getByText('2 selected')).toBeInTheDocument();

        await user.click(screen.getByRole('checkbox', { name: /Themenseite/ }));

        expect(screen.getByText('1 selected')).toBeInTheDocument();
    });

    describe('Edit these', () => {
        function renderHandOff(part: Partial<SelectableListPart>) {
            return renderWithProviders(
                <SelectableListPartView part={{ type: 'selectable_list', label: 'Which template?', items, multi: true, ...part }} sessionId="s-1" />,
            );
        }

        it('hands the objects of the selected items to the composer', async () => {
            const user = userEvent.setup();

            renderHandOff({});

            const editThese = await screen.findByRole('button', { name: 'Edit these' });

            expect(editThese).toBeDisabled();

            // An item without an object hands nothing over.
            await user.click(screen.getByRole('checkbox', { name: /Themenseite/ }));

            expect(editThese).toBeDisabled();

            await user.click(screen.getByRole('checkbox', { name: /Kampagnen-Landingpage/ }));

            expect(editThese).toBeEnabled();
            expect(screen.getByText('2 selected')).toBeInTheDocument();
        });

        it('leaves a list that answers a choice interaction to the interaction card', async () => {
            renderHandOff({ interaction_id: 'e3f7b1a9-2c64-4d08-95b7-1a8e0c5d7f36' });

            expect(await screen.findByText('0 selected')).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Edit these' })).not.toBeInTheDocument();
        });
    });
});
