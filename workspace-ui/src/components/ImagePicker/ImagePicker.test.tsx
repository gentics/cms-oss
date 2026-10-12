import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { CmsItem } from '@/services/cmsApiService/cmsApiService';
import { createWrapper } from '@/test/renderWithProviders';

import { ImagePicker } from './ImagePicker';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };
const WORKSHOP = { id: 4411, name: 'team-workshop-2026.jpg', type: 'image' };
const DESK = { id: 4412, name: 'editorial-desk.jpg', type: 'image' };

// Answers the CMS requests of the picker: one node, both images without a search text, the
// workshop image for "team", nothing otherwise. `images` replaces the image search's answer.
function stubCms(images?: Response) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        if (url.pathname === '/rest/folder/getItems/7') {
            if (images) {
                return images;
            }

            const search = url.searchParams.get('search') ?? '';

            return Response.json({ items: search === '' ? [WORKSHOP, DESK] : (search === 'team' ? [WORKSHOP] : []) });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderPicker(value: CmsItem | null = null, onValueChange = vi.fn()) {
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<ImagePicker nodeId={3} value={value} onValueChange={onValueChange} />, { wrapper: Wrapper });

    return onValueChange;
}

describe('ImagePicker', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the node\'s images without a search text, each as the CMS serves it, searching images only', async () => {
        const fetchMock = stubCms();

        renderPicker();

        expect(await screen.findByRole('img', { name: 'team-workshop-2026.jpg' })).toHaveAttribute('src', '/rest/file/content/load/4411?nodeId=3');
        expect(screen.getByRole('img', { name: 'editorial-desk.jpg' })).toHaveAttribute('src', '/rest/file/content/load/4412?nodeId=3');

        const search = fetchMock.mock.calls.map(([input]) => new URL(String(input), 'http://app.test')).find((url) => url.pathname === '/rest/folder/getItems/7')!;

        expect(search.searchParams.get('nodeId')).toBe('3');
        expect(search.searchParams.get('search')).toBe('');
        expect(search.searchParams.getAll('type')).toEqual(['image']);
    });

    it('searches the typed text', async () => {
        const user = userEvent.setup();

        stubCms();
        renderPicker();
        await screen.findByRole('img', { name: 'editorial-desk.jpg' });

        await user.type(screen.getByRole('searchbox', { name: 'Search images' }), 'team');

        // The grid keeps the last result until the search has run.
        await waitFor(() => expect(screen.queryByRole('img', { name: 'editorial-desk.jpg' })).not.toBeInTheDocument());
        expect(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' })).toBeInTheDocument();
    });

    it('hands on the picked image and marks the one it is given', async () => {
        const user = userEvent.setup();

        stubCms();
        const onValueChange = renderPicker({ id: 4412, name: 'editorial-desk.jpg', type: 'image' });

        expect(await screen.findByRole('radio', { name: 'editorial-desk.jpg' })).toBeChecked();

        await user.click(screen.getByRole('radio', { name: 'team-workshop-2026.jpg' }));

        expect(onValueChange).toHaveBeenCalledWith(WORKSHOP);
    });

    it('says when nothing was found, and when the images could not be loaded', async () => {
        const user = userEvent.setup();

        stubCms();
        renderPicker();
        await screen.findByRole('img', { name: 'editorial-desk.jpg' });
        await user.type(screen.getByRole('searchbox', { name: 'Search images' }), 'nothing');

        expect(await screen.findByText('No images found.')).toHaveAttribute('role', 'status');

        vi.unstubAllGlobals();
        stubCms(new Response(null, { status: 500 }));
        renderPicker();

        expect(await screen.findByText('The images could not be loaded.')).toBeInTheDocument();
    });
});
