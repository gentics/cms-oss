import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Button } from './button';
import { UiProvider } from './provider';
import { useToast } from './use-toast';

import '@/i18n';

function ShowToast({ onUndo }: { onUndo?: () => void }) {
    const toast = useToast();

    return (
        <Button
            onClick={() =>
                toast.add({
                    title: 'Page deleted',
                    type: 'success',
                    timeout: 0,
                    actionProps: onUndo ? { children: 'Undo', onClick: onUndo } : undefined,
                })}
        >
            Delete
        </Button>
    );
}

describe('Toast', () => {
    it('is announced in the notifications live region', async () => {
        const user = userEvent.setup();

        render(<ShowToast />, { wrapper: UiProvider });

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        const region = screen.getByRole('region', { name: 'Notifications' });

        expect(region).toHaveAttribute('aria-live', 'polite');
        expect(await within(region).findByText('Page deleted')).toBeInTheDocument();
    });

    it('runs its action and can be dismissed with the keyboard', async () => {
        const user = userEvent.setup();
        const onUndo = vi.fn();

        render(<ShowToast onUndo={onUndo} />, { wrapper: UiProvider });

        await user.click(screen.getByRole('button', { name: 'Delete' }));

        const undo = await screen.findByRole('button', { name: 'Undo' });
        undo.focus();
        await user.keyboard('{Enter}');
        expect(onUndo).toHaveBeenCalledOnce();

        screen.getByRole('button', { name: 'Dismiss' }).focus();
        await user.keyboard('{Enter}');

        await waitFor(() => expect(screen.queryByText('Page deleted')).not.toBeInTheDocument());
    });
});
