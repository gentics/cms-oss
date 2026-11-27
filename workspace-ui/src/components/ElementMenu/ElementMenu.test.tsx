import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';

import { ElementMenu, type ElementMenuOffers } from './ElementMenu';

import '@/i18n';

function renderMenu(offers: ElementMenuOffers) {
    const handlers = { onFeedback: vi.fn(), onEditLink: vi.fn(), onSwapImage: vi.fn(), onWrite: vi.fn(), onReference: vi.fn() };
    const anchor = createRef<HTMLDivElement>();

    render(
        <>
            <div ref={anchor} />
            <ElementMenu open onOpenChange={() => {}} anchor={anchor} finalFocus={false} label="Paragraph · Text" offers={offers} {...handlers} />
        </>,
        { wrapper: UiProvider },
    );

    return handlers;
}

describe('ElementMenu', () => {
    it('offers feedback, writing and the chat for a text, and calls back', async () => {
        const user = userEvent.setup();
        const handlers = renderMenu({ link: false, image: false, write: true });

        expect(await screen.findByText('Paragraph · Text')).toBeInTheDocument();
        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual(['Give feedback', 'Write it yourself', 'Reference in the chat']);

        await user.click(screen.getByRole('menuitem', { name: 'Write it yourself' }));
        expect(handlers.onWrite).toHaveBeenCalledTimes(1);
    });

    it('offers only what it is told: editing a link, swapping an image, or neither', async () => {
        renderMenu({ link: true, image: false, write: false });
        expect((await screen.findAllByRole('menuitem')).map((item) => item.textContent)).toEqual(['Give feedback', 'Edit link', 'Reference in the chat']);
    });

    it('offers swapping an image', async () => {
        const user = userEvent.setup();
        const handlers = renderMenu({ link: false, image: true, write: false });

        await user.click(await screen.findByRole('menuitem', { name: 'Swap the image' }));
        expect(handlers.onSwapImage).toHaveBeenCalledTimes(1);
        expect(screen.queryByRole('menuitem', { name: 'Write it yourself' })).not.toBeInTheDocument();
    });
});
