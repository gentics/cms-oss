import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { Button } from './button';
import { Drawer, DrawerBody, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle, DrawerTrigger } from './drawer';
import { UiProvider } from './provider';

import '@/i18n';

function SessionsDrawer() {
    return (
        <Drawer>
            <DrawerTrigger render={<Button />}>Open drawer</DrawerTrigger>
            <DrawerContent>
                <DrawerHeader>
                    <DrawerTitle>Sessions</DrawerTitle>
                    <DrawerDescription>Your recent sessions.</DrawerDescription>
                </DrawerHeader>
                <DrawerBody>
                    <Button>Session 1</Button>
                </DrawerBody>
            </DrawerContent>
        </Drawer>
    );
}

describe('Drawer', () => {
    it('opens from the keyboard as a named dialog with focus inside', async () => {
        const user = userEvent.setup();

        render(<SessionsDrawer />, { wrapper: UiProvider });

        await user.tab();
        await user.keyboard('{Enter}');

        const drawer = await screen.findByRole('dialog', { name: 'Sessions' });

        expect(drawer).toHaveAccessibleDescription('Your recent sessions.');
        expect(drawer).toContainElement(document.activeElement as HTMLElement);
    });

    it('closes with Escape and with the close button, returning focus to the trigger', async () => {
        const user = userEvent.setup();

        render(<SessionsDrawer />, { wrapper: UiProvider });

        const trigger = screen.getByRole('button', { name: 'Open drawer' });

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
