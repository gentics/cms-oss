import i18n from 'i18next';
import { beforeAll, describe, expect, it } from 'vitest';

import { elementLabel, elementLink, elementName, elementTextBlock } from './previewElement';

import '@/i18n';

const part = { tagName: 'pagecontent', constructName: 'Aloha Text (lang)' };
const heading = { ...part, textBlock: { index: 0, count: 2, kind: 'heading1' as const, excerpt: 'Willkommen' } };
const link = { ...part, link: { index: 1, count: 3, excerpt: 'Kontakt', href: '/kontakt' } };

beforeAll(async () => {
    await i18n.changeLanguage('en');
});

describe('previewElement', () => {
    it('names a whole part by its construct, else by its tag', () => {
        expect(elementName(i18n.t, part)).toBe('Aloha Text (lang)');
        expect(elementName(i18n.t, { tagName: 'content_2', constructName: '' })).toBe('content_2');
        expect(elementLabel(i18n.t, part)).toBe('Aloha Text (lang) · pagecontent');
        expect(elementTextBlock(i18n.t, part)).toBeUndefined();
        expect(elementLink(i18n.t, part)).toBeUndefined();
    });

    it('names a text block by its kind in front, and gives it in the form the chat uses', () => {
        expect(elementName(i18n.t, heading)).toBe('Heading 1 · Aloha Text (lang)');
        expect(elementLabel(i18n.t, heading)).toBe('Aloha Text (lang) · pagecontent · Heading 1');
        expect(elementTextBlock(i18n.t, heading)).toEqual({ index: 0, count: 2, label: 'Heading 1', excerpt: 'Willkommen' });
    });

    it('names a link as "Link" in front, and gives it in the form the chat uses', () => {
        expect(elementName(i18n.t, link)).toBe('Link · Aloha Text (lang)');
        expect(elementLink(i18n.t, link)).toEqual({ index: 1, count: 3, label: 'Link', excerpt: 'Kontakt' });
    });
});
