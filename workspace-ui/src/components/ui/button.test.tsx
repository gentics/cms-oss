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

    it('renders a link in its look, which stays a link', () => {
        render(
            <Button variant="ghost" nativeButton={false} role={undefined} render={<a href="/" />}>
                Home
            </Button>,
            { wrapper: UiProvider },
        );

        expect(screen.getByRole('link', { name: 'Home' })).toHaveAttribute('data-variant', 'ghost');
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('has a brand size for the logo and name in the topbar', () => {
        render(<Button variant="ghost" size="brand">Gentics Workspace</Button>, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Gentics Workspace' })).toHaveAttribute('data-size', 'brand');
    });

    it('has a mini icon size for chips and passages', () => {
        render(<Button variant="ghost" size="icon-xs" aria-label="Remove">×</Button>, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Remove' })).toHaveAttribute('data-size', 'icon-xs');
    });

    it('has a large round icon size for the main voice action', () => {
        render(<Button variant="primary" size="icon-xl" aria-label="Tap to speak">●</Button>, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Tap to speak' })).toHaveAttribute('data-size', 'icon-xl');
    });

    it('is a toggle with aria-pressed', () => {
        render(<Button variant="ghost" aria-pressed>Verbatim</Button>, { wrapper: UiProvider });

        expect(screen.getByRole('button', { name: 'Verbatim' })).toHaveAttribute('aria-pressed', 'true');
    });
});
