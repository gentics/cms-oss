import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { type Editor, useEditor } from '@tiptap/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';
import { ComposerTextInput } from '@/components/ComposerTextInput/ComposerTextInput';
import { readParts } from '@/helper/composerParts/composerParts';
import { createWrapper } from '@/test/renderWithProviders';

import { ReferenceMenu } from './ReferenceMenu';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };
const UPLOAD = {
    id: '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02',
    kind: 'upload',
    name: 'brief.pdf',
    media_type: 'application/pdf',
    size: 1_048_576,
    mode: 'verbatim',
    sha256: 'x',
    created_at: '2026-10-07T08:00:00Z',
    created_by: 'user',
    download_url: '/x',
};

// Answers the CMS and GenAIx requests of the menu: one node, a search that finds a page and a
// folder for "Camp" and nothing else, and one uploaded file.
function stubBackend() {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        if (url.pathname === '/rest/folder/getItems/7') {
            const found = url.searchParams.get('search')?.toLowerCase().startsWith('camp')
                ? [{ id: 8871, name: 'Campaign launch', type: 'page', path: '/Corporate Website/Campaigns/' }, { id: 42, name: 'Campaigns', type: 'folder' }]
                : [];

            return Response.json({ items: found });
        }

        if (url.pathname.endsWith('/sessions/s-1/files')) {
            return Response.json({ items: [UPLOAD], next_cursor: null });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function Field({ onEditor, sessionId }: { onEditor: (editor: Editor) => void; sessionId?: string }) {
    const editor = useEditor({ extensions: composerExtensions });

    onEditor(editor);

    return (
        <>
            <ComposerTextInput editor={editor} label="What should happen?" isEmpty={false} size="base" onKeyDown={() => {}} onFocus={() => {}} onBlur={() => {}} />
            <ReferenceMenu editor={editor} sessionId={sessionId} />
        </>
    );
}

function renderField(sessionId?: string) {
    let editor: Editor | undefined;
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<Field sessionId={sessionId} onEditor={(created) => { editor = created; }} />, { wrapper: Wrapper });

    return { field: screen.getByRole('textbox', { name: 'What should happen?' }), parts: () => readParts(editor!.state.doc) };
}

describe('ReferenceMenu', () => {
    beforeEach(() => {
        stubBackend();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('groups what the CMS finds for the text after @, and Enter inserts the active entry', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('Use @Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(await within(list).findByText('Pages · 1')).toBeInTheDocument();
        expect(within(list).getByText('Folders · 1')).toBeInTheDocument();
        expect(field).toHaveAttribute('aria-expanded', 'true');
        expect(field).toHaveAttribute('aria-activedescendant', within(list).getAllByRole('option')[0]!.id);

        await user.keyboard('{ArrowDown}{Enter}');

        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(parts()).toEqual([
            { type: 'text', text: 'Use ' },
            { type: 'reference', ref: { type: 'folder', id: '42', node_id: 3, label: 'Campaigns' } },
        ]);
    });

    it('offers the session files and the project without a query; a click inserts the file', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField('s-1');

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(await within(list).findByRole('option', { name: /Corporate Website/ })).toBeInTheDocument();

        await user.click(await within(list).findByRole('option', { name: /brief\.pdf/ }));

        expect(parts()).toEqual([{ type: 'file_ref', file_id: UPLOAD.id, mode: 'verbatim' }]);
    });

    it('narrows to a group with a typed prefix, and the filter chip takes the prefix out again', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@page:Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(await within(list).findByText('Pages · 1')).toBeInTheDocument();
        expect(within(list).queryByText('Folders · 1')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Remove filter' }));

        expect(field).toHaveTextContent('@Camp');
        expect(await within(list).findByText('Folders · 1')).toBeInTheDocument();
    });

    it('narrows to the active entry\'s group with Tab', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await within(list).findByText('Folders · 1');
        await user.keyboard('{End}{Tab}');

        await waitFor(() => expect(within(list).queryByText('Pages · 1')).not.toBeInTheDocument());
        expect(within(list).getByText('Folders · 1')).toBeInTheDocument();
    });

    it('keeps the query as text when nothing is found', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('@zzz');

        await user.click(await screen.findByRole('button', { name: '“zzz” as plain text' }));

        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(parts()).toEqual([{ type: 'text', text: '@zzz' }]);
    });

    it('closes on Esc and keeps the text', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('@Camp');
        await screen.findByRole('listbox', { name: 'Context' });
        await user.keyboard('{Escape}');

        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(field).not.toHaveAttribute('aria-expanded');
        expect(parts()).toEqual([{ type: 'text', text: '@Camp' }]);
    });
});
