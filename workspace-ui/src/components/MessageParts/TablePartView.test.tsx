import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';

import { HandOffContext } from '@/components/EditTheseButton/handOffContext';
import { UiProvider } from '@/components/ui/provider';
import i18n from '@/i18n';
import type { TablePart } from '@/services/apiService/genaix/types';
import { renderWithProviders } from '@/test/renderWithProviders';

import { TablePartView } from './TablePartView';

const part: TablePart = {
    type: 'table',
    label: 'Pages mentioning the terms of service',
    total: 23,
    columns: [
        { key: 'page', label: 'Page', type: 'ref' },
        { key: 'folder', label: 'Folder', type: 'string' },
        { key: 'online', label: 'Online', type: 'boolean' },
        { key: 'published_at', label: 'Published', type: 'date' },
        { key: 'hits', label: 'Hits', type: 'number' },
    ],
    rows: [
        { page: { type: 'page', id: '8871', node_id: 3, label: 'Garantiebedingungen' }, folder: 'Richtlinien', online: true, published_at: '2023-06-14T08:12:00Z', hits: 1250 },
        { page: { type: 'page', id: '8903', node_id: 3, label: 'Nutzungsbedingungen' }, folder: null, online: false, published_at: '2023-02-01T09:00:00Z', hits: 3 },
    ],
    source: { tool: 'search_content', summary: '23 hits, permission-filtered.' },
};

describe('TablePartView', () => {
    afterEach(async () => {
        await i18n.changeLanguage('en');
    });

    it('shows every cell by its column type', () => {
        render(<TablePartView part={part} sessionId="s-1" />, { wrapper: UiProvider });

        const [, first, second] = screen.getAllByRole('row');

        expect(within(first!).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['Garantiebedingungen', 'Richtlinien', 'Yes', 'Jun 14, 2023', '1,250']);
        expect(within(second!).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['Nutzungsbedingungen', '–', 'No', 'Feb 1, 2023', '3']);
    });

    it('says when it shows only a page of the rows, and where they came from', () => {
        render(<TablePartView part={part} sessionId="s-1" />, { wrapper: UiProvider });

        expect(screen.getByText('2 of 23')).toBeInTheDocument();
        expect(screen.getByText('23 hits, permission-filtered.')).toBeInTheDocument();
    });

    it('formats numbers, dates and booleans in the UI language', async () => {
        await i18n.changeLanguage('de');
        render(<TablePartView part={part} sessionId="s-1" />, { wrapper: UiProvider });

        const [, first] = screen.getAllByRole('row');

        expect(within(first!).getAllByRole('cell').map((cell) => cell.textContent)).toEqual(['Garantiebedingungen', 'Richtlinien', 'Ja', '14.06.2023', '1.250']);
    });

    it('has no selection outside a read-only session', () => {
        render(<TablePartView part={part} sessionId="s-1" />, { wrapper: UiProvider });

        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Edit these' })).not.toBeInTheDocument();
    });

    describe('in a read-only session', () => {
        function renderSelectable(table: TablePart = part) {
            return renderWithProviders(
                <HandOffContext value={{ nodeId: 3 }}>
                    <TablePartView part={table} sessionId="s-1" />
                </HandOffContext>,
            );
        }

        it('selects rows by their object, and all of them at once', async () => {
            const user = userEvent.setup();

            renderSelectable();

            const editThese = await screen.findByRole('button', { name: 'Edit these' });
            const all = screen.getByRole('checkbox', { name: 'Select all rows' });

            expect(screen.getByText('0 selected')).toBeInTheDocument();
            expect(editThese).toBeDisabled();
            // The foot holds only the selection, without the notes on paging and source.
            expect(screen.queryByText('2 of 23')).not.toBeInTheDocument();
            expect(screen.queryByText('23 hits, permission-filtered.')).not.toBeInTheDocument();

            await user.click(screen.getByRole('checkbox', { name: 'Select Garantiebedingungen' }));

            expect(screen.getByText('1 selected')).toBeInTheDocument();
            expect(editThese).toBeEnabled();
            expect(all).toHaveAttribute('aria-checked', 'mixed');

            await user.click(all);

            expect(screen.getByText('2 selected')).toBeInTheDocument();
            expect(screen.getByRole('checkbox', { name: 'Select Nutzungsbedingungen' })).toBeChecked();

            await user.click(all);

            expect(screen.getByText('0 selected')).toBeInTheDocument();
        });

        it('offers no checkbox for a row without an object, and no selection for a table without a ref column', async () => {
            const withoutRef = { ...part, rows: [...part.rows, { page: null, folder: 'Archiv', online: false, hits: 0 }] };
            const { unmount } = renderSelectable(withoutRef);

            const rows = await screen.findAllByRole('row');

            expect(within(rows.at(-1)!).queryByRole('checkbox')).not.toBeInTheDocument();
            expect(screen.getAllByRole('checkbox')).toHaveLength(3);

            unmount();
            renderSelectable({ ...part, columns: part.columns.filter((column) => column.type !== 'ref') });

            expect(await screen.findByRole('table')).toBeInTheDocument();
            expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: 'Edit these' })).not.toBeInTheDocument();
        });
    });
});
