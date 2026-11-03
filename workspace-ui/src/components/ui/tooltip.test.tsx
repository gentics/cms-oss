import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Undo2Icon } from 'lucide-react';
import { describe, expect, it } from 'vitest';

import { Button } from './button';
import { UiProvider } from './provider';
import { Tooltip, TooltipContent, TooltipTrigger } from './tooltip';

import '@/i18n';

function UndoButton() {
    return (
        <Tooltip>
            <TooltipTrigger
                render={(
                    <Button variant="ghost" size="icon" aria-label="Undo">
                        <Undo2Icon />
                    </Button>
                )}
            />
            <TooltipContent>Undo last change</TooltipContent>
        </Tooltip>
    );
}

describe('Tooltip', () => {
    it('opens on keyboard focus and closes with Escape', async () => {
        const user = userEvent.setup();

        render(<UndoButton />, { wrapper: UiProvider });

        await user.tab();
        expect(screen.getByRole('button', { name: 'Undo' })).toHaveFocus();
        expect(await screen.findByText('Undo last change')).toBeVisible();

        await user.keyboard('{Escape}');

        await waitFor(() => expect(screen.queryByText('Undo last change')).not.toBeInTheDocument());
        expect(screen.getByRole('button', { name: 'Undo' })).toHaveFocus();
    });
});
