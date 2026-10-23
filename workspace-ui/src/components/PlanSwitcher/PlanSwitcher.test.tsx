import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { Plan, PlanStatus } from '@/services/apiService/genaix/types';

import { PlanSwitcher } from './PlanSwitcher';

import '@/i18n';

function plan(id: string, status: PlanStatus, summary: string): Plan {
    return { id, workflows: [{ id: 'content_create', version: '1.0.0' }], status, version: 1, summary, items: [] };
}

const DONE = plan('pl-1', 'done', 'New terms page');
const PROPOSED = plan('pl-2', 'proposed', 'Colour the teaser');

describe('PlanSwitcher', () => {
    it('shows nothing with one open plan', () => {
        const { container } = render(<PlanSwitcher plans={[DONE]} currentId="pl-1" onPick={vi.fn()} />, { wrapper: UiProvider });

        expect(container).toBeEmptyDOMElement();
    });

    it('offers the open plans, the shown one checked, and picks another', async () => {
        const user = userEvent.setup();
        const onPick = vi.fn();

        render(<PlanSwitcher plans={[DONE, PROPOSED]} currentId="pl-2" onPick={onPick} />, { wrapper: UiProvider });
        await user.click(screen.getByRole('button', { name: '2 open plans' }));

        const items = await screen.findAllByRole('menuitemradio');

        expect(items.map((item) => item.textContent)).toEqual(['New terms pageDone', 'Colour the teaserProposed']);
        expect(screen.getByRole('menuitemradio', { name: /Colour the teaser/ })).toHaveAttribute('aria-checked', 'true');

        await user.click(screen.getByRole('menuitemradio', { name: /New terms page/ }));

        expect(onPick).toHaveBeenCalledWith('pl-1');
    });
});
