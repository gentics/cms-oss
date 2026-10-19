import { getSchema, type JSONContent } from '@tiptap/core';
import { afterEach, describe, expect, it } from 'vitest';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';
import i18n from '@/i18n';
import type { ContextReference, FileMode } from '@/services/apiService/genaix/types';

import { holdsToken, pageElementEditParts, pageElementImageParts, pageElementLabel, pageElementNaming, pageElementParts, pageElementReplaceImageNaming, pageElementReplaceNaming, pageSettingsParts, partReferences, pendingFileSource, readParts, referenceContent, resolvePendingFileSources, selectedRange, textContent, tokenLabel, touchesVerbatim } from './composerParts';

const schema = getSchema(composerExtensions);

// A field's document with `content`.
function doc(...content: JSONContent[]) {
    return schema.nodeFromJSON({ type: 'doc', content });
}

const text = (value: string): JSONContent => ({ type: 'text', text: value });
const lineBreak: JSONContent = { type: 'hardBreak' };
const verbatim = (value: string): JSONContent => ({ type: 'verbatim', attrs: { text: value } });
const reference = (ref: ContextReference): JSONContent => ({ type: 'reference', attrs: { ref } });
const setting = (key: string, value: string, label = ''): JSONContent => ({ type: 'setting', attrs: { key, value, label } });
const fileRef = (fileId: string, mode: FileMode, name: string): JSONContent => ({ type: 'fileRef', attrs: { fileId, mode, name } });
const page9142: ContextReference = { type: 'page', id: '9142', node_id: 3, label: 'Neue Nutzungsbedingungen ab Q3' };
const pageElement = (tagName: string, constructName: string, naming: string): JSONContent => ({
    type: 'pageElement',
    attrs: { page: page9142, tagName, constructName, text: naming },
});

function field(html: string): HTMLElement {
    const element = document.createElement('div');

    element.innerHTML = html;
    document.body.append(element);

    return element;
}

// Selects `length` characters of `node` from `start`, as a user would.
function select(node: Node, start: number, length: number): Range {
    const range = document.createRange();

    range.setStart(node, start);
    range.setEnd(node, start + length);
    document.getSelection()!.removeAllRanges();
    document.getSelection()!.addRange(range);

    return range;
}

describe('pageElementNaming', () => {
    it('names a part with its type, or with its name alone, in the user\'s language', async () => {
        await i18n.changeLanguage('en');
        expect(pageElementNaming(i18n.t, { tagName: 'content_2', constructName: 'Text' })).toBe(' – part "content_2" (Text): ');
        expect(pageElementNaming(i18n.t, { tagName: 'content_2', constructName: '' })).toBe(' – part "content_2": ');

        await i18n.changeLanguage('de');
        expect(pageElementNaming(i18n.t, { tagName: 'content_2', constructName: 'Text' })).toBe(' – Teil "content_2" (Text): ');
        await i18n.changeLanguage('en');
    });
});

describe('pageElementParts', () => {
    it('gives the page as a reference, then the naming text and the given text as one text part', () => {
        expect(pageElementParts({ page: page9142, tagName: 'content_2', constructName: 'Text', text: ' – part "content_2" (Text): ' }, 'shorter,  more\nactive ')).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "content_2" (Text): shorter, more\nactive' },
        ]);
    });
});

describe('pageElementReplaceNaming', () => {
    it('names a part and asks to replace its text, with its type or with its name alone, in the user\'s language', async () => {
        await i18n.changeLanguage('en');
        expect(pageElementReplaceNaming(i18n.t, { tagName: 'content_2', constructName: 'Text' })).toBe(' – part "content_2" (Text), replace its text with: ');
        expect(pageElementReplaceNaming(i18n.t, { tagName: 'content_2', constructName: '' })).toBe(' – part "content_2", replace its text with: ');

        await i18n.changeLanguage('de');
        expect(pageElementReplaceNaming(i18n.t, { tagName: 'content_2', constructName: 'Text' })).toBe(' – Teil "content_2" (Text), ersetze seinen Text durch: ');
        expect(pageElementReplaceNaming(i18n.t, { tagName: 'content_2', constructName: '' })).toBe(' – Teil "content_2", ersetze seinen Text durch: ');
        await i18n.changeLanguage('en');
    });
});

