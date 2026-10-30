import { describe, expect, it } from 'vitest';

import { type Guideline, guidelineFromFile, guidelinePath, guidelineSlug, parseGuidelineSlug, sortGuidelines, unknownGuidelines } from './guidelines';

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

    it.each([
        ['no priority', 'tone-of-voice.md'],
        ['a priority that is no number', 'a_tone-of-voice.md'],
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

const guideline = (slug: string, prio: number): Guideline => ({ slug, package: 'p', name: slug, prio, isDefault: false, fileName: `${prio}_${slug}.md` });

describe('sortGuidelines', () => {
    it('sorts by priority, lowest first, then by slug, without changing the input', () => {
        const input = [guideline('p/c', 20), guideline('p/b', 10), guideline('p/a', 20)];

        expect(sortGuidelines(input).map(({ slug }) => slug)).toEqual(['p/b', 'p/a', 'p/c']);
        expect(input.map(({ slug }) => slug)).toEqual(['p/c', 'p/b', 'p/a']);
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
