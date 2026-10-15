import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import type { Check, Publication } from '@/services/apiService/genaix/types';
import { renderWithProviders } from '@/test/renderWithProviders';

import { ReviewPages } from './ReviewPages';

const SESSION = 's-1';

function publication(id: number, name: string | undefined, status: Publication['status']): Publication {
    return { cms_ref: { type: 'page', id, node_id: 3, ...(name && { name }) }, status, approval_chain: [] };
}

const PUBLICATIONS = [
    publication(8644, 'Versand und Lieferung', 'published'),
    publication(9011, 'Serviceinformationen', 'review_requested'),
    publication(8871, undefined, 'pending'),
];

function check(id: string, severity: Check['severity'], pageId?: number): Check {
    return { id, label: id, severity, blocking: false, message: '', source: { type: 'gate' }, ...(pageId && { target: { type: 'page', id: String(pageId) } }) };
}

const CHECKS = [
    check('chk-1', 'fail', 9011),
    check('chk-2', 'warn', 9011),
    check('chk-3', 'pass', 8644),
    // About the whole plan: counted on no page.
    check('chk-4', 'fail'),
];

describe('ReviewPages', () => {
    it('lists each page with its publish status and its open issues, the shown one current', async () => {
        renderWithProviders(<ReviewPages sessionId={SESSION} publications={PUBLICATIONS} checks={CHECKS} selectedId={9011} />, { path: `/sessions/${SESSION}/review` });

        const pages = await screen.findByRole('navigation', { name: 'Pages' });
        const links = within(pages).getAllByRole('link');

        expect(links.map((link) => link.textContent)).toEqual([
            'Versand und LieferungPublished',
            'ServiceinformationenReview requested2 open issues',
            'Page 8871Not published',
        ]);
        expect(within(pages).getByRole('link', { name: /Serviceinformationen/ })).toHaveAttribute('aria-current', 'page');
        expect(within(pages).getByRole('link', { name: /Versand/ })).not.toHaveAttribute('aria-current');
    });

    it('puts the page picked into the URL, keeping the plan', async () => {
        const user = userEvent.setup();
        const { router } = renderWithProviders(
            <ReviewPages sessionId={SESSION} publications={PUBLICATIONS} checks={CHECKS} selectedId={8644} />,
            { path: `/sessions/${SESSION}/review?plan=pl-2` },
        );

        await user.click(await screen.findByRole('link', { name: /Serviceinformationen/ }));

        expect(router.state.location.pathname).toBe(`/sessions/${SESSION}/review`);
        expect(router.state.location.search).toEqual({ plan: 'pl-2', page: 9011 });
    });
});