describe('pageElementEditParts', () => {
    it('gives the page as a reference, the naming text, then the new text as a verbatim part, unchanged and labelled with the part', () => {
        expect(pageElementEditParts({ page: page9142, tagName: 'content_2', constructName: 'Text', text: ' – part "content_2" (Text), replace its text with: ' }, ' Ab dem  1. Juli\ngilt ')).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "content_2" (Text), replace its text with: ' },
            { type: 'verbatim', text: ' Ab dem  1. Juli\ngilt ', source: 'user', label: 'Text · content_2' },
        ]);
    });

    it('labels a part without a type by its name alone', () => {
        const [, , quote] = pageElementEditParts({ page: page9142, tagName: 'content_3', constructName: '', text: ' – part "content_3", replace its text with: ' }, 'Neu');

        expect(quote).toEqual({ type: 'verbatim', text: 'Neu', source: 'user', label: 'content_3' });
    });
});

describe('a block of a part', () => {
    const block = { index: 0, count: 2, label: 'Heading 1', excerpt: 'headline 1' };
    const headline = { tagName: 'pagecontent', constructName: 'Aloha Text (lang)', block };

    it('names the block by its place, its kind and the start of its text, after the part, in the user\'s language', async () => {
        await i18n.changeLanguage('en');
        expect(pageElementNaming(i18n.t, headline)).toBe(' – part "pagecontent" (Aloha Text (lang)), block 1 of 2 (Heading 1 "headline 1"): ');
        expect(pageElementNaming(i18n.t, { ...headline, constructName: '' })).toBe(' – part "pagecontent", block 1 of 2 (Heading 1 "headline 1"): ');
        expect(pageElementReplaceNaming(i18n.t, headline)).toBe(' – part "pagecontent" (Aloha Text (lang)), block 1 of 2 (Heading 1 "headline 1"), replace its text with: ');
        expect(pageElementReplaceNaming(i18n.t, { ...headline, constructName: '' })).toBe(' – part "pagecontent", block 1 of 2 (Heading 1 "headline 1"), replace its text with: ');

        await i18n.changeLanguage('de');
        expect(pageElementReplaceNaming(i18n.t, { ...headline, block: { ...block, label: 'Überschrift 1' } })).toBe(' – Teil "pagecontent" (Aloha Text (lang)), Block 1 von 2 (Überschrift 1 „headline 1“), ersetze seinen Text durch: ');
        await i18n.changeLanguage('en');
    });

    it('labels the block after the part, and the part alone without one', () => {
        expect(pageElementLabel(headline)).toBe('Aloha Text (lang) · pagecontent · Heading 1');
        expect(pageElementLabel({ ...headline, constructName: '' })).toBe('pagecontent · Heading 1');
        expect(pageElementLabel({ tagName: 'content_2', constructName: 'Text', block: null })).toBe('Text · content_2');
    });

    it('labels the verbatim part of a hand edit of the block with the block', () => {
        const [, , quote] = pageElementEditParts({ page: page9142, ...headline, text: ' – part "pagecontent" (Aloha Text (lang)), block 1 of 2 (Heading 1 "headline 1"), replace its text with: ' }, 'Neue Überschrift');

        expect(quote).toEqual({ type: 'verbatim', text: 'Neue Überschrift', source: 'user', label: 'Aloha Text (lang) · pagecontent · Heading 1' });
    });

    it('keeps the block on the part\'s token in the field, and shows it in the token\'s label', () => {
        const node = doc({ type: 'pageElement', attrs: { page: page9142, ...headline, text: ' – part "pagecontent" (Aloha Text (lang)), block 1 of 2 (Heading 1 "headline 1"): ' } });

        expect(node.firstChild?.attrs.block).toEqual(block);
        expect(tokenLabel(node.firstChild!)).toBe('Aloha Text (lang) · pagecontent · Heading 1');
    });
});

