import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Checkbox } from './checkbox';
import { Label } from './label';
import { UiProvider } from './provider';

import '@/i18n';

describe('Checkbox', () => {
    it('gets its name from the label and toggles with Space', async () => {
        const user = userEvent.setup();

        render(
            <Label>
                <Checkbox />
                Publish now
            </Label>,
            { wrapper: UiProvider },
        );

        const checkbox = screen.getByRole('checkbox', { name: 'Publish now' });

        await user.tab();
        expect(checkbox).toHaveFocus();
        expect(checkbox).not.toBeChecked();

        await user.keyboard(' ');
        expect(checkbox).toBeChecked();

        await user.keyboard(' ');
        expect(checkbox).not.toBeChecked();
    });

    it('reports the indeterminate state as mixed', () => {
        render(<Checkbox indeterminate aria-label="All pages" />, { wrapper: UiProvider });

        expect(screen.getByRole('checkbox', { name: 'All pages' })).toHaveAttribute('aria-checked', 'mixed');
    });

    it('does not toggle while disabled', async () => {
        const user = userEvent.setup();

        render(<Checkbox disabled aria-label="Publish now" />, { wrapper: UiProvider });

        const checkbox = screen.getByRole('checkbox', { name: 'Publish now' });

        await user.click(checkbox);

        expect(checkbox).not.toBeChecked();
        expect(checkbox).toHaveAttribute('aria-disabled', 'true');
    });
});
