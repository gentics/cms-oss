import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Label } from './label';
import { UiProvider } from './provider';
import { Textarea } from './textarea';

import '@/i18n';

describe('Textarea', () => {
    it('is named by its label and takes several lines of typed text', async () => {
        const user = userEvent.setup();

        render(
            <>
                <Label htmlFor="feedback">What should happen?</Label>
                <Textarea id="feedback" />
            </>,
            { wrapper: UiProvider },
        );

        const field = screen.getByRole('textbox', { name: 'What should happen?' });

        await user.type(field, 'Shorter{Shift>}{Enter}{/Shift}more active');

        expect(field).toHaveValue('Shorter\nmore active');
    });

    it('can be disabled', () => {
        render(<Textarea aria-label="Feedback" disabled />, { wrapper: UiProvider });

        expect(screen.getByRole('textbox', { name: 'Feedback' })).toBeDisabled();
    });
});
