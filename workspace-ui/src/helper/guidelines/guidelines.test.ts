import { describe, expect, it } from 'vitest';

import type { Plan, PlanItem } from '@/services/apiService/genaix/types';

import { type Guideline, guidelineFromFile, guidelineName, guidelinePath, guidelineSlug, parseGuidelineSlug, planGuideline, sortGuidelines, unknownGuidelines } from './guidelines';

describe('guidelineFromFile', () => {
    it('reads a default guideline: the slug ends in .default, the priority is left out', () => {
        expect(guidelineFromFile('corporate', '10_tone-of-voice.default.md')).toEqual({
            slug: 'corporate/tone-of-voice.default',
            package: 'corporate',
            name: 'tone-of-voice',
            prio: 10,
            isDefault: true,
            fileName: '10_tone-of-voice.default.md',
        });
    });

    it('reads an available guideline: the slug is <package>/<name>', () => {
        expect(guidelineFromFile('corporate', '20_seo-basics.md')).toEqual({
            slug: 'corporate/seo-basics',
            package: 'corporate',
            name: 'seo-basics',
            prio: 20,
            isDefault: false,
            fileName: '20_seo-basics.md',
        });
    });

    it('ends the priority at the first underscore and keeps the others in the name', () => {
        expect(guidelineFromFile('p', '5_press_release.md')).toMatchObject({ prio: 5, name: 'press_release', slug: 'p/press_release' });
    });

    it('reads a guideline without a priority, which has the same slug as with one', () => {
        expect(guidelineFromFile('corporate', 'tone-of-voice.default.md')).toEqual({
            slug: 'corporate/tone-of-voice.default',
            package: 'corporate',
            name: 'tone-of-voice',
            isDefault: true,
            fileName: 'tone-of-voice.default.md',
        });
        expect(guidelineFromFile('corporate', 'seo-basics.md')).toMatchObject({ slug: 'corporate/seo-basics', isDefault: false });
    });

    it('takes what does not start with digits and _ as the name', () => {
        expect(guidelineFromFile('p', 'a_tone-of-voice.md')).toMatchObject({ name: 'a_tone-of-voice', slug: 'p/a_tone-of-voice' });
        expect(guidelineFromFile('p', 'a_tone-of-voice.md')).not.toHaveProperty('prio');
    });

    it.each([
        ['no name', '10_.md'],
        ['no name before .default', '10_.default.md'],
        ['another extension', '10_tone-of-voice.txt'],
    ])('ignores a file with %s', (_, fileName) => {
        expect(guidelineFromFile('corporate', fileName)).toBeUndefined();
    });
});

describe('guidelinePath', () => {
    it('is the file in the guidelines folder of files-internal', () => {
        expect(guidelinePath({ fileName: '10_tone-of-voice.default.md' })).toBe('guidelines/10_tone-of-voice.default.md');
    });
});

describe('guidelineSlug', () => {
    it('adds .default only for a default guideline', () => {
        expect(guidelineSlug('corporate', 'tone-of-voice', true)).toBe('corporate/tone-of-voice.default');
        expect(guidelineSlug('corporate', 'tone-of-voice', false)).toBe('corporate/tone-of-voice');
    });
});

describe('parseGuidelineSlug', () => {
    it('reads package, name and default from a slug', () => {
        expect(parseGuidelineSlug('corporate/tone-of-voice.default')).toEqual({ package: 'corporate', name: 'tone-of-voice', isDefault: true });
        expect(parseGuidelineSlug('corporate/seo-basics')).toEqual({ package: 'corporate', name: 'seo-basics', isDefault: false });
    });

    it('is the inverse of guidelineSlug', () => {
        expect(parseGuidelineSlug(guidelineSlug('corporate', 'faq.v2', true))).toEqual({ package: 'corporate', name: 'faq.v2', isDefault: true });
    });

    it.each(['tone-of-voice-de', '/tone', 'corporate/'])('is undefined for %s, which is no <package>/<name> slug', (slug) => {
        expect(parseGuidelineSlug(slug)).toBeUndefined();
    });
});

