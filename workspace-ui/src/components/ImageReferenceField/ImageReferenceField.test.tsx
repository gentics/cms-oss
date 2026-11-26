import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';
import { stubUploads } from '@/test/stubUploads';

import { ImageReferenceField, type ImageReferenceType } from './ImageReferenceField';

import '@/i18n';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };
const WORKSHOP = { id: 4411, name: 'team-workshop-2026.jpg', type: 'image' };

// The session (with the node it works in, if any) and the CMS: one node with one image.
function stubSessionAndCms(context: { node_id?: number } = { node_id: 3 }) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', status: 'active', context });
        }

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        if (url.pathname === '/rest/folder/getItems/7') {
            return Response.json({ items: [WORKSHOP] });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderField(types: ImageReferenceType[] = ['image', 'session_file'], disabled = false) {
    const onChange = vi.fn();
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(
        <>
            <span id="label">Header image</span>
            <ImageReferenceField sessionId="s-1" types={types} onChange={onChange} labelledBy="label" disabled={disabled} />
        </>,
        { wrapper: Wrapper },
    );

    return onChange;
}

describe('ImageReferenceField', () => {
    // jsdom has no object URLs; the tile of an uploaded image shows it from one.
    beforeEach(() => {
        vi.stubGlobal('URL', Object.assign(class extends URL {}, { createObjectURL: vi.fn(() => 'blob:hero'), revokeObjectURL: vi.fn() }));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('is named by its label and offers the images of the node the session works in', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const onChange = renderField();

        expect(screen.getByRole('group', { name: 'Header image' })).toBeInTheDocument();

        await user.click(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' }));

        expect(onChange).toHaveBeenLastCalledWith({ type: 'image', id: '4411', node_id: 3, label: 'team-workshop-2026.jpg' });
    });

    it('uploads a picked file into the session as a source, then names it as a session file', async () => {
        const user = userEvent.setup();
        let release: () => void = () => {};
        const hold = new Promise<void>((resolve) => {
            release = resolve;
        });

        stubSessionAndCms();
        const uploads = stubUploads([{ status: 201, body: { id: 'f-1' }, progress: [0.4], hold }]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));

        // While it uploads: the share sent, and no value.
        expect(await screen.findByRole('status')).toHaveTextContent('Uploading the image … 40 %');
        expect(onChange).toHaveBeenLastCalledWith(undefined);
        expect(uploads[0]).toMatchObject({ method: 'POST', url: '/rest/proxy/genaix/sessions/s-1/files' });
        expect(uploads[0]!.body.get('mode')).toBe('source');

        release();

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' }));
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('says why an upload failed and leaves the field without a value', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        stubUploads([{ status: 415, body: { type: 't', title: 't', status: 415, genaix_code: 'unsupported_media_type' } }]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));

        expect(await screen.findByRole('alert')).toHaveTextContent('The image could not be uploaded. This file type is not supported.');
        expect(onChange).not.toHaveBeenCalledWith(expect.objectContaining({ type: 'session_file' }));
    });

    it('offers only the upload when the session works in no node', async () => {
        stubSessionAndCms({});
        renderField();

        expect(await screen.findByLabelText('Upload image')).toBeInTheDocument();
        expect(screen.queryByRole('searchbox', { name: 'Search images' })).not.toBeInTheDocument();
    });

    it('offers no upload where only a CMS image may be named', async () => {
        stubSessionAndCms();
        renderField(['image']);

        expect(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' })).toBeInTheDocument();
        expect(screen.queryByLabelText('Upload image')).not.toBeInTheDocument();
    });

    it('names a file uploaded before again when its tile is picked again, without uploading it a second time', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const uploads = stubUploads([{ status: 201, body: { id: 'f-1' } }, { status: 201, body: { id: 'f-2' } }]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' }));

        await user.click(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' }));
        expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'image', id: '4411' }));

        await user.click(screen.getByRole('radio', { name: 'hero.png' }));

        expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' });
        expect(uploads).toHaveLength(1);
    });

    it('waits for a file still uploading when its tile is picked again, without uploading it a second time', async () => {
        const user = userEvent.setup();
        let release: () => void = () => {};
        const hold = new Promise<void>((resolve) => {
            release = resolve;
        });

        stubSessionAndCms();
        const uploads = stubUploads([{ status: 201, body: { id: 'f-1' }, progress: [0.5], hold }, { status: 201, body: { id: 'f-2' } }]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));
        await user.click(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' }));
        await user.click(screen.getByRole('radio', { name: 'hero.png' }));

        expect(onChange).toHaveBeenLastCalledWith(undefined);
        expect(uploads).toHaveLength(1);

        release();

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' }));
        expect(uploads).toHaveLength(1);
    });

    it('uploads a file again when its tile is picked again after its upload failed', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const uploads = stubUploads([
            { status: 415, body: { type: 't', title: 't', status: 415, genaix_code: 'unsupported_media_type' } },
            { status: 201, body: { id: 'f-2' } },
        ]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));
        expect(await screen.findByRole('alert')).toBeInTheDocument();

        await user.click(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' }));
        await user.click(screen.getByRole('radio', { name: 'hero.png' }));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-2', label: 'hero.png' }));
        expect(uploads).toHaveLength(2);
    });

    it('uploads a file again when its tile is clicked right after its upload failed', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const uploads = stubUploads([
            { status: 415, body: { type: 't', title: 't', status: 415, genaix_code: 'unsupported_media_type' } },
            { status: 201, body: { id: 'f-2' } },
        ]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], 'hero.png', { type: 'image/png' }));
        expect(await screen.findByRole('alert')).toBeInTheDocument();

        await user.click(screen.getByRole('radio', { name: 'hero.png' }));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-2', label: 'hero.png' }));
        expect(uploads).toHaveLength(2);
    });

    it('speaks of a file, not an image, where only an upload may be named', async () => {
        const user = userEvent.setup();
        let release: () => void = () => {};
        const hold = new Promise<void>((resolve) => {
            release = resolve;
        });

        stubSessionAndCms({});
        stubUploads([{ status: 415, body: { type: 't', title: 't', status: 415, genaix_code: 'unsupported_media_type' }, progress: [0.5], hold }]);
        renderField(['session_file']);

        expect(await screen.findByRole('radiogroup', { name: 'Choose a file' })).toBeInTheDocument();

        await user.upload(screen.getByLabelText('Upload file'), new File(['pdf'], 'briefing.pdf', { type: 'application/pdf' }));

        expect(await screen.findByRole('status')).toHaveTextContent('Uploading the file … 50 %');

        release();

        expect(await screen.findByRole('alert')).toHaveTextContent('The file could not be uploaded.');
    });

    it('names a file with a long name by at most 512 characters, as the contract allows a label', async () => {
        const user = userEvent.setup();
        const name = `${'a'.repeat(596)}.png`;

        stubSessionAndCms();
        stubUploads([{ status: 201, body: { id: 'f-1' } }]);
        const onChange = renderField();

        await user.upload(await screen.findByLabelText('Upload image'), new File(['png'], name, { type: 'image/png' }));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'session_file', id: 'f-1' })));

        const { label } = onChange.mock.lastCall![0] as { label: string };

        expect(label).toHaveLength(512);
        expect(label).toBe(`${name.slice(0, 511)}…`);
    });

    it('names the same file chosen again from the device by its earlier upload, but uploads it again once it changed', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const uploads = stubUploads([{ status: 201, body: { id: 'f-1' } }, { status: 201, body: { id: 'f-2' } }]);
        const onChange = renderField();
        const choose = async (file: File) => user.upload(screen.getByLabelText('Upload image'), file);

        await screen.findByLabelText('Upload image');
        await choose(new File(['png'], 'hero.png', { type: 'image/png', lastModified: 1_000 }));
        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' }));

        await user.click(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' }));
        await choose(new File(['png'], 'hero.png', { type: 'image/png', lastModified: 1_000 }));

        expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-1', label: 'hero.png' });
        expect(uploads).toHaveLength(1);

        // Edited and saved again: another date, another file.
        await choose(new File(['png, edited'], 'hero.png', { type: 'image/png', lastModified: 2_000 }));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-2', label: 'hero.png' }));
        expect(uploads).toHaveLength(2);
    });

    it('says why there are no CMS images where an upload may be named too but the session works in no node', async () => {
        stubSessionAndCms({});
        renderField(['image', 'session_file']);

        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('This session works in no CMS node, so you can only upload an image.'));
        expect(screen.getByLabelText('Upload image')).toBeInTheDocument();
    });

    it('says while the session loads, and when it could not be loaded, where only a CMS image may be named', async () => {
        let answerSession: (response: Response) => void = () => {};

        vi.stubGlobal('fetch', vi.fn<typeof fetch>((input) => {
            if (new URL(String(input), 'http://app.test').pathname === '/rest/proxy/genaix/sessions/s-1') {
                return new Promise<Response>((resolve) => {
                    answerSession = resolve;
                });
            }

            return Promise.resolve(new Response(null, { status: 404 }));
        }));
        renderField(['image']);

        expect(await screen.findByRole('status')).toHaveTextContent('Loading images …');

        answerSession(Response.json({ type: 't', title: 't', status: 404, genaix_code: 'session_not_found' }, { status: 404 }));

        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('The images could not be loaded.'));
    });

    it('still takes only an image where a CMS image may be named too', async () => {
        const user = userEvent.setup({ applyAccept: false });

        stubSessionAndCms();
        const uploads = stubUploads([{ status: 201, body: { id: 'f-1' } }]);
        const onChange = renderField(['image', 'session_file']);

        await user.upload(await screen.findByLabelText('Upload image'), new File(['pdf'], 'briefing.pdf', { type: 'application/pdf' }));

        expect(screen.getByRole('alert')).toHaveTextContent('This file type is not supported: application/pdf');
        expect(uploads).toHaveLength(0);
        expect(onChange).not.toHaveBeenCalled();
    });

    it('says so where only a CMS image may be named but the session works in no node', async () => {
        stubSessionAndCms({});
        renderField(['image']);

        // First "Loading images …" while the session loads, then why there is nothing to pick.
        await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('This session works in no CMS node, so there are no images to choose from.'));
    });

    it('takes any file a session upload takes where only an upload may be named', async () => {
        const user = userEvent.setup();

        stubSessionAndCms({});
        stubUploads([{ status: 201, body: { id: 'f-3' } }]);
        const onChange = renderField(['session_file']);

        await user.upload(await screen.findByLabelText('Upload file'), new File(['pdf'], 'briefing.pdf', { type: 'application/pdf' }));

        await waitFor(() => expect(onChange).toHaveBeenLastCalledWith({ type: 'session_file', id: 'f-3', label: 'briefing.pdf' }));
    });

    it('takes no choice while the card is locked', async () => {
        stubSessionAndCms();
        renderField(['image', 'session_file'], true);

        expect(await screen.findByRole('radio', { name: 'team-workshop-2026.jpg' })).toHaveAttribute('aria-disabled', 'true');
    });
});
