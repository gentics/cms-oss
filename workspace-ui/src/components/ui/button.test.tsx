import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Button } from './button';
import { UiProvider } from './provider';

import '@/i18n';

describe('Button', () => {
    it('is reached with Tab and activated with Enter and Space', async () => {
        const user = userEvent.setup();
        const onClick = vi.fn();

        render(<Button onClick={onClick}>Save</Button>, { wrapper: UiProvider });

        await user.tab();
        expect(screen.getByRole('button', { name: 'Save' })).toHaveFocus();

        await user.keyboard('{Enter}');
        await user.keyboard(' ');

        expect(onClick).toHaveBeenCalledTimes(2);
    });

    it('cannot be activated while disabled', async () => {
        const user = userEvent.setup();
        const onClick = vi.fn();

        render(
            <Button disabled onClick={onClick}>
                Save
            </Button>,
            { wrapper: UiProvider },
        );

        await user.click(screen.getByRole('button', { name: 'Save' }));
        await user.tab();

        expect(onClick).not.toHaveBeenCalled();
        expect(screen.getByRole('button', { name: 'Save' })).not.toHaveFocus();
    });

    it('marks its variant for styling and tests', () => {
        render(<Button variant="primary">Publish</Button>, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Publish' })).toHaveAttribute('data-variant', 'primary');
    });

    it('has a ghost-danger variant for deleting in a list row', () => {
        render(<Button variant="ghost-danger" size="icon" aria-label="Delete" />, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Delete' })).toHaveAttribute('data-variant', 'ghost-danger');
    });
});
