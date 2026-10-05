import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Button } from './button';
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from './dropdown-menu';
import { UiProvider } from './provider';

import '@/i18n';

function OptionsMenu({ onRename = () => undefined }: { onRename?: () => void }) {
    return (
        <DropdownMenu>
            <DropdownMenuTrigger render={<Button />}>Options</DropdownMenuTrigger>
            <DropdownMenuContent>
                <DropdownMenuGroup>
                    <DropdownMenuLabel>Page</DropdownMenuLabel>
                    <DropdownMenuItem onClick={onRename}>Rename</DropdownMenuItem>
                    <DropdownMenuItem disabled>Save</DropdownMenuItem>
                </DropdownMenuGroup>
                <DropdownMenuSeparator />
                <DropdownMenuCheckboxItem defaultChecked>Line numbers</DropdownMenuCheckboxItem>
                <DropdownMenuItem variant="danger">Delete</DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

describe('DropdownMenu', () => {
    it('opens with Enter, moves with the arrow keys and runs an item with Enter', async () => {
        const user = userEvent.setup();
        const onRename = vi.fn();

        render(<OptionsMenu onRename={onRename} />, { wrapper: UiProvider });

        const trigger = screen.getByRole('button', { name: 'Options' });

        await user.tab();
        expect(trigger).toHaveFocus();

        await user.keyboard('{Enter}');
        expect(await screen.findByRole('menu')).toBeInTheDocument();

        // Which item is highlighted first is up to Base UI; walk down to "Rename".
        const rename = screen.getByRole('menuitem', { name: 'Rename' });
        for (let step = 0; step < 4 && document.activeElement !== rename; step++) {
            await user.keyboard('{ArrowDown}');
        }
        expect(rename).toHaveFocus();

        await user.keyboard('{Enter}');

        expect(onRename).toHaveBeenCalledOnce();
        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
        expect(trigger).toHaveFocus();
    });

    it('closes with Escape and returns focus to the trigger', async () => {
        const user = userEvent.setup();

        render(<OptionsMenu />, { wrapper: UiProvider });

        await user.tab();
        await user.keyboard('{Enter}');
        expect(await screen.findByRole('menu')).toBeInTheDocument();

        await user.keyboard('{Escape}');

        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
        expect(screen.getByRole('button', { name: 'Options' })).toHaveFocus();
    });

    it('marks disabled and checkbox items for assistive technology', async () => {
        const user = userEvent.setup();

        render(<OptionsMenu />, { wrapper: UiProvider });

        await user.click(screen.getByRole('button', { name: 'Options' }));

        expect(await screen.findByRole('menuitem', { name: 'Save' })).toHaveAttribute('aria-disabled', 'true');
        expect(screen.getByRole('menuitemcheckbox', { name: 'Line numbers' })).toHaveAttribute('aria-checked', 'true');
    });

    it('selects one radio item of a group', async () => {
        const user = userEvent.setup();
        const onValueChange = vi.fn();

        render(
            <DropdownMenu>
                <DropdownMenuTrigger render={<Button />}>View</DropdownMenuTrigger>
                <DropdownMenuContent>
                    <DropdownMenuRadioGroup value="tree" onValueChange={onValueChange}>
                        <DropdownMenuRadioItem value="tree">Tree</DropdownMenuRadioItem>
                        <DropdownMenuRadioItem value="table">Table</DropdownMenuRadioItem>
                    </DropdownMenuRadioGroup>
                </DropdownMenuContent>
            </DropdownMenu>,
            { wrapper: UiProvider },
        );

        await user.click(screen.getByRole('button', { name: 'View' }));

        expect(await screen.findByRole('menuitemradio', { name: 'Tree' })).toHaveAttribute('aria-checked', 'true');
        expect(screen.getByRole('menuitemradio', { name: 'Table' })).toHaveAttribute('aria-checked', 'false');

        await user.click(screen.getByRole('menuitemradio', { name: 'Table' }));

        expect(onValueChange).toHaveBeenCalledWith('table', expect.anything());
    });
});
