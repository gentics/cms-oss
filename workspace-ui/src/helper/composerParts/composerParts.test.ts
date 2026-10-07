import { getSchema, type JSONContent } from '@tiptap/core';
import { afterEach, describe, expect, it } from 'vitest';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';
import type { ContextReference, FileMode } from '@/services/apiService/genaix/types';

import { holdsToken, pendingFileSource, readParts, resolvePendingFileSources, selectedRange, textContent, touchesVerbatim } from './composerParts';

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

describe('readParts', () => {
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

describe('textContent', () => {
    it('takes the text literally, with line breaks and without empty text nodes', () => {
        expect(readParts(doc(...textContent('a <b>x</b>\n\nend')))).toEqual([{ type: 'text', text: 'a <b>x</b>\n\nend' }]);
        expect(textContent('')).toEqual([]);
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
