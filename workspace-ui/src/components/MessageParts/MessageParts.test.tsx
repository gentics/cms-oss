import { render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { UiProvider } from '@/components/ui/provider';
import type { MessagePart, UnknownPart } from '@/services/apiService/genaix/types';

import { PartRenderer } from './MessageParts';

import '@/i18n';

function renderPart(part: MessagePart | UnknownPart) {
    return render(<PartRenderer part={part} sessionId="s-1" />, { wrapper: UiProvider });
}

describe('PartRenderer', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('renders a text part', () => {
        renderPart({ type: 'text', format: 'plain', text: 'Ich habe die Aktualisierung gelesen.' });

        expect(screen.getByText('Ich habe die Aktualisierung gelesen.')).toBeInTheDocument();
    });

    it('renders a status note with its kind', () => {
        renderPart({ type: 'status_note', kind: 'warning', text: 'Saved a snapshot for comparison.' });

        expect(screen.getByText('Saved a snapshot for comparison.')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'Warning' })).toBeInTheDocument();
    });

    it('renders a tree view as a card with its nodes', () => {
        renderPart({ type: 'tree_view', label: 'Candidate folders', nodes: [{ id: '3', label: 'Acme GmbH', type: 'node', children: [{ id: '42', label: 'Richtlinien', type: 'folder', children: [] }] }] });

        const card = screen.getByRole('region', { name: 'Candidate folders' });

        expect(within(card).getByRole('checkbox', { name: 'Acme GmbH' })).toBeInTheDocument();
        expect(within(card).getByRole('checkbox', { name: 'Richtlinien' })).toBeInTheDocument();
    });

    it('renders a selectable list with its items', () => {
        renderPart({ type: 'selectable_list', label: 'Which template?', multi: false, items: [{ id: 'tpl-17', label: 'Kampagnen-Landingpage', description: 'Hero, text columns, form embed.' }] });

        expect(screen.getByRole('radio', { name: /Kampagnen-Landingpage/ })).toBeInTheDocument();
        expect(screen.getByText('Hero, text columns, form embed.')).toBeInTheDocument();
    });

    it('renders a properties list as label and value, a reference as its token, and null as a dash', () => {
        renderPart({
            type: 'properties_list',
            label: 'Derived settings',
            items: [
                { key: 'folder', label: 'Folder', value: 'Richtlinien', ref: { type: 'folder', id: '42', node_id: 3 } },
                { key: 'language', label: 'Language', value: 'de' },
                { key: 'publish_at', label: 'Publish at', value: null },
            ],
        });

        const card = screen.getByRole('region', { name: 'Derived settings' });
        const terms = within(card).getAllByRole('term').map((term) => term.textContent);
        const values = within(card).getAllByRole('definition').map((value) => value.textContent);

        expect(terms).toEqual(['Folder', 'Language', 'Publish at']);
        expect(values).toEqual(['Richtlinien', 'de', '–']);
    });

    it('renders an image grid with every image', () => {
        renderPart({ type: 'image_grid', label: 'Hero images', items: [{ id: 'img-1', url: '/img/1.jpg', alt: 'Campaign visual' }, { id: 'img-2', url: '/img/2.jpg' }] });

        expect(screen.getByRole('img', { name: 'Campaign visual' })).toHaveAttribute('src', '/img/1.jpg');
        expect(screen.getByRole('img', { name: 'Image 2' })).toHaveAttribute('src', '/img/2.jpg');
    });

    it('renders a table with its columns and rows', () => {
        renderPart({ type: 'table', label: 'Pages', columns: [{ key: 'page', label: 'Page' }], rows: [{ page: 'Garantiebedingungen' }] });

        expect(screen.getByRole('columnheader', { name: 'Page' })).toBeInTheDocument();
        expect(screen.getByRole('cell', { name: 'Garantiebedingungen' })).toBeInTheDocument();
    });

    it('renders a page structure with the status of every block, not by colour alone', () => {
        renderPart({
            type: 'page_structure',
            label: 'Seitenstruktur',
            page_ref: { type: 'page', id: 9142, name: 'Neue Nutzungsbedingungen ab Q3' },
            blocks: [
                { id: 'content_1', construct_keyword: 'hero_teaser', label: 'Hero', status: 'created', tag_keyword: 'content_1' },
                { id: 'content_3', construct_keyword: 'quote_block', label: 'Widerrufsbelehrung', status: 'planned', verbatim: true },
            ],
        });

        const blocks = screen.getAllByRole('listitem');

        expect(blocks[0]).toHaveTextContent('Hero');
        expect(blocks[0]).toHaveTextContent('Created');
        expect(blocks[1]).toHaveTextContent('Planned');
        expect(within(blocks[1]!).getByRole('img', { name: 'Word for word' })).toBeInTheDocument();
        expect(screen.getByText('Neue Nutzungsbedingungen ab Q3')).toBeInTheDocument();
    });

    it('renders a construct draft with its parts, template and validation', () => {
        renderPart({
            type: 'construct_draft',
            keyword: 'terms_change_table',
            name: { de: 'Aenderungstabelle', en: 'Terms change table' },
            parts: [{ keyword: 'heading', name: { en: 'Heading' }, type_id: 1, editable: true, mandatory: true }],
            template: '<table>{{cms.tag.parts.heading}}</table>',
            validation: { ok: true, issues: [{ severity: 'warning', message: 'Set a default row count.', line: 3 }] },
        });

        expect(screen.getByRole('region', { name: 'Terms change table' })).toBeInTheDocument();
        expect(screen.getByText('Heading')).toBeInTheDocument();
        expect(screen.getByText('<table>{{cms.tag.parts.heading}}</table>')).toBeInTheDocument();
        expect(screen.getByText('The template is valid')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'Warning' })).toBeInTheDocument();
        expect(screen.getByText('Set a default row count.')).toBeInTheDocument();
    });

    it('renders an API call log', () => {
        renderPart({ type: 'api_call_log', label: 'CMS calls', calls: [{ method: 'POST', path: '/rest/construct?nodeId=3', summary: 'Create construct', status: 200 }] });

        expect(screen.getByRole('cell', { name: 'POST /rest/construct?nodeId=3' })).toBeInTheDocument();
        expect(screen.getByRole('cell', { name: '200' })).toBeInTheDocument();
        expect(screen.getByRole('cell', { name: 'Create construct' })).toBeInTheDocument();
    });

    it('renders a citation with its marker and the highlighted passage as text, not as HTML', () => {
        const { container } = renderPart({
            type: 'citation',
            refs: [{ marker: '[1]', ref: { type: 'page', id: '8871', label: 'Garantiebedingungen' }, snippet: 'ab einem Warenwert von <em>35 Euro</em> <img src=x onerror=alert(1)>kostenfrei' }],
        });

        expect(screen.getByText('[1]')).toBeInTheDocument();
        expect(screen.getByText('Garantiebedingungen')).toBeInTheDocument();
        expect(container.querySelector('mark')).toHaveTextContent('35 Euro');
        expect(container.querySelector('img')).toBeNull();
    });

    it('renders a file reference as a download link into the session', () => {
        renderPart({ type: 'file_ref', file_id: 'f-1', name: 'page-draft-9142.json' });

        expect(screen.getByRole('link', { name: /page-draft-9142\.json/ })).toHaveAttribute('href', '/genaix/api/v1/sessions/s-1/files/f-1/content');
    });

    describe('a part type outside the registry (contract `UnknownPart`)', () => {
        it('shows its text', () => {
            renderPart({ type: 'chart_v2', text: 'Three pages changed this week.', series: [1, 2, 3] });

            expect(screen.getByText('Three pages changed this week.')).toBeInTheDocument();
        });

        it('shows nothing without text, and no JSON', () => {
            const { container } = renderPart({ type: 'chart_v2', series: [1, 2, 3] });

            expect(container).toBeEmptyDOMElement();
        });
    });

    it('falls back to the UnknownPart rule when a known part has a shape it cannot render', () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        // `rows` holds no objects: the table cannot read its cells.
        const { container } = renderPart({ type: 'table', columns: [{ key: 'a', label: 'A' }], rows: [null] } as unknown as MessagePart);

        expect(container).toBeEmptyDOMElement();
    });
});
