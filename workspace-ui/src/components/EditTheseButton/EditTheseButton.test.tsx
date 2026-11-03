import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import { useHandOffStore } from '@/store/useHandOffStore';

import { EditTheseButton } from './EditTheseButton';

import '@/i18n';

const page = { type: 'page' as const, id: '8871', node_id: 3, label: 'Garantiebedingungen' };

describe('EditTheseButton', () => {
    beforeEach(() => {
        useHandOffStore.setState({ handOffs: {} });
    });

    it('hands the selected objects to the composer of its session', async () => {
        const user = userEvent.setup();

        render(
            <EditTheseButton sessionId="s-1" references={[page]} />,
            { wrapper: UiProvider },
        );

        await user.click(screen.getByRole('button', { name: 'Edit these' }));

        expect(useHandOffStore.getState().handOffs['s-1']).toEqual([page]);
    });

    it('is disabled while nothing is selected', () => {
        render(<EditTheseButton sessionId="s-1" references={[]} />, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Edit these' })).toBeDisabled();
    });
});
