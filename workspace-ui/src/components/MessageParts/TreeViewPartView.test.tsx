import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { TreeViewPart } from '@/services/apiService/genaix/types';

import { TreeViewPartView } from './TreeViewPartView';

import '@/i18n';

const part: TreeViewPart = {
    type: 'tree_view',
    label: 'Candidate folders',
    nodes: [{
        id: '3',
        label: 'Acme GmbH',
        type: 'node',
        children: [
            { id: '42', label: 'Richtlinien', type: 'folder', selected: true, children: [{ id: '8871', label: 'Garantiebedingungen', type: 'page', children: [] }] },
            { id: '57', label: 'Aktuelles', type: 'folder', children: [] },
        ],
    }],
};

function renderTree() {
    return render(<TreeViewPartView part={part} sessionId="s-1" />, { wrapper: UiProvider });
}

describe('TreeViewPartView', () => {
    it('starts with every level open and the selection from `selected`', () => {
        renderTree();

        expect(screen.getByRole('button', { name: 'Collapse Acme GmbH' })).toHaveAttribute('aria-expanded', 'true');
        expect(screen.getByRole('checkbox', { name: 'Garantiebedingungen' })).toBeInTheDocument();
        expect(screen.getByRole('checkbox', { name: 'Richtlinien' })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: 'Aktuelles' })).not.toBeChecked();
    });

    it('collapses and expands a node with children; a leaf has no toggle', async () => {
        const user = userEvent.setup();

        renderTree();

        await user.click(screen.getByRole('button', { name: 'Collapse Richtlinien' }));

        expect(screen.queryByRole('checkbox', { name: 'Garantiebedingungen' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Expand Richtlinien' })).toHaveAttribute('aria-expanded', 'false');

        await user.click(screen.getByRole('button', { name: 'Expand Richtlinien' }));

        expect(screen.getByRole('checkbox', { name: 'Garantiebedingungen' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Aktuelles/ })).not.toBeInTheDocument();
    });

    it('selects and deselects a node by its label', async () => {
        const user = userEvent.setup();

        renderTree();

        await user.click(screen.getByText('Aktuelles'));
        expect(screen.getByRole('checkbox', { name: 'Aktuelles' })).toBeChecked();

        await user.click(screen.getByRole('checkbox', { name: 'Richtlinien' }));
        expect(screen.getByRole('checkbox', { name: 'Richtlinien' })).not.toBeChecked();
    });
});
