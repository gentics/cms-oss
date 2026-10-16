import { describe, expect, it } from 'vitest';

import i18n from '@/i18n';
import type { PlanItem } from '@/services/apiService/genaix/types';

import { glanceText, isWorthAGlance, planGlance, showsProgress } from './planGlance';

function item(id: string, overrides: Partial<PlanItem> & Pick<PlanItem, 'kind'>): PlanItem {
    return { id, label: id, workflow: 'content_create', status: 'pending', ...overrides };
}

const folder = (id: string) => ({ type: 'folder' as const, label: `Folder ${id}`, path: '/Acme/' });
const page = (id: string) => ({ type: 'page' as const, label: `Page ${id}` });

// The draft's plan: three folders, four pages with tag fills, two pages edited, two translations, a report.
const items: PlanItem[] = [
    item('f1', { kind: 'create', creates: folder('1'), status: 'done' }),
    item('f2', { kind: 'create', creates: folder('2'), status: 'done' }),
    item('f3', { kind: 'create', creates: folder('3') }),
    item('p1', { kind: 'create', creates: page('1'), status: 'done' }),
    item('p1-hero', { kind: 'construct', parent: 'p1', status: 'done' }),
    item('p2', { kind: 'create', creates: page('2') }),
    item('p3', { kind: 'create', creates: page('3') }),
    item('p4', { kind: 'create', creates: page('4') }),
    item('e1', { kind: 'update', target: { type: 'page', id: '8644' } }),
    item('e2', { kind: 'update', target: { type: 'page', id: '8871' } }),
    item('t1', { kind: 'translate', creates: { type: 'page', label: 'Shipping', language: 'en' } }),
    item('t2', { kind: 'translate', creates: { type: 'page', label: 'Warranty', language: 'en' } }),
    item('r1', { kind: 'report' }),
    item('o1', { kind: 'other' }),
];

describe('planGlance', () => {
    it('counts the top-level items by kind and object type, in plan order', () => {
        expect(planGlance(items)).toEqual([
            { kind: 'create', type: 'folder', total: 3, done: 2, isNew: true },
            { kind: 'create', type: 'page', total: 4, done: 1, isNew: true },
            { kind: 'update', type: 'page', total: 2, done: 0, isNew: false },
            { kind: 'translate', type: 'page', total: 2, done: 0, isNew: true },
        ]);
    });

    it('leaves out sub-steps, removed items, reports and other items', () => {
        expect(planGlance([
            item('p1', { kind: 'create', creates: page('1') }),
            item('p2', { kind: 'construct', parent: 'p1' }),
            item('p3', { kind: 'update', target: { type: 'page', id: '1' }, status: 'removed' }),
            item('p4', { kind: 'report' }),
            item('p5', { kind: 'other' }),
        ])).toEqual([{ kind: 'create', type: 'page', total: 1, done: 0, isNew: true }]);
    });

    it('takes the object type from `creates` before `target`', () => {
        // A create's target is the parent folder; what it brings into being is a page.
        expect(planGlance([item('p1', { kind: 'create', creates: page('1'), target: { type: 'folder', id: '42' } })])[0]?.type).toBe('page');
    });

    it('counts an item naming no object under no type', () => {
        expect(planGlance([item('p1', { kind: 'link' })])).toEqual([{ kind: 'link', total: 1, done: 0, isNew: false }]);
    });

    it('counts a construct item as new only when every one says what it creates', () => {
        expect(planGlance([item('c1', { kind: 'construct', creates: { type: 'construct', label: 'Event teaser' } })])[0]?.isNew).toBe(true);
        expect(planGlance([item('c1', { kind: 'construct', target: { type: 'construct', id: '7' } })])[0]?.isNew).toBe(false);
    });

    it('is empty for a plan without counted items', () => {
        expect(planGlance([])).toEqual([]);
        expect(planGlance([item('r1', { kind: 'report' })])).toEqual([]);
    });
});

describe('showsProgress', () => {
    it('shows the progress once the plan is accepted, not while it is drafted or proposed', () => {
        expect(['accepted', 'running', 'done', 'released', 'published', 'review_requested'].every((status) => showsProgress(status as never))).toBe(true);
        expect(['drafting', 'proposed', 'discarded', 'superseded'].some((status) => showsProgress(status as never))).toBe(false);
    });
});

describe('glanceText', () => {
    const t = i18n.getFixedT('en');

    it('reads new objects as "+n", others as their count', () => {
        const [folders, pages, edits, translations] = planGlance(items);

        expect(glanceText(t, folders!, false)).toEqual({ value: '+3', label: 'folders' });
        expect(glanceText(t, pages!, false)).toEqual({ value: '+4', label: 'new pages' });
        expect(glanceText(t, edits!, false)).toEqual({ value: '2', label: 'pages edited' });
        expect(glanceText(t, translations!, false)).toEqual({ value: '+2', label: 'translations' });
    });

    it('reads the progress as "done/total"', () => {
        expect(glanceText(t, planGlance(items)[0]!, true)).toEqual({ value: '2/3', label: 'folders' });
    });

    it('falls back to the kind for an object type without its own wording', () => {
        expect(glanceText(t, { kind: 'update', type: 'template', total: 1, done: 0, isNew: false }, false).label).toBe('object edited');
        expect(glanceText(t, { kind: 'link', total: 2, done: 0, isNew: false }, false).label).toBe('links');
    });
});

describe('isWorthAGlance', () => {
    it('is worth it from two counted items on, not for a plan of one item', () => {
        expect(isWorthAGlance(planGlance(items))).toBe(true);
        expect(isWorthAGlance(planGlance([item('e1', { kind: 'update', target: { type: 'page', id: '41' } })]))).toBe(false);
        expect(isWorthAGlance([])).toBe(false);
    });
});
