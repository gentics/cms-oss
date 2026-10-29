import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { ImageGridPart } from '@/services/apiService/genaix/types';

import { ImageGridPartView } from './ImageGridPartView';

import '@/i18n';

const items: ImageGridPart['items'] = [
    { id: 'img-1', url: '/img/1.jpg', alt: 'Campaign visual', selected: true },
    { id: 'img-2', url: '/img/2.jpg', alt: 'Team photo' },
];

function renderGrid(multi: boolean) {
    return render(<ImageGridPartView part={{ type: 'image_grid', label: 'Hero image', items, multi }} sessionId="s-1" />, { wrapper: UiProvider });
}

// The tile around an image, which carries the selected state.
function tileOf(name: string) {
    return screen.getByRole('img', { name }).closest('label')!;
}

describe('ImageGridPartView', () => {
    it('shows the selected image as selected, and moves the one selection on click', async () => {
        const user = userEvent.setup();

        renderGrid(false);

        expect(tileOf('Campaign visual')).toHaveAttribute('data-selected');
        expect(tileOf('Team photo')).not.toHaveAttribute('data-selected');

        await user.click(screen.getByRole('img', { name: 'Team photo' }));

        expect(screen.getByRole('radio', { name: 'Team photo' })).toBeChecked();
        expect(tileOf('Team photo')).toHaveAttribute('data-selected');
        expect(tileOf('Campaign visual')).not.toHaveAttribute('data-selected');
    });

    it('keeps several images selected when multi', async () => {
        const user = userEvent.setup();

        renderGrid(true);

        await user.click(screen.getByRole('img', { name: 'Team photo' }));

        expect(screen.getByRole('checkbox', { name: 'Campaign visual' })).toBeChecked();
        expect(screen.getByRole('checkbox', { name: 'Team photo' })).toBeChecked();
        expect(screen.getByText('2 selected')).toBeInTheDocument();
    });
});
