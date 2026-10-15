import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

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

    it('always shows a link that opens the read-only URL in a new tab', () => {
        render(<PreviewColumn {...noActions} preview={preview(1)} />, { wrapper: UiProvider });

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

        describe('on the dev server', () => {
            afterEach(() => {
                vi.unstubAllEnvs();
            });

            // `npm run dev` and `npm run dev:debug`.
            it.each(['development', 'debug'])('loads the preview from the dev server\'s origin, which forwards it to the CMS with the session cookie (mode %s)', (mode) => {
                vi.stubEnv('MODE', mode);

                expect(loadedUrl(BASE)).toBe(`${window.location.origin}/alohapage?realid=9142&nodeid=3&mode=view&real=view&tagmarkers=true`);
            });
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

describe('PreviewColumn with several pages', () => {
    const HOME_URL = 'https://cms.example.com/alohapage?realid=1&nodeid=3&mode=view';
    const NEWS_URL = 'https://cms.example.com/alohapage?realid=2&nodeid=3&mode=view';

    function page(seq: number, id: number, name: string, previewUrl: string): LivePreview {
        return { seq, cms_ref: { type: 'page', id, node_id: 3, name }, preview_url: previewUrl, mode: 'view' };
    }

    function frameUrl(): string | null {
        return screen.getByTitle('Preview of the page in the CMS').getAttribute('src');
    }

    async function pick(user: ReturnType<typeof userEvent.setup>, name: string) {
        await user.click(screen.getByRole('combobox', { name: 'Pages' }));
        await user.click(await screen.findByRole('option', { name }));
    }

    it('has no page select for a single page', () => {
        const home = page(1, 1, 'Home', HOME_URL);

        render(<PreviewColumn {...noActions} preview={home} previews={[home]} />, { wrapper: UiProvider });

        expect(screen.queryByRole('combobox', { name: 'Pages' })).not.toBeInTheDocument();
    });

    it('shows the latest page and switches to another one picked in the select', async () => {
        const user = userEvent.setup();
        const home = page(1, 1, 'Home', HOME_URL);
        const news = page(2, 2, 'News', NEWS_URL);

        render(<PreviewColumn {...noActions} preview={news} previews={[home, news]} />, { wrapper: UiProvider });

        expect(screen.getByRole('combobox', { name: 'Pages' })).toHaveTextContent('News');
        expect(frameUrl()).toContain('realid=2');

        await pick(user, 'Home');

        expect(screen.getByRole('combobox', { name: 'Pages' })).toHaveTextContent('Home');
        expect(frameUrl()).toContain('realid=1');
    });

    it('names a page without a name by its id', () => {
        const home = page(1, 1, 'Home', HOME_URL);
        const unnamed: LivePreview = { seq: 2, cms_ref: { type: 'page', id: 2, node_id: 3 }, preview_url: NEWS_URL, mode: 'view' };

        render(<PreviewColumn {...noActions} preview={unnamed} previews={[home, unnamed]} />, { wrapper: UiProvider });

        expect(screen.getByRole('combobox', { name: 'Pages' })).toHaveTextContent('Page 2');
    });

    it('shows the page of a newer preview again', async () => {
        const user = userEvent.setup();
        const home = page(1, 1, 'Home', HOME_URL);
        const news = page(2, 2, 'News', NEWS_URL);
        const { rerender } = render(<PreviewColumn {...noActions} preview={news} previews={[home, news]} />, { wrapper: UiProvider });

        await pick(user, 'Home');
        expect(frameUrl()).toContain('realid=1');

        const newerNews = page(3, 2, 'News', NEWS_URL);

        rerender(<PreviewColumn {...noActions} preview={newerNews} previews={[home, newerNews]} />);

        expect(screen.getByRole('combobox', { name: 'Pages' })).toHaveTextContent('News');
        expect(frameUrl()).toContain('realid=2');
    });
});
