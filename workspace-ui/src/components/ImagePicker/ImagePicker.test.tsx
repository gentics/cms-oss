import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

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

    it('lists all of the node\'s images without a search text, each as the CMS serves it, searching images only', async () => {
        const fetchMock = stubCms();

        renderPicker();

        expect(await screen.findByRole('img', { name: 'team-workshop-2026.jpg' })).toHaveAttribute('src', '/rest/file/content/load/4411?nodeId=3');
        expect(screen.getByRole('img', { name: 'editorial-desk.jpg' })).toHaveAttribute('src', '/rest/file/content/load/4412?nodeId=3');

        const search = fetchMock.mock.calls.map(([input]) => new URL(String(input), 'http://app.test')).find((url) => url.pathname === '/rest/folder/getItems/7')!;

        expect(search.searchParams.get('nodeId')).toBe('3');
        expect(search.searchParams.has('search')).toBe(false);
        expect(search.searchParams.get('recursive')).toBe('true');
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

    describe('upload', () => {
        // jsdom has no object URLs; the tile of an uploaded image shows it from one.
        beforeEach(() => {
            vi.stubGlobal('URL', Object.assign(class extends URL {}, { createObjectURL: vi.fn(() => 'blob:hero'), revokeObjectURL: vi.fn() }));
        });

        it('offers the upload tile also when the node has no images', async () => {
            stubCms(Response.json({ items: [] }));
            renderPicker();

            expect(await screen.findByText('No images found.')).toBeInTheDocument();
            expect(screen.getByLabelText('Upload image')).toHaveAttribute('type', 'file');
        });

        it('takes an image from the device, shows it as a picked tile and hands it on', async () => {
            const user = userEvent.setup();
            const hero = new File(['png'], 'hero.png', { type: 'image/png' });

            stubCms();
            const onValueChange = renderPicker();

            await user.upload(screen.getByLabelText('Upload image'), hero);

            expect(onValueChange).toHaveBeenCalledWith(hero);
            expect(screen.getByRole('img', { name: 'hero.png' })).toHaveAttribute('src', 'blob:hero');
            expect(screen.getByRole('radio', { name: 'hero.png' })).toBeInTheDocument();
        });

        it('refuses a file that is not an image, and one over the upload limit, saying why', async () => {
            const user = userEvent.setup({ applyAccept: false });
            const large = new File(['png'], 'large.png', { type: 'image/png' });

            Object.defineProperty(large, 'size', { value: 30 * 1_048_576 });
            stubCms();
            const onValueChange = renderPicker();

            await user.upload(screen.getByLabelText('Upload image'), new File(['pdf'], 'terms.pdf', { type: 'application/pdf' }));

            expect(screen.getByRole('alert')).toHaveTextContent('This file type is not supported: application/pdf');

            await user.upload(screen.getByLabelText('Upload image'), large);

            expect(screen.getByRole('alert')).toHaveTextContent('It is 30 MB; files can be up to 25 MB.');
            expect(onValueChange).not.toHaveBeenCalled();
            expect(screen.queryByRole('radio', { name: /terms|large/ })).not.toBeInTheDocument();
        });
    });
});
