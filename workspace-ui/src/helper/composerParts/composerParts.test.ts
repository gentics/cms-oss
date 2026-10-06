import { getSchema, type JSONContent } from '@tiptap/core';
import { afterEach, describe, expect, it } from 'vitest';

import { composerExtensions } from '@/components/ComposerTextInput/composerExtensions';

import { readParts, selectedRange, textContent, touchesVerbatim } from './composerParts';

const schema = getSchema(composerExtensions);

// A field's document with `content`.
function doc(...content: JSONContent[]) {
    return schema.nodeFromJSON({ type: 'doc', content });
}

const text = (value: string): JSONContent => ({ type: 'text', text: value });
const lineBreak: JSONContent = { type: 'hardBreak' };
const verbatim = (value: string): JSONContent => ({ type: 'verbatim', attrs: { text: value } });

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