describe('pageElementReplaceImageNaming', () => {
    it('names a part and asks to replace its image, with its type or with its name alone, in the user\'s language', async () => {
        await i18n.changeLanguage('en');
        expect(pageElementReplaceImageNaming(i18n.t, { tagName: 'content_1', constructName: 'Image' })).toBe(' – part "content_1" (Image), replace its image with: ');
        expect(pageElementReplaceImageNaming(i18n.t, { tagName: 'content_1', constructName: '' })).toBe(' – part "content_1", replace its image with: ');

        await i18n.changeLanguage('de');
        expect(pageElementReplaceImageNaming(i18n.t, { tagName: 'content_1', constructName: 'Image' })).toBe(' – Teil "content_1" (Image), ersetze sein Bild durch: ');
        expect(pageElementReplaceImageNaming(i18n.t, { tagName: 'content_1', constructName: '' })).toBe(' – Teil "content_1", ersetze sein Bild durch: ');
        await i18n.changeLanguage('en');
    });
});

describe('pageElementImageParts', () => {
    it('gives the page as a reference, the naming text, then the picked image as a reference', () => {
        const image: ContextReference = { type: 'image', id: '4411', node_id: 3, label: 'team-workshop-2026.jpg' };

        expect(pageElementImageParts({ page: page9142, tagName: 'content_1', constructName: 'Image', text: ' – part "content_1" (Image), replace its image with: ' }, image)).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "content_1" (Image), replace its image with: ' },
            { type: 'reference', ref: image },
        ]);
    });

    it('ends at the naming text without an image, for an uploaded one that follows as the turn\'s file', () => {
        expect(pageElementImageParts({ page: page9142, tagName: 'content_1', constructName: 'Image', text: ' – part "content_1" (Image), replace its image with: ' })).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "content_1" (Image), replace its image with: ' },
        ]);
    });
});

describe('pageSettingsParts', () => {
    const template: ContextReference = { type: 'template', id: '21', node_id: 3, label: 'Themenseite' };

    it('gives the page, the request to change its settings, then each change: its label and the value as written, the template as a reference', async () => {
        await i18n.changeLanguage('en');
        expect(pageSettingsParts(i18n.t, page9142, [{ label: 'Name', value: 'Terms  of use' }, { label: 'Template', value: template }])).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – change its page settings:' },
            { type: 'text', text: '\nName: ' },
            { type: 'verbatim', text: 'Terms  of use', source: 'user', label: 'Name' },
            { type: 'text', text: '\nTemplate: ' },
            { type: 'reference', ref: template },
        ]);
    });

    it('asks to empty an emptied setting, in the user\'s language', async () => {
        await i18n.changeLanguage('de');
        expect(pageSettingsParts(i18n.t, page9142, [{ label: 'Nice URL', value: '' }])).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – ändere seine Seiteneinstellungen:' },
            { type: 'text', text: '\nNice URL: leeren' },
        ]);
        await i18n.changeLanguage('en');
    });
});

