import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Input } from './input';
import { Label } from './label';
import { UiProvider } from './provider';

import '@/i18n';

describe('Input', () => {
    it('is named by its label and takes typed text', async () => {
        const user = userEvent.setup();

        render(
            <>
                <Label htmlFor="name">Name</Label>
                <Input id="name" />
            </>,
            { wrapper: UiProvider },
        );

        const field = screen.getByRole('textbox', { name: 'Name' });

        await user.type(field, 'Gentics');

        expect(field).toHaveValue('Gentics');
    });

    it('reports a change of its value', async () => {
        const user = userEvent.setup();
        const values: string[] = [];

        render(<Input aria-label="Name" onValueChange={(value) => values.push(value)} />, { wrapper: UiProvider });

        await user.type(screen.getByRole('textbox', { name: 'Name' }), 'ab');

        expect(values).toEqual(['a', 'ab']);
    });

    it('can be marked invalid and disabled', () => {
        render(
            <>
                <Input aria-label="Invalid" aria-invalid />
                <Input aria-label="Disabled" disabled />
            </>,
            { wrapper: UiProvider },
        );

        expect(screen.getByRole('textbox', { name: 'Invalid' })).toBeInvalid();
        expect(screen.getByRole('textbox', { name: 'Disabled' })).toBeDisabled();
    });
});
