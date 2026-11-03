import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Label } from './label';
import { UiProvider } from './provider';
import { Radio, RadioGroup } from './radio-group';

import '@/i18n';

function Templates({ onValueChange, disabled }: { onValueChange?: (value: unknown) => void; disabled?: boolean }) {
    return (
        <RadioGroup aria-label="Template" defaultValue="17" onValueChange={onValueChange} disabled={disabled}>
            <Label>
                <Radio value="17" />
                Kampagnen-Landingpage
            </Label>
            <Label>
                <Radio value="21" />
                Themenseite
            </Label>
        </RadioGroup>
    );
}

describe('RadioGroup', () => {
    it('selects exactly one radio, named by its label', async () => {
        const user = userEvent.setup();
        const onValueChange = vi.fn();

        render(<Templates onValueChange={onValueChange} />, { wrapper: UiProvider });

        const group = screen.getByRole('radiogroup', { name: 'Template' });
        const first = screen.getByRole('radio', { name: 'Kampagnen-Landingpage' });
        const second = screen.getByRole('radio', { name: 'Themenseite' });

        expect(group).toBeInTheDocument();
        expect(first).toBeChecked();
        expect(second).not.toBeChecked();

        await user.click(second);

        expect(second).toBeChecked();
        expect(first).not.toBeChecked();
        expect(onValueChange).toHaveBeenLastCalledWith('21', expect.anything());
    });

    it('does not change while disabled', async () => {
        const user = userEvent.setup();

        render(<Templates disabled />, { wrapper: UiProvider });

        await user.click(screen.getByRole('radio', { name: 'Themenseite' }));

        expect(screen.getByRole('radio', { name: 'Kampagnen-Landingpage' })).toBeChecked();
    });
});
