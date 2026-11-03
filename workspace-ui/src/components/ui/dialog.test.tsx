import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Button } from './button';
import { Dialog, DialogBody, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from './dialog';
import { UiProvider } from './provider';

import '@/i18n';

function PublishDialog() {
    return (
        <Dialog>
            <DialogTrigger render={<Button />}>Open dialog</DialogTrigger>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Publish page</DialogTitle>
                    <DialogDescription>The page goes live right away.</DialogDescription>
                </DialogHeader>
                <DialogBody>Content</DialogBody>
                <DialogFooter>
                    <DialogClose render={<Button />}>Cancel</DialogClose>
                    <DialogClose render={<Button variant="primary" />}>Publish</DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

describe('Dialog', () => {
    it('opens from the keyboard as a named modal dialog with focus inside', async () => {
        const user = userEvent.setup();

        render(<PublishDialog />, { wrapper: UiProvider });

        await user.tab();
        await user.keyboard('{Enter}');

        const dialog = await screen.findByRole('dialog', { name: 'Publish page' });

        expect(dialog).toHaveAccessibleDescription('The page goes live right away.');
        expect(dialog).toContainElement(document.activeElement as HTMLElement);
    });

    it('keeps focus inside while tabbing', async () => {
        const user = userEvent.setup();

        render(<PublishDialog />, { wrapper: UiProvider });

        await user.click(screen.getByRole('button', { name: 'Open dialog' }));
        const dialog = await screen.findByRole('dialog', { name: 'Publish page' });

        // At the edges Tab lands on a Base UI focus guard, which moves focus back into the dialog
        // in the next animation frame.
        for (let step = 0; step < 5; step++) {
            await user.tab();
            await waitFor(() => expect(dialog).toContainElement(document.activeElement as HTMLElement));
        }
    });

    it('closes with Escape and with the close button, returning focus to the trigger', async () => {
        const user = userEvent.setup();

        render(<PublishDialog />, { wrapper: UiProvider });

        const trigger = screen.getByRole('button', { name: 'Open dialog' });

        await user.click(trigger);
        await screen.findByRole('dialog');
        await user.keyboard('{Escape}');

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();

        await user.click(trigger);
        await user.click(await screen.findByRole('button', { name: 'Close' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();
    });
});
