import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useRecentFoldersStore } from '@/store/useRecentFoldersStore';
import { createWrapper } from '@/test/renderWithProviders';

import { FolderSearchDialog } from './FolderSearchDialog';

const WEBSITE = { id: 3, name: 'Website', folderId: 7, type: 'node' };
const PORTAL = { id: 5, name: 'Partner portal', folderId: 9, type: 'channel', inheritedFromName: 'Website' };

function folder(id: number, path: string, hasSubfolders = false, inherited = false) {
    return { id, name: path.split('/').filter(Boolean).at(-1)!, path, hasSubfolders, inherited };
}

const PRODUCTS = folder(10, '/Website/Products/', true);
const NEWS = folder(11, '/Website/News/', true);
const CLOUD = folder(20, '/Website/Products/Cloud/');
const CLOUD_PLATFORM = folder(21, '/Website/Products/Cloud/Cloud platform/');
// 61 folders in News, one more than a page.
const NEWS_ITEMS = Array.from({ length: 61 }, (_, index) => folder(100 + index, `/Website/News/Item ${String(index + 1).padStart(2, '0')}/`));

// The subfolders of each folder, and what a search below the node's root folder finds.
const LEVELS: Record<string, unknown[]> = {
    7: [NEWS, PRODUCTS],
    10: [CLOUD],
    11: NEWS_ITEMS,
    9: [folder(30, '/Partner portal/Local news/'), folder(10, '/Partner portal/Products/', true, true)],
};

// Answers the CMS like `getFolders`: a page of a level, or a search, with how many there are.
function stubBackend() {
    vi.stubGlobal('fetch', vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');
        const [, id] = /^\/rest\/folder\/getFolders\/(\d+)$/.exec(url.pathname) ?? [];

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [WEBSITE, PORTAL] });
        }

        if (id) {
            const search = url.searchParams.get('search');
            // The CMS sorts by name, so "Cloud platform" comes before "Cloud".
            const all = search === null ? LEVELS[id] ?? [] : [CLOUD_PLATFORM, CLOUD].filter((found) => found.name.toLowerCase().includes(search.toLowerCase()));
            const skip = Number(url.searchParams.get('skipCount') ?? 0);
            const max = Number(url.searchParams.get('maxItems'));

            return Response.json({ folders: all.slice(skip, skip + max), numItems: all.length, hasMoreItems: all.length > skip + max });
        }

        return new Response(null, { status: 404 });
    }));
}

function renderDialog(initialQuery?: string) {
    const onPick = vi.fn();
    const onCancel = vi.fn();
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<FolderSearchDialog initialQuery={initialQuery} onPick={onPick} onCancel={onCancel} />, { wrapper: Wrapper });

    return { onPick, onCancel, search: screen.getByRole('combobox', { name: 'Search folders' }) };
}

