import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { LivePreview } from '@/store/useWorkspaceEventStore';

import { PreviewColumn } from './PreviewColumn';

import '@/i18n';

const URL_9142 = 'https://cms.example.com/alohapage?realid=9142&nodeid=3&mode=view';
// What the column loads: the same preview, read-only for the CMS too (`readOnlyPreviewUrl`).
const READ_ONLY_9142 = `${URL_9142}&real=view&tagmarkers=true`;

function preview(seq: number, previewUrl = URL_9142): LivePreview {
    return { seq, cms_ref: { type: 'page', id: 9142, node_id: 3 }, preview_url: previewUrl, mode: 'view' };
}

// The URL the column loads for `previewUrl`.
function loadedUrl(previewUrl: string): string {
    render(<PreviewColumn {...noActions} preview={preview(1, previewUrl)} />, { wrapper: UiProvider });

    return screen.getByTitle('Preview of the page in the CMS').getAttribute('src')!;
}

// The column's actions on a part, unused here.
const noActions = { onReferenceInChat: () => {}, onSendFeedback: () => Promise.resolve(), onSendEdit: () => Promise.resolve(), onSendImage: () => Promise.resolve(), onSendSettings: () => Promise.resolve(), canSendFeedback: true };

describe('PreviewColumn', () => {
    it('frames the CMS preview read-only', () => {
        render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

        expect(screen.getByRole('heading', { name: 'Preview' })).toBeInTheDocument();
        expect(screen.getByTitle('Preview of the page in the CMS')).toHaveAttribute('src', READ_ONLY_9142);
    });

    it('always shows the read-only URL and a link that opens it in a new tab', () => {
        render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

        expect(screen.getByText(READ_ONLY_9142)).toBeInTheDocument();
        const open = screen.getByRole('link', { name: 'Open' });

        expect(open).toHaveAttribute('href', READ_ONLY_9142);
        expect(open).toHaveAttribute('target', '_blank');
        expect(open).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('reloads the frame on a new preview.updated with the same URL', () => {
        const { rerender } = render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });
        const first = screen.getByTitle('Preview of the page in the CMS');

        rerender(<PreviewColumn {...noActions} preview={preview(2)} />);

        expect(screen.getByTitle('Preview of the page in the CMS')).not.toBe(first);
    });

    describe('read-only URL', () => {
        const BASE = 'https://cms.example.com/alohapage?realid=9142&nodeid=3';

        it('adds mode=view, real=view and tagmarkers=true when they are missing', () => {
            expect(loadedUrl(BASE)).toBe(`${BASE}&mode=view&real=view&tagmarkers=true`);
        });

        it('returns a URL that already has all of them unchanged', () => {
            const url = `${BASE}&real=view&tagmarkers=true&mode=view`;

            expect(loadedUrl(url)).toBe(url);
        });

        it('turns an edit preview into a read-only one', () => {
            expect(loadedUrl(`${BASE}&mode=edit&real=edit`)).toBe(`${BASE}&mode=view&real=view&tagmarkers=true`);
        });

        it('keeps the other parameters, the path and the fragment', () => {
            const url = new URL(loadedUrl('https://cms.example.com/prefix/alohapage?realid=1&links=frontend#top'));

            expect(url.pathname).toBe('/prefix/alohapage');
            expect(url.hash).toBe('#top');
            expect(Object.fromEntries(url.searchParams)).toEqual({ realid: '1', links: 'frontend', mode: 'view', real: 'view', tagmarkers: 'true' });
        });
    });

    describe('width and full screen', () => {
        it('starts at desktop width', () => {
            render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

            expect(screen.getByRole('group', { name: 'Preview width' })).toBeInTheDocument();
            expect(screen.getByRole('button', { name: 'Desktop' })).toHaveAttribute('aria-pressed', 'true');
            expect(screen.getByRole('button', { name: 'Mobile · 390 px' })).toHaveAttribute('aria-pressed', 'false');
        });

        it('switches between mobile and desktop width', async () => {
            const user = userEvent.setup();

            render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

            await user.click(screen.getByRole('button', { name: 'Mobile · 390 px' }));
            expect(screen.getByRole('button', { name: 'Mobile · 390 px' })).toHaveAttribute('aria-pressed', 'true');
            expect(screen.getByRole('button', { name: 'Desktop' })).toHaveAttribute('aria-pressed', 'false');

            await user.click(screen.getByRole('button', { name: 'Desktop' }));
            expect(screen.getByRole('button', { name: 'Desktop' })).toHaveAttribute('aria-pressed', 'true');
        });

        it('keeps the width when the preview reloads', async () => {
            const user = userEvent.setup();
            const { rerender } = render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

            await user.click(screen.getByRole('button', { name: 'Mobile · 390 px' }));
            rerender(<PreviewColumn {...noActions} preview={preview(2)} />);

            expect(screen.getByRole('button', { name: 'Mobile · 390 px' })).toHaveAttribute('aria-pressed', 'true');
        });

        it('enters full screen and leaves it with the same button', async () => {
            const user = userEvent.setup();

            render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

            await user.click(screen.getByRole('button', { name: 'Full screen' }));
            expect(screen.getByRole('button', { name: 'Leave full screen' })).toHaveAttribute('aria-pressed', 'true');

            await user.click(screen.getByRole('button', { name: 'Leave full screen' }));
            expect(screen.getByRole('button', { name: 'Full screen' })).toHaveAttribute('aria-pressed', 'false');
        });

        it('leaves full screen with Escape', async () => {
            const user = userEvent.setup();

            render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

            await user.click(screen.getByRole('button', { name: 'Full screen' }));
            await user.keyboard('{Escape}');

            expect(screen.getByRole('button', { name: 'Full screen' })).toHaveAttribute('aria-pressed', 'false');
        });
    });
});
