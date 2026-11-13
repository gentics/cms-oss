import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PencilIcon } from 'lucide-react';
import { describe, expect, it } from 'vitest';

import { Button } from './button';
import { CardLayoutBody, CardLayoutFooter, CardLayoutHeader } from './card-layout';
import { Popover, PopoverClose, PopoverContent, PopoverTitle, PopoverTrigger } from './popover';
import { UiProvider } from './provider';

import '@/i18n';

describe('CardLayout', () => {
    it('lays out a popover in head, body and foot, the popover still named by its title in the head', async () => {
        const user = userEvent.setup();

        render(
            <Popover>
                <PopoverTrigger render={<Button />}>Edit link</PopoverTrigger>
                <PopoverContent>
                    <CardLayoutHeader>
                        <PencilIcon size={16} aria-hidden="true" />
                        <PopoverTitle>Link</PopoverTitle>
                        <PopoverClose render={<Button aria-label="Cancel" />} />
                    </CardLayoutHeader>
                    <CardLayoutBody>
                        <label htmlFor="text">Link text</label>
                        <input id="text" />
                    </CardLayoutBody>
                    <CardLayoutFooter>
                        <Button>Apply</Button>
                    </CardLayoutFooter>
                </PopoverContent>
            </Popover>,
            { wrapper: UiProvider },
        );

        await user.click(screen.getByRole('button', { name: 'Edit link' }));

        const card = await screen.findByRole('dialog', { name: 'Link' });
        const [header, body, footer] = Array.from(card.children);

        expect(header).toHaveAttribute('data-slot', 'card-layout-header');
        expect(within(header as HTMLElement).getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
        expect(body).toHaveAttribute('data-slot', 'card-layout-body');
        expect(within(body as HTMLElement).getByLabelText('Link text')).toBeInTheDocument();
        expect(footer).toHaveAttribute('data-slot', 'card-layout-footer');
        expect(within(footer as HTMLElement).getByRole('button', { name: 'Apply' })).toBeInTheDocument();
    });

    it('takes further classes, as the other layer parts do', () => {
        render(<CardLayoutBody className="extra">Body</CardLayoutBody>, { wrapper: UiProvider });

        expect(screen.getByText('Body')).toHaveClass('extra');
    });
});
