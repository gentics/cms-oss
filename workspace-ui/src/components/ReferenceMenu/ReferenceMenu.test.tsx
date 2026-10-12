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

// The objects of the node: on its root level a page and a folder, below it a page and a folder
// that "Camp" finds.
const ROOT_ITEMS = [{ id: 1, name: 'Home', type: 'page' }, { id: 2, name: 'News', type: 'folder', path: '/Corporate Website/' }];
const FOUND_ITEMS = [{ id: 8871, name: 'Campaign launch', type: 'page', path: '/Corporate Website/Campaigns/' }, { id: 42, name: 'Campaigns', type: 'folder' }];

// While set, CMS searches for text wait for it before they answer.
let searchesHeld: Promise<void> | null = null;

// Answers the CMS and GenAIx requests of the menu: one node with `ROOT_ITEMS` and `FOUND_ITEMS`,
// and one uploaded file.
function stubBackend() {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        if (url.pathname === '/rest/folder/getItems/7' || url.pathname === '/rest/folder/getFolders/7') {
            const search = url.searchParams.get('search');

            if (search !== null && searchesHeld) {
                await searchesHeld;
            }
            const listed = search === null ? ROOT_ITEMS : FOUND_ITEMS.filter(() => search.toLowerCase().startsWith('camp'));
            const type = url.searchParams.get('type') ?? 'folder';
            const items = listed.filter((item) => item.type === type);

            return Response.json(type === 'folder' ? { folders: items } : { items });
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

// The CMS searches for text made so far, as `type:search`.
function searches(fetchMock: ReturnType<typeof stubBackend>): string[] {
    return fetchMock.mock.calls
        .map(([input]) => new URL(String(input), 'http://app.test'))
        .filter((url) => url.pathname.startsWith('/rest/folder/') && url.searchParams.has('search'))
        .map((url) => `${url.searchParams.get('type') ?? 'folder'}:${url.searchParams.get('search')}`);
}

function renderField(sessionId?: string) {
    let editor: Editor | undefined;
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<Field sessionId={sessionId} onEditor={(created) => { editor = created; }} />, { wrapper: Wrapper });

    return { field: screen.getByRole('textbox', { name: 'What should happen?' }), parts: () => readParts(editor!.state.doc) };
}

describe('ReferenceMenu', () => {
    let fetchMock: ReturnType<typeof stubBackend>;

    beforeEach(() => {
        fetchMock = stubBackend();
    });

    afterEach(() => {
        searchesHeld = null;
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

    it('offers the groups for a bare @; a click on one shows what it holds, a click there inserts it', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField('s-1');

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(within(list).getAllByRole('option').map((option) => option.textContent)).toEqual(['Pages', 'Folders', 'Images', 'Files', 'Project']);
        expect(screen.queryByText('No context found.')).not.toBeInTheDocument();

        await user.click(within(list).getByRole('option', { name: 'Files' }));
        await user.click(await within(list).findByRole('option', { name: /brief\.pdf/ }));

        expect(parts()).toEqual([{ type: 'file_ref', file_id: UPLOAD.id, mode: 'verbatim' }]);
    });

    it('offers no Files group without a session', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(within(list).getAllByRole('option').map((option) => option.textContent)).toEqual(['Pages', 'Folders', 'Images', 'Project']);
    });

    it('shows the root level of the group chosen with the keyboard, and Backspace goes back to the groups', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await user.keyboard('{ArrowDown}{Enter}');

        expect(screen.getByRole('button', { name: 'Remove filter' }).parentElement).toHaveTextContent('Folders');
        expect(await within(list).findByText('Folders · 1')).toBeInTheDocument();
        expect(within(list).getByRole('option', { name: /News/ })).toBeInTheDocument();
        expect(within(list).queryByRole('option', { name: /Home/ })).not.toBeInTheDocument();

        await user.keyboard('{Backspace}');

        expect(within(list).getAllByRole('option')).toHaveLength(4);
        expect(parts()).toEqual([{ type: 'text', text: '@' }]);
    });

    it('goes into a group with the right arrow and back to the groups with the left one', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await user.keyboard('{ArrowRight}');

        expect(screen.getByRole('button', { name: 'Remove filter' }).parentElement).toHaveTextContent('Pages');
        expect(await within(list).findByRole('option', { name: /Home/, selected: true })).toBeInTheDocument();

        await user.keyboard('{ArrowLeft}');

        expect(screen.queryByRole('button', { name: 'Remove filter' })).not.toBeInTheDocument();
        expect(within(list).getAllByRole('option').map((option) => option.textContent)).toEqual(['Pages', 'Folders', 'Images', 'Project']);
    });

    it('scrolls the entry the keyboard moves to into view, with its group head when it is the first there', async () => {
        const scrolled = vi.spyOn(Element.prototype, 'scrollIntoView');
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await within(list).findByText('Folders · 1');
        scrolled.mockClear();
        await user.keyboard('{ArrowDown}');

        const folder = within(list).getByRole('option', { name: /Campaigns/, selected: true });

        expect(scrolled.mock.contexts).toEqual([within(list).getByText('Folders · 1'), folder]);
        expect(scrolled).toHaveBeenCalledWith({ block: 'nearest' });
        scrolled.mockRestore();
    });

    it('loads the root levels with the bare @, before a group is chosen', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await waitFor(() => expect(fetchMock.mock.calls.filter(([input]) => String(input).startsWith('/rest/folder/'))).toHaveLength(3));
        fetchMock.mockClear();

        await user.click(within(list).getByRole('option', { name: 'Pages' }));

        expect(within(list).getByRole('option', { name: /Home/ })).toBeInTheDocument();
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('searches only the chosen group once text is typed', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await user.keyboard('{ArrowDown}{Tab}Camp');

        expect(await within(list).findByRole('option', { name: /Campaigns/ })).toBeInTheDocument();
        expect(within(list).queryByRole('option', { name: /News/ })).not.toBeInTheDocument();
        expect(within(list).queryByText('Pages · 1')).not.toBeInTheDocument();
        expect(searches(fetchMock)).toEqual(['folder:Camp']);
    });

    it('keeps the last results, narrowed to the typed text, while the search for more text runs', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await within(list).findByRole('option', { name: /Campaign launch/ });

        let release = () => {};

        searchesHeld = new Promise((resolve) => {
            release = resolve;
        });
        await user.keyboard('aigns');
        // Past the delay: the search for "Campaigns" has started and waits.
        await new Promise((resolve) => setTimeout(resolve, 300));

        expect(within(list).getByRole('option', { name: /Campaigns/ })).toBeInTheDocument();
        expect(within(list).queryByRole('option', { name: /Campaign launch/ })).not.toBeInTheDocument();
        expect(screen.getByRole('status', { name: 'Searching' })).toBeInTheDocument();

        release();
    });

    it('does not say that nothing was found before the search has answered', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@zzz');

        expect(screen.getByRole('status', { name: 'Searching' })).toBeInTheDocument();
        expect(screen.queryByText('No context found for “zzz”.')).not.toBeInTheDocument();
        expect(await screen.findByText('No context found for “zzz”.')).toBeInTheDocument();
        expect(screen.queryByRole('status', { name: 'Searching' })).not.toBeInTheDocument();
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

    it('offers the advanced folder search last among folders; it starts with the typed text, and its folder replaces @query', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('Use @folder:Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(await within(list).findByText('Folders · 1')).toBeInTheDocument();

        const advanced = screen.getByRole('option', { name: /Advanced folder search/ });

        expect(list).toHaveAttribute('aria-owns', advanced.id);

        await user.keyboard('{End}');

        expect(field).toHaveAttribute('aria-activedescendant', advanced.id);

        await user.keyboard('{Enter}');

        const dialog = await screen.findByRole('dialog', { name: 'Choose a folder' });
        const search = within(dialog).getByRole('combobox', { name: 'Search folders' });

        expect(screen.queryByRole('listbox', { name: 'Context' })).not.toBeInTheDocument();
        expect(search).toHaveValue('Camp');
        expect(await within(dialog).findByText('1 match')).toBeInTheDocument();
        await waitFor(() => expect(search).toHaveFocus());

        await user.keyboard('{Enter}');

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(parts()).toEqual([
            { type: 'text', text: 'Use ' },
            { type: 'reference', ref: { type: 'folder', id: '42', node_id: 3, label: 'Campaigns' } },
        ]);
    });

    it('goes back to the @-menu, still narrowed to folders, when the folder search is cancelled', async () => {
        const user = userEvent.setup();
        const { field, parts } = renderField();

        await user.click(field);
        await user.keyboard('@');
        await screen.findByRole('listbox', { name: 'Context' });
        await user.keyboard('{ArrowDown}{Enter}Camp');

        await user.click(await screen.findByRole('option', { name: /Advanced folder search/ }));

        const dialog = await screen.findByRole('dialog', { name: 'Choose a folder' });

        const search = within(dialog).getByRole('combobox', { name: 'Search folders' });

        expect(search).toHaveValue('Camp');
        // The dialog moves the focus to its search a frame after it opened.
        await waitFor(() => expect(search).toHaveFocus());

        await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

        const list = await screen.findByRole('listbox', { name: 'Context' });

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Remove filter' }).parentElement).toHaveTextContent('Folders');
        expect(await within(list).findByRole('option', { name: /Campaigns/ })).toBeInTheDocument();
        // The editor focuses its field a frame later.
        await waitFor(() => expect(field).toHaveFocus());
        expect(parts()).toEqual([{ type: 'text', text: '@Camp' }]);
    });

    it('offers the advanced folder search only among folders', async () => {
        const user = userEvent.setup();
        const { field } = renderField();

        await user.click(field);
        await user.keyboard('@Camp');

        const list = await screen.findByRole('listbox', { name: 'Context' });

        await within(list).findByText('Folders · 1');

        expect(screen.queryByRole('option', { name: /Advanced folder search/ })).not.toBeInTheDocument();
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
