import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useRef, useState } from 'react';
import { describe, expect, it } from 'vitest';

import { Button } from './button';
import { Popover, PopoverClose, PopoverContent, PopoverTitle, PopoverTrigger } from './popover';
import { UiProvider } from './provider';

import '@/i18n';

describe('Popover', () => {
    it('opens from its trigger, is named by its title, and closes with its close button', async () => {
        const user = userEvent.setup();

        render(
            <Popover>
                <PopoverTrigger render={<Button />}>Feedback</PopoverTrigger>
                <PopoverContent>
                    <PopoverTitle>Feedback for this spot</PopoverTitle>
                    <PopoverClose render={<Button />}>Cancel</PopoverClose>
                </PopoverContent>
            </Popover>,
            { wrapper: UiProvider },
        );

        await user.click(screen.getByRole('button', { name: 'Feedback' }));
        expect(await screen.findByRole('dialog', { name: 'Feedback for this spot' })).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });

    it('opens without a trigger at an anchor element, and closes with Escape', async () => {
        const user = userEvent.setup();

        function AnchoredPopover() {
            const anchor = useRef<HTMLDivElement>(null);
            const [open, setOpen] = useState(false);

            return (
                <>
                    <div ref={anchor}>Paragraph</div>
                    <Button onClick={() => setOpen(true)}>Give feedback</Button>
                    <Popover open={open} onOpenChange={setOpen}>
                        <PopoverContent anchor={anchor}>
                            <PopoverTitle>Paragraph</PopoverTitle>
                            <input aria-label="What should happen?" />
                        </PopoverContent>
                    </Popover>
                </>
            );
        }

        render(<AnchoredPopover />, { wrapper: UiProvider });
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Give feedback' }));
        expect(await screen.findByRole('dialog', { name: 'Paragraph' })).toBeInTheDocument();

        await user.click(screen.getByRole('textbox', { name: 'What should happen?' }));
        await user.keyboard('{Escape}');
        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    });
});
