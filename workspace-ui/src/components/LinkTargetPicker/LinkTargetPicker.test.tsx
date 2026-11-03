import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { LinkTargetPicker, type LinkTargetValue } from './LinkTargetPicker';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };
const WELCOME = { id: 9100, name: 'Willkommen', type: 'page', path: '/Corporate Website/' };
const AGB = { id: 9150, name: 'AGB', type: 'page', path: '/Corporate Website/Service/' };
const IMPRINT = { id: 9151, name: 'Impressum', type: 'page', path: '/Corporate Website/Service/' };
const PRIVACY = { id: 9160, name: 'Datenschutz', type: 'page', path: '/Corporate Website/Rechtliches/' };
const COOKIES = { id: 9161, name: 'Cookies', type: 'page', path: '/Corporate Website/Rechtliches/' };

// Answers the CMS requests of the picker: one node with the welcome page in its root folder 7, the
// page 9142 the link is on in folder 8 (next to AGB and Impressum), the page 9160 "Datenschutz" in
// folder 9 (next to Cookies); a search over the node finds AGB for "agb", nothing otherwise.
function stubCms() {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');
        const folders: Record<string, unknown[]> = { 7: [WELCOME], 8: [AGB, IMPRINT], 9: [PRIVACY, COOKIES] };
        const pages: Record<string, { name: string; folderId: number }> = { 9142: { name: 'Nutzungsbedingungen', folderId: 8 }, 9160: { name: 'Datenschutz', folderId: 9 } };

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        const folder = /^\/rest\/folder\/getItems\/(\d+)$/.exec(url.pathname)?.[1];

        if (folder !== undefined) {
            const search = url.searchParams.get('search') ?? '';

            if (search !== '') {
                return Response.json({ items: folder === '7' && search === 'agb' ? [AGB] : [] });
            }

            return Response.json({ items: folders[folder] ?? [] });
        }

        const pageId = /^\/rest\/page\/load\/(\d+)$/.exec(url.pathname)?.[1];
        const page = pageId === undefined ? undefined : pages[pageId];

        if (page) {
            return Response.json({ page: { id: Number(pageId), ...page, cdate: 0, edate: 0 } });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

// The folders whose items the picker asked for without a search text.
function listedFolders(fetchMock: ReturnType<typeof stubCms>): string[] {
    return fetchMock.mock.calls
        .map(([input]) => new URL(String(input), 'http://app.test'))
        .filter((url) => url.pathname.startsWith('/rest/folder/getItems/') && !url.searchParams.has('search'))
        .map((url) => `${url.pathname.split('/').pop()} ${url.searchParams.get('type')} ${url.searchParams.get('recursive') ?? 'flat'}`);
}

// The picker with its value kept as a link card keeps it; `onValueChange` sees every change.
function renderPicker(initial: LinkTargetValue, onValueChange = vi.fn(), options: { nodeId?: number; pageId?: number } = {}) {
    // An explicit `nodeId: undefined` means no node; left out, node 3.
    const nodeId = 'nodeId' in options ? options.nodeId : 3;
    const pageId = options.pageId ?? 9142;
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    function Picker() {
        const [value, setValue] = useState(initial);

        return <LinkTargetPicker nodeId={nodeId} pageId={pageId} value={value} onValueChange={(next) => { setValue(next); onValueChange(next); }} />;
    }

    render(<Picker />, { wrapper: Wrapper });

    return onValueChange;
}

describe('LinkTargetPicker', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows a URL target in its field and hands on what is typed', async () => {
        const user = userEvent.setup();

        stubCms();
        const onValueChange = renderPicker({ type: 'url', url: 'https://example.com' });

        expect(screen.getByRole('radio', { name: 'URL' })).toBeChecked();

        const field = screen.getByRole('textbox', { name: 'URL' });

        expect(field).toHaveValue('https://example.com');

        await user.clear(field);
        await user.type(field, '/agb');

        expect(onValueChange).toHaveBeenLastCalledWith({ type: 'url', url: '/agb' });
    });

    it('lists the folder of the CMS page the link points to, marks that page, and hands on another one picked from it', async () => {
        const user = userEvent.setup();

        const fetchMock = stubCms();
        const onValueChange = renderPicker({ type: 'page', page: { id: 9160 } });

        expect(screen.getByRole('radio', { name: 'CMS page' })).toBeChecked();
        // The folder's list, the picked page in it once (no extra row for it any more).
        expect(await screen.findByRole('radio', { name: /Cookies/ })).not.toBeChecked();
        expect(screen.getAllByRole('radio', { name: /Datenschutz/ })).toHaveLength(1);
        expect(screen.getByRole('radio', { name: /Datenschutz/ })).toBeChecked();
        expect(screen.queryByText('Willkommen')).not.toBeInTheDocument();
        expect(listedFolders(fetchMock)).toEqual(['9 page flat']);

        await user.click(screen.getByRole('radio', { name: /Cookies/ }));

        expect(onValueChange).toHaveBeenLastCalledWith({ type: 'page', page: { id: 9161, name: 'Cookies' } });
    });

    it('lists the folder of the page the link is on when the link points to no CMS page', async () => {
        const fetchMock = stubCms();

        renderPicker({ type: 'page', page: null });

        expect(await screen.findByRole('radio', { name: /AGB/ })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Impressum/ })).toBeInTheDocument();
        expect(screen.queryByText('Willkommen')).not.toBeInTheDocument();
        expect(listedFolders(fetchMock)).toEqual(['8 page flat']);
    });

    it('lists the node\'s root folder when the page whose folder it needs cannot be read', async () => {
        const fetchMock = stubCms();

        renderPicker({ type: 'page', page: null }, vi.fn(), { pageId: 9999 });

        expect(await screen.findByRole('radio', { name: /Willkommen/ })).toBeInTheDocument();
        expect(listedFolders(fetchMock)).toEqual(['7 page flat']);
    });

    it('says in the empty search field that it searches all pages', () => {
        stubCms();
        renderPicker({ type: 'page', page: null });

        expect(screen.getByRole('searchbox', { name: 'Search pages' })).toHaveAttribute('placeholder', 'Search all pages');
    });

    it('searches the node\'s pages for the typed text', async () => {
        const user = userEvent.setup();

        const fetchMock = stubCms();

        renderPicker({ type: 'page', page: null });
        await screen.findByRole('radio', { name: /Impressum/ });

        await user.type(screen.getByRole('searchbox', { name: 'Search pages' }), 'agb');

        await waitFor(() => expect(screen.queryByText('Impressum')).not.toBeInTheDocument());
        expect(await screen.findByText('AGB')).toBeInTheDocument();

        const searched = fetchMock.mock.calls.map(([input]) => new URL(String(input), 'http://app.test')).filter((url) => url.pathname === '/rest/folder/getItems/7');

        expect(searched.every((url) => url.searchParams.get('type') === 'page' && url.searchParams.get('nodeId') === '3')).toBe(true);
    });

    it('keeps what was given for each kind when switching between URL and page', async () => {
        const user = userEvent.setup();

        stubCms();
        const onValueChange = renderPicker({ type: 'url', url: 'https://example.com' });

        await user.click(screen.getByRole('radio', { name: 'CMS page' }));
        expect(onValueChange).toHaveBeenLastCalledWith({ type: 'page', page: null });

        await user.click(await screen.findByRole('radio', { name: /AGB/ }));
        await user.click(screen.getByRole('radio', { name: 'URL' }));
        expect(onValueChange).toHaveBeenLastCalledWith({ type: 'url', url: 'https://example.com' });

        await user.click(screen.getByRole('radio', { name: 'CMS page' }));
        expect(onValueChange).toHaveBeenLastCalledWith({ type: 'page', page: { id: 9150, name: 'AGB' } });
    });

    it('offers only a URL without a node', () => {
        stubCms();
        renderPicker({ type: 'url', url: '' }, vi.fn(), { nodeId: undefined });

        expect(screen.getByRole('radio', { name: 'URL' })).toBeInTheDocument();
        expect(screen.queryByRole('radio', { name: 'CMS page' })).not.toBeInTheDocument();
    });

    it('says when no page was found, and when the pages could not be loaded', async () => {
        const user = userEvent.setup();

        stubCms();
        renderPicker({ type: 'page', page: null });
        await screen.findByRole('radio', { name: /AGB/ });

        await user.type(screen.getByRole('searchbox', { name: 'Search pages' }), 'nichts');
        expect(await screen.findByText('No pages found.')).toBeInTheDocument();

        vi.stubGlobal('fetch', vi.fn<typeof fetch>(async () => new Response(null, { status: 500 })));
        await user.clear(screen.getByRole('searchbox', { name: 'Search pages' }));
        await user.type(screen.getByRole('searchbox', { name: 'Search pages' }), 'fehler');
        expect(await screen.findByText('The pages could not be loaded.')).toBeInTheDocument();
    });
});
