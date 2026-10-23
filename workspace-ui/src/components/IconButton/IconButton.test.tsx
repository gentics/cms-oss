import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { XIcon } from 'lucide-react';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { IconButton } from './IconButton';

import '@/i18n';

describe('IconButton', () => {
    it('is named by its label, shows it as a tooltip on focus and passes clicks through', async () => {
        const user = userEvent.setup();
        const onClick = vi.fn();

        render(<IconButton variant="ghost" size="icon" label="Remove" onClick={onClick}><XIcon /></IconButton>, { wrapper: UiProvider });

        await user.tab();

        expect(screen.getByRole('button', { name: 'Remove' })).toHaveFocus();
        expect(await screen.findByText('Remove', { selector: '[data-slot="tooltip-content"]' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Remove' }));

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
