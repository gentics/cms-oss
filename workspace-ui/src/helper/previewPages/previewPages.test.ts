import { describe, expect, it } from 'vitest';

import type { PlanItem, Preview } from '@/services/apiService/genaix/types';

import { previewPages } from './previewPages';

function preview(id: number, name: string): Preview {
    return { cms_ref: { type: 'page', id, node_id: 3, name }, preview_url: `https://cms.example.com/alohapage?realid=${id}&nodeid=3&mode=view`, mode: 'view' };
}

function item(id: string, overrides: Partial<PlanItem> & Pick<PlanItem, 'kind'>): PlanItem {
    return { id, label: id, workflow: 'content_edit', status: 'pending', ...overrides };
}

const page = (id: string) => ({ type: 'page' as const, id, node_id: 3 });

describe('previewPages', () => {
    it('groups the pages by the kind of the item that created or changed them, in the order they come', () => {
        const shipping = preview(8644, 'Versand und Lieferung');
        const fair = preview(9312, 'Messe Frankfurt');
        const warranty = preview(8871, 'Garantiebedingungen');
        const english = preview(9401, 'Shipping and delivery');
        const items = [
            item('p1', { kind: 'create', creates: { type: 'page', label: 'Messe Frankfurt' }, target: { type: 'folder', id: '57' }, created: page('9312'), status: 'done' }),
            item('p2', { kind: 'construct', parent: 'p1', target: page('9312') }),
            item('p3', { kind: 'update', target: page('8644') }),
            item('p4', { kind: 'update', target: page('8871') }),
            item('p5', { kind: 'translate', creates: { type: 'page', label: 'Shipping and delivery', language: 'en', of: page('8644') }, target: page('8644'), created: page('9401') }),
        ];

        expect(previewPages([shipping, fair, warranty, english], items)).toEqual([
            { kind: 'update', previews: [shipping, warranty] },
            { kind: 'create', previews: [fair] },
            { kind: 'translate', previews: [english] },
        ]);
    });

    it('puts a page no item names, or one of a report, under other', () => {
        const named = preview(1, 'Report page');
        const unknown = preview(2, 'Somewhere');

        expect(previewPages([named, unknown], [item('p1', { kind: 'report', target: page('1') })])).toEqual([
            { kind: 'other', previews: [named, unknown] },
        ]);
    });

    it('does not take a create item\'s target for the page it creates', () => {
        // A translation acts on the original page (`target`) but creates the variant: the original is edited elsewhere.
        const original = preview(8644, 'Versand und Lieferung');

        expect(previewPages([original], [
            item('p1', { kind: 'translate', creates: { type: 'page', label: 'Shipping' }, target: page('8644') }),
            item('p2', { kind: 'update', target: page('8644') }),
        ])).toEqual([{ kind: 'update', previews: [original] }]);
    });

    it('leaves out removed items and sub-steps', () => {
        const shipping = preview(8644, 'Versand und Lieferung');

        expect(previewPages([shipping], [
            item('p1', { kind: 'update', target: page('8644'), status: 'removed' }),
            item('p2', { kind: 'link', target: page('8644'), parent: 'p9' }),
        ])).toEqual([{ kind: 'other', previews: [shipping] }]);
    });
});
