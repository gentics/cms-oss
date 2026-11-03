import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { createWrapper } from '@/test/renderWithProviders';

import { PageSettings } from './PageSettings';

// 2026-09-21 and 2024-03-12, as the CMS sends dates: unix seconds.
const PAGE = {
    id: 9142,
    name: 'Products – Overview',
    fileName: 'products.html',
    description: '',
    niceUrl: '/products',
    priority: 1,
    templateId: 17,
    folderId: 42,
    language: 'en',
    cdate: Date.UTC(2024, 2, 12, 12) / 1000,
    edate: Date.UTC(2026, 8, 21, 12) / 1000,
};
const TEMPLATES = [{ id: 17, name: 'Product page' }, { id: 21, name: 'Overview page' }];

// Answers the CMS requests of the sheet: the page, or `page` in its place, and the folder's templates.
function stubCms(page: Response = Response.json({ page: PAGE })) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/page/load/9142') {
            return page;
        }

        if (url.pathname === '/rest/folder/getTemplates/42') {
            return Response.json({ templates: TEMPLATES });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderSheet({ canSend = true, onApply = vi.fn(() => Promise.resolve()), onClose = vi.fn() } = {}) {
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<PageSettings pageId={9142} nodeId={3} canSend={canSend} onClose={onClose} onApply={onApply} />, { wrapper: Wrapper });

    return { onApply, onClose };
}

describe('PageSettings', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('shows the page\'s settings as the CMS has them, language and dates read only', async () => {
        const fetchMock = stubCms();

        renderSheet();

        expect(await screen.findByRole('textbox', { name: 'Name' })).toHaveValue('Products – Overview');
        expect(screen.getByRole('textbox', { name: 'Filename' })).toHaveValue('products.html');
        expect(screen.getByRole('textbox', { name: 'Description' })).toHaveValue('');
        expect(screen.getByRole('textbox', { name: 'Nice URL' })).toHaveValue('/products');
        expect(screen.getByRole('spinbutton', { name: 'Priority' })).toHaveValue(1);
        await waitFor(() => expect(screen.getByRole('combobox', { name: 'Template' })).toHaveTextContent('Product page'));
        expect(screen.getByText('Language').nextElementSibling).toHaveTextContent('en');
        expect(screen.getByText('Created').nextElementSibling).toHaveTextContent(new Date(PAGE.cdate * 1000).toLocaleDateString('en'));
        expect(screen.getByText('Last edited').nextElementSibling).toHaveTextContent(new Date(PAGE.edate * 1000).toLocaleDateString('en'));
        expect(screen.queryByRole('textbox', { name: 'Language' })).not.toBeInTheDocument();
        expect(fetchMock).toHaveBeenCalledWith('/rest/page/load/9142?nodeId=3', expect.anything());
    });

    it('applies only when something changed, and hands on the changed settings only, the template as a reference', async () => {
        const user = userEvent.setup();

        stubCms();
        const { onApply } = renderSheet();
        const name = await screen.findByRole('textbox', { name: 'Name' });
        const apply = screen.getByRole('button', { name: 'Apply' });

        expect(apply).toBeDisabled();

        await user.clear(name);
        await user.type(name, 'Products');
        await user.clear(screen.getByRole('textbox', { name: 'Nice URL' }));

        const template = screen.getByRole('combobox', { name: 'Template' });

        await waitFor(() => expect(template).toHaveTextContent('Product page'));
        await user.click(template);
        await user.click(await screen.findByRole('option', { name: 'Overview page' }));
        await user.click(apply);

        expect(onApply).toHaveBeenCalledWith([
            { label: 'Name', value: 'Products' },
            { label: 'Nice URL', value: '' },
            { label: 'Template', value: { type: 'template', id: '21', node_id: 3, label: 'Overview page' } },
        ]);
    });

    it('does not apply while a turn cannot be sent', async () => {
        const user = userEvent.setup();

        stubCms();
        renderSheet({ canSend: false });
        await user.type(await screen.findByRole('textbox', { name: 'Filename' }), 'x');

        expect(screen.getByRole('button', { name: 'Apply' })).toBeDisabled();
    });

    it('closes with Escape and with its close button', async () => {
        const user = userEvent.setup();

        stubCms();
        const { onClose } = renderSheet();

        await user.click(await screen.findByRole('textbox', { name: 'Name' }));
        await user.keyboard('{Escape}');
        expect(onClose).toHaveBeenCalledTimes(1);

        await user.click(screen.getByRole('button', { name: 'Close' }));
        expect(onClose).toHaveBeenCalledTimes(2);
    });

    it('says when the page could not be loaded', async () => {
        stubCms(new Response(null, { status: 500 }));
        renderSheet();

        expect(await screen.findByText('The page settings could not be loaded.')).toHaveAttribute('role', 'status');
        expect(screen.queryByRole('button', { name: 'Apply' })).not.toBeInTheDocument();
    });
});