describe('FolderSearchDialog', () => {
    beforeEach(() => {
        stubBackend();
        useRecentFoldersStore.setState({ recent: [] });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('lists the root level of the first node, opens a folder with its chevron and goes back along the path', async () => {
        const user = userEvent.setup();

        renderDialog();

        expect(screen.getByRole('dialog', { name: 'Choose a folder' })).toBeInTheDocument();

        const list = screen.getByRole('listbox', { name: 'Folders' });

        expect(await within(list).findByText('All folders · 2')).toBeInTheDocument();
        expect(within(list).getAllByRole('option').map((option) => option.textContent)).toEqual(['News', 'Products']);

        await user.click(screen.getByRole('button', { name: 'Open Products' }));

        expect(await within(list).findByText('In Products · 1')).toBeInTheDocument();
        expect(within(list).getByRole('option', { name: 'Cloud' })).toBeInTheDocument();

        const path = screen.getByRole('navigation', { name: 'Folder path' });

        expect(within(path).getByText('Products')).toHaveAttribute('aria-current', 'location');
        // The folder opened is picked, so that it can be taken as well.
        expect(screen.getByRole('button', { name: 'Use folder' })).toBeEnabled();
        expect(screen.getByText('Website ›', { exact: false })).toHaveTextContent('Website › Products');

        await user.click(within(path).getByRole('button', { name: 'Website' }));

        expect(await within(list).findByText('All folders · 2')).toBeInTheDocument();
    });

    it('searches all folders for the text it starts with, picks the best one and takes it with Enter', async () => {
        const user = userEvent.setup();
        const { onPick, search } = renderDialog('cloud');

        expect(search).toHaveValue('cloud');

        const list = screen.getByRole('listbox', { name: 'Folders' });

        expect(await within(list).findByText('2 matches')).toBeInTheDocument();

        // "Cloud" is the text itself, so it comes before "Cloud platform", which the CMS sorted first.
        const options = within(list).getAllByRole('option');

        expect(options.map((option) => option.textContent)).toEqual(['CloudProducts', 'Cloud platformProducts › Cloud']);
        expect(options[0]).toHaveAttribute('aria-selected', 'true');
        expect(search).toHaveAttribute('aria-activedescendant', options[0]!.id);

        await user.keyboard('{Enter}');

        expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 3 }), CLOUD);
        expect(useRecentFoldersStore.getState().recent).toEqual([{ nodeId: 3, folder: CLOUD }]);
    });

    it('takes nothing with Enter while the search for new text has not answered', async () => {
        const user = userEvent.setup();
        const { onPick, search } = renderDialog('cloud');

        await screen.findByText('2 matches');
        await user.type(search, ' platform');
        await user.keyboard('{Enter}');

        expect(onPick).not.toHaveBeenCalled();

        // Once it has answered, its best folder is picked.
        await screen.findByText('1 match');
        await user.keyboard('{Enter}');

        expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 3 }), CLOUD_PLATFORM);
    });

    it('shows the folders chosen last first', async () => {
        useRecentFoldersStore.setState({ recent: [{ nodeId: 3, folder: CLOUD }, { nodeId: 5, folder: NEWS }] });

        renderDialog();

        const recent = await screen.findByRole('group', { name: 'Recent' });

        expect(within(recent).getAllByRole('option').map((option) => option.textContent)).toEqual(['CloudProducts']);
    });

    it('picks with the arrows, opens with the right arrow and goes up with the left one', async () => {
        const user = userEvent.setup();

        renderDialog();

        const list = screen.getByRole('listbox', { name: 'Folders' });

        await within(list).findByText('All folders · 2');
        await user.keyboard('{ArrowDown}{ArrowDown}');

        expect(within(list).getByRole('option', { name: 'Products' })).toHaveAttribute('aria-selected', 'true');

        await user.keyboard('{ArrowRight}');

        expect(await within(list).findByText('In Products · 1')).toBeInTheDocument();

        await user.keyboard('{ArrowLeft}');

        expect(await within(list).findByText('All folders · 2')).toBeInTheDocument();
        expect(within(list).getByRole('option', { name: 'Products' })).toHaveAttribute('aria-selected', 'true');
    });

    it('opens a folder with a double click and takes it only with the button', async () => {
        const user = userEvent.setup();
        const { onPick } = renderDialog();

        const list = screen.getByRole('listbox', { name: 'Folders' });

        await user.dblClick(await within(list).findByRole('option', { name: 'Products' }));

        expect(await within(list).findByText('In Products · 1')).toBeInTheDocument();
        expect(onPick).not.toHaveBeenCalled();

        // A folder without subfolders stays where it is and is only picked.
        const cloud = within(list).getByRole('option', { name: 'Cloud' });

        await user.dblClick(cloud);

        expect(cloud).toHaveAttribute('aria-selected', 'true');
        expect(within(list).getByText('In Products · 1')).toBeInTheDocument();
        expect(onPick).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', { name: 'Use folder' }));

        expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 3 }), CLOUD);
    });

    it('takes a folder picked with the mouse with Enter, also after going along the path', async () => {
        const user = userEvent.setup();
        const { onPick, search } = renderDialog();

        const list = screen.getByRole('listbox', { name: 'Folders' });

        await user.click(await within(list).findByRole('option', { name: 'Products' }));

        expect(search).toHaveFocus();

        await user.click(screen.getByRole('button', { name: 'Open Products' }));
        await user.click(within(screen.getByRole('navigation', { name: 'Folder path' })).getByRole('button', { name: 'Website' }));
        await within(list).findByText('All folders · 2');

        expect(search).toHaveFocus();

        await user.click(within(list).getByRole('option', { name: 'News' }));
        await user.keyboard('{Enter}');

        expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 3 }), NEWS);
    });

    it('shows a long level a page at a time', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.click(await screen.findByRole('button', { name: 'Open News' }));

        const list = screen.getByRole('listbox', { name: 'Folders' });

        expect(await within(list).findByText('In News · 61')).toBeInTheDocument();
        expect(within(list).getAllByRole('option')).toHaveLength(60);

        await user.click(screen.getByRole('button', { name: 'Show 1 more of 1' }));

        await waitFor(() => expect(within(list).getAllByRole('option')).toHaveLength(61));
        expect(screen.queryByRole('button', { name: /more of/ })).not.toBeInTheDocument();
    });

    it('switches to a channel and marks the folders local to it', async () => {
        const user = userEvent.setup();

        renderDialog();

        const node = screen.getByRole('combobox', { name: 'Node' });

        await waitFor(() => expect(node).toHaveTextContent('Website'));
        await user.click(node);
        await user.click(await screen.findByRole('option', { name: 'Partner portal (channel of Website)' }));

        const list = screen.getByRole('listbox', { name: 'Folders' });
        const local = await within(list).findByRole('option', { name: /Local news/ });

        expect(within(local).getByText('Local')).toBeInTheDocument();
        expect(within(within(list).getByRole('option', { name: /Products/ })).queryByText('Local')).not.toBeInTheDocument();
    });

    it('cancels with the button and with Esc, and takes nothing without a folder', async () => {
        const user = userEvent.setup();
        const { onPick, onCancel } = renderDialog();

        await screen.findByText('All folders · 2');

        expect(screen.getByRole('button', { name: 'Use folder' })).toBeDisabled();
        expect(screen.getByText('Pick a folder')).toBeInTheDocument();

        await user.keyboard('{Enter}');
        await user.click(screen.getByRole('button', { name: 'Cancel' }));
        await user.keyboard('{Escape}');

        expect(onPick).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(2);
    });

    it('says when no folder matches', async () => {
        const user = userEvent.setup();
        const { search } = renderDialog();

        await user.type(search, 'zzz');

        expect(await screen.findByText('No folder matches “zzz” in Website.')).toBeInTheDocument();
    });
});
