import { QueryClient } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { ContextReference } from '@/services/apiService/genaix/types';
import { createWrapper } from '@/test/renderWithProviders';

import { FormReferenceField } from './FormReferenceField';

import '@/i18n';

const NODE = { id: 3, name: 'Corporate Website', folderId: 7 };
const CONTACT = { id: 4410, name: 'Kontaktformular', formType: 'contact' };
const NEWSLETTER = { id: 4412, name: 'Newsletter-Anmeldung', formType: 'crm-lead' };

// The session (with the node it works in, if any) and the CMS: one node with a CMS form and an external one.
function stubSessionAndCms({ context = { node_id: 3 }, formStatus = 200 }: { context?: { node_id?: number }; formStatus?: number } = {}) {
    const fetchMock = vi.fn<typeof fetch>(async (input) => {
        const url = new URL(String(input), 'http://app.test');

        if (url.pathname === '/rest/proxy/genaix/sessions/s-1') {
            return Response.json({ id: 's-1', status: 'active', context });
        }

        if (url.pathname === '/rest/node') {
            return Response.json({ items: [NODE] });
        }

        if (url.pathname === '/rest/form') {
            const query = url.searchParams.get('q');

            return formStatus === 200
                ? Response.json({ items: [CONTACT, NEWSLETTER].filter(({ name }) => query === null || name.startsWith(query)) })
                : new Response(null, { status: formStatus });
        }

        if (url.pathname === '/rest/form/types') {
            return Response.json({ items: [{ type: 'crm-lead', external: true }] });
        }

        return new Response(null, { status: 404 });
    });

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
}

function renderField(value?: ContextReference) {
    const onChange = vi.fn();
    const Wrapper = createWrapper(new QueryClient({ defaultOptions: { queries: { retry: false } } }));

    render(<FormReferenceField sessionId="s-1" value={value} onChange={onChange} label="Form to embed" />, { wrapper: Wrapper });

    return onChange;
}

describe('FormReferenceField', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('offers the forms of the node the session works in, and names a picked one as a form of that node', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const onChange = renderField();

        await user.click(await screen.findByRole('radio', { name: /Kontaktformular/ }));

        expect(onChange).toHaveBeenLastCalledWith({ type: 'form', id: '4410', node_id: 3, label: 'Kontaktformular' });
    });

    it('says which form is external and names it so (contract `ContextReference.external`)', async () => {
        const user = userEvent.setup();

        stubSessionAndCms();
        const onChange = renderField();

        const external = await screen.findByRole('radio', { name: /Newsletter-Anmeldung/ });

        await waitFor(() => expect(screen.getByText('External')).toBeInTheDocument());
        await user.click(external);

        expect(onChange).toHaveBeenLastCalledWith({ type: 'form', id: '4412', node_id: 3, label: 'Newsletter-Anmeldung', external: true });
    });

    it('searches the forms for the text typed', async () => {
        const user = userEvent.setup();
        const fetchMock = stubSessionAndCms();

        renderField();
        await user.type(await screen.findByRole('searchbox', { name: 'Search forms' }), 'News');

        await waitFor(() => expect(screen.queryByRole('radio', { name: /Kontaktformular/ })).not.toBeInTheDocument());
        expect(screen.getByRole('radio', { name: /Newsletter-Anmeldung/ })).toBeInTheDocument();
        expect(fetchMock.mock.calls.map(([input]) => new URL(String(input), 'http://app.test')).some((url) => url.pathname === '/rest/form' && url.searchParams.get('q') === 'News')).toBe(true);
    });

    it('shows the picked form as checked', async () => {
        stubSessionAndCms();
        renderField({ type: 'form', id: '4410', node_id: 3, label: 'Kontaktformular' });

        expect(await screen.findByRole('radio', { name: /Kontaktformular/ })).toBeChecked();
    });

    it('says so when the session works in no node, and asks the CMS for no forms', async () => {
        const fetchMock = stubSessionAndCms({ context: {} });

        renderField();

        expect(await screen.findByText('This session works in no CMS node, so there are no forms to choose from.')).toBeInTheDocument();
        expect(fetchMock.mock.calls.some(([input]) => String(input).startsWith('/rest/form'))).toBe(false);
    });

    it('says so when the forms cannot be loaded', async () => {
        stubSessionAndCms({ formStatus: 500 });
        renderField();

        expect(await screen.findByText('The forms could not be loaded.')).toBeInTheDocument();
    });
});