describe('readParts', () => {
    it('reads a page part as a reference to its page, its naming text joined with what follows', () => {
        expect(readParts(doc(pageElement('content_2', 'Text', ' – part "content_2" (Text): '), text('make this shorter')))).toEqual([
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "content_2" (Text): make this shorter' },
        ]);
    });

    it('ends the text before a page part and keeps its naming text when nothing follows', () => {
        expect(readParts(doc(text('Look at '), pageElement('hero', 'Image', ' – part "hero" (Image): ')))).toEqual([
            { type: 'text', text: 'Look at ' },
            { type: 'reference', ref: page9142 },
            { type: 'text', text: ' – part "hero" (Image):' },
        ]);
    });

    it('gives no parts for an empty or blank field', () => {
        expect(readParts(doc())).toEqual([]);
        expect(readParts(doc(text('  '), lineBreak))).toEqual([]);
    });

    it('reads typed text as one text part, collapsing spaces and trimming the ends', () => {
        expect(readParts(doc(text('  Which   pages are offline? ')))).toEqual([{ type: 'text', text: 'Which pages are offline?' }]);
    });

    it('keeps line breaks', () => {
        expect(readParts(doc(text('First'), lineBreak, text('second'), lineBreak, text('third')))).toEqual([{ type: 'text', text: 'First\nsecond\nthird' }]);
    });

    it('reads a locked passage as a verbatim part with its text unchanged', () => {
        expect(readParts(doc(text('Use exactly this title: '), verbatim('Review  now')))).toEqual([
            { type: 'text', text: 'Use exactly this title: ' },
            { type: 'verbatim', text: 'Review  now', source: 'user' },
        ]);
    });

    it('sends where a passage was quoted from as its source', () => {
        const quoted: JSONContent = { type: 'verbatim', attrs: { text: 'Review now', source: '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02' } };

        expect(readParts(doc(quoted))).toEqual([{ type: 'verbatim', text: 'Review now', source: '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02' }]);
    });

    it('reads a reference token as a reference part with its ref unchanged', () => {
        const ref: ContextReference = { type: 'page', id: '8871', node_id: 3, label: 'Product launch 2025' };

        expect(readParts(doc(text('Shorten '), reference(ref)))).toEqual([
            { type: 'text', text: 'Shorten ' },
            { type: 'reference', ref },
        ]);
    });

    it('reads a setting token as a setting part, without an empty label', () => {
        expect(readParts(doc(setting('language', 'de', 'German'), text(' and '), setting('publish_at', '2026-11-01T08:00:00Z')))).toEqual([
            { type: 'setting', key: 'language', value: 'de', label: 'German' },
            { type: 'text', text: ' and ' },
            { type: 'setting', key: 'publish_at', value: '2026-11-01T08:00:00Z' },
        ]);
    });

    it('reads a file token as a file_ref part at its place, without its name', () => {
        expect(readParts(doc(text('Summarise '), fileRef('6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02', 'verbatim', 'brief.pdf'), text(' briefly')))).toEqual([
            { type: 'text', text: 'Summarise ' },
            { type: 'file_ref', file_id: '6b0d9e31-44a8-4c57-9f2e-8d1a3c7b5e02', mode: 'verbatim' },
            { type: 'text', text: ' briefly' },
        ]);
    });

    it('gives a message of only a token', () => {
        expect(readParts(doc(text(' '), setting('language', 'de', 'German'), text(' ')))).toEqual([
            { type: 'setting', key: 'language', value: 'de', label: 'German' },
        ]);
    });

    // 09-integration-guide.md, "Composing messages from the Workspace", worked example.
    it('serialises the worked example of the integration guide exactly', () => {
        const field = doc(
            text('Create a new page under '),
            reference({ type: 'folder', id: '42', node_id: 3, label: 'Campaigns' }),
            text(' in '),
            setting('language', 'de', 'German'),
            text(', based on '),
            reference({ type: 'page', id: '8871', node_id: 3, label: 'Product launch 2025' }),
            text('. Use exactly this title: '),
            verbatim('This is our Landingpage'),
        );

        expect(readParts(field)).toEqual([
            { type: 'text', text: 'Create a new page under ' },
            { type: 'reference', ref: { type: 'folder', id: '42', node_id: 3, label: 'Campaigns' } },
            { type: 'text', text: ' in ' },
            { type: 'setting', key: 'language', value: 'de', label: 'German' },
            { type: 'text', text: ', based on ' },
            { type: 'reference', ref: { type: 'page', id: '8871', node_id: 3, label: 'Product launch 2025' } },
            { type: 'text', text: '. Use exactly this title: ' },
            { type: 'verbatim', text: 'This is our Landingpage', source: 'user' },
        ]);
    });
});