describe('guidelineName', () => {
    it('is the name of the guideline a slug names, default or not', () => {
        expect(guidelineName('corporate/tone-of-voice.default')).toBe('tone-of-voice');
        expect(guidelineName('corporate/seo-basics')).toBe('seo-basics');
    });

    it('is the slug itself when it is no <package>/<name> slug', () => {
        expect(guidelineName('tone-of-voice-de')).toBe('tone-of-voice-de');
    });
});

const guideline = (slug: string, prio?: number): Guideline => ({
    slug,
    package: 'p',
    name: slug,
    ...(prio !== undefined && { prio }),
    isDefault: false,
    fileName: prio === undefined ? `${slug}.md` : `${prio}_${slug}.md`,
});

describe('sortGuidelines', () => {
    it('sorts by priority, lowest first, then by slug, without changing the input', () => {
        const input = [guideline('p/c', 20), guideline('p/b', 10), guideline('p/a', 20)];

        expect(sortGuidelines(input).map(({ slug }) => slug)).toEqual(['p/b', 'p/a', 'p/c']);
        expect(input.map(({ slug }) => slug)).toEqual(['p/c', 'p/b', 'p/a']);
    });

    it('puts the guidelines without a priority after the others, by slug', () => {
        const input = [guideline('p/z'), guideline('p/c', 20), guideline('p/a')];

        expect(sortGuidelines(input).map(({ slug }) => slug)).toEqual(['p/c', 'p/a', 'p/z']);
    });

    it('keeps the first of guidelines with the same slug', () => {
        const input = [guideline('p/seo'), guideline('p/seo', 10)];

        expect(sortGuidelines(input).map(({ fileName }) => fileName)).toEqual(['10_p/seo.md']);
    });
});

describe('unknownGuidelines', () => {
    it('returns the ids that name no existing guideline, in their order', () => {
        const known = [guideline('corporate/tone-of-voice.default', 10), guideline('corporate/seo-basics', 20)];

        // `tone-of-voice` was made a default guideline, so its old slug is gone.
        expect(unknownGuidelines(['corporate/tone-of-voice', 'corporate/seo-basics', 'other/legal'], known))
            .toEqual(['corporate/tone-of-voice', 'other/legal']);
    });
});

describe('planGuideline', () => {
    const item = (extra: Partial<PlanItem>): PlanItem => ({ id: 'p1', label: 'Item', kind: 'update', workflow: 'w', status: 'pending', ...extra });
    const plan = (...items: PlanItem[]): Plan => ({ id: 'pl-1', workflows: [{ id: 'w', version: '1.0.0' }], status: 'proposed', version: 1, summary: '', items });
    const guideline = { type: 'guideline' as const, id: 'corporate/seo-basics' };

    it('is the guideline an item edits', () => {
        expect(planGuideline(plan(item({ target: { type: 'page', id: '1' } }), item({ id: 'p2', target: guideline })))).toEqual({ slug: 'corporate/seo-basics', isCreated: false });
    });

    it('is the guideline a create item wrote, as created by the plan', () => {
        expect(planGuideline(plan(item({ kind: 'create', status: 'done', created: guideline })))).toEqual({ slug: 'corporate/seo-basics', isCreated: true });
    });

    it('is none for a guideline still to be created, a sub-step, a removed item or a plan on pages only', () => {
        expect(planGuideline(plan(item({ kind: 'create', target: { type: 'folder', id: '42' } })))).toBeUndefined();
        expect(planGuideline(plan(item({ parent: 'p0', target: guideline })))).toBeUndefined();
        expect(planGuideline(plan(item({ status: 'removed', target: guideline })))).toBeUndefined();
        expect(planGuideline(plan(item({ target: { type: 'page', id: '1' } })))).toBeUndefined();
    });
});