describe('resolvePendingFileSources', () => {
    it('points a passage quoting a submitted file at that file\'s uploaded id, and leaves the rest', () => {
        const parts = [
            { type: 'verbatim', text: 'First', source: pendingFileSource(1) },
            { type: 'verbatim', text: 'Second', source: 'user' },
            { type: 'text', text: 'and' },
        ] as const;

        expect(resolvePendingFileSources([...parts], ['file-a', 'file-b'])).toEqual([
            { type: 'verbatim', text: 'First', source: 'file-b' },
            { type: 'verbatim', text: 'Second', source: 'user' },
            { type: 'text', text: 'and' },
        ]);
    });
});

describe('partReferences', () => {
    it('is the referenced objects in order, each once, without the other parts', () => {
        const page: ContextReference = { type: 'page', id: '8871', node_id: 3, label: 'Product launch 2025' };
        const folder: ContextReference = { type: 'folder', id: '42', node_id: 3, label: 'Campaigns' };
        const samePageOtherNode: ContextReference = { type: 'page', id: '8871', node_id: 4 };

        expect(partReferences([
            { type: 'text', text: 'Compare ' },
            { type: 'reference', ref: page },
            { type: 'text', text: ' in ' },
            { type: 'reference', ref: folder },
            { type: 'setting', key: 'language', value: 'de' },
            { type: 'reference', ref: { ...page } },
            { type: 'reference', ref: samePageOtherNode },
        ])).toEqual([page, folder, samePageOtherNode]);
    });

    it('is empty without reference parts', () => {
        expect(partReferences([{ type: 'text', text: 'Which pages are offline?' }])).toEqual([]);
    });
});

describe('textContent', () => {
    it('takes the text literally, with line breaks and without empty text nodes', () => {
        expect(readParts(doc(...textContent('a <b>x</b>\n\nend')))).toEqual([{ type: 'text', text: 'a <b>x</b>\n\nend' }]);
        expect(textContent('')).toEqual([]);
    });
});

describe('referenceContent', () => {
    it('gives a reference token per object, which the field reads back as reference parts', () => {
        const pages: ContextReference[] = [
            { type: 'page', id: '8871', node_id: 3, label: 'Garantiebedingungen' },
            { type: 'page', id: '8903', node_id: 3, label: 'Nutzungsbedingungen' },
        ];

        expect(readParts(doc(...referenceContent(pages), { type: 'text', text: 'publish these' }))).toEqual([
            { type: 'reference', ref: pages[0] },
            { type: 'text', text: ' ' },
            { type: 'reference', ref: pages[1] },
            { type: 'text', text: ' publish these' },
        ]);
        expect(referenceContent([])).toEqual([]);
    });
});

describe('touchesVerbatim', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('refuses a selection that already holds a passage', () => {
        const element = field('Keep <span data-verbatim=""><span data-verbatim-text="">this</span></span> as it is');
        const range = document.createRange();

        range.selectNodeContents(element);

        expect(touchesVerbatim(range)).toBe(true);
        expect(touchesVerbatim(select(element.firstChild!, 0, 4))).toBe(false);
    });
});

describe('holdsToken', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('is true only for a selection that contains a token', () => {
        const element = field('See <span data-token="reference"><span data-token-label="">Campaigns</span></span> now');
        const range = document.createRange();

        range.selectNodeContents(element);

        expect(holdsToken(range)).toBe(true);
        expect(holdsToken(select(element.firstChild!, 0, 3))).toBe(false);
    });
});

describe('selectedRange', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('is the selection inside the field, and null for a collapsed, blank or outside selection', () => {
        const element = field('Some text');
        const outside = field('Elsewhere');

        expect(selectedRange(element)).toBeNull();

        select(element.firstChild!, 0, 4);
        expect(selectedRange(element)?.toString()).toBe('Some');

        select(element.firstChild!, 4, 1);
        expect(selectedRange(element)).toBeNull();

        select(outside.firstChild!, 0, 4);
        expect(selectedRange(element)).toBeNull();
    });
});
