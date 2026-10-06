import { afterEach, describe, expect, it } from 'vitest';

import { markVerbatim, readParts, selectedRange, unmarkVerbatim } from './composerParts';

const spanOptions = {
    iconMarkup: '<i data-icon></i>',
    removeButtonMarkup: '<button type="button" aria-label="Remove verbatim"></button>',
};

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
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('gives no parts for an empty or blank field', () => {
        expect(readParts(field(''))).toEqual([]);
        expect(readParts(field('  <br>'))).toEqual([]);
    });

    it('reads typed text as one text part, collapsing spaces and trimming the ends', () => {
        expect(readParts(field('  Which   pages are offline? '))).toEqual([{ type: 'text', text: 'Which pages are offline?' }]);
    });

    it('keeps line breaks from <br> and from the blocks a browser inserts', () => {
        expect(readParts(field('First<br>second<div>third</div>'))).toEqual([{ type: 'text', text: 'First\nsecond\nthird' }]);
    });

    it('reads a locked passage as a verbatim part with its text unchanged', () => {
        const element = field('Use exactly this title: Review  now');

        markVerbatim(select(element.firstChild!, 24, 11), spanOptions);

        expect(readParts(element)).toEqual([
            { type: 'text', text: 'Use exactly this title: ' },
            { type: 'verbatim', text: 'Review  now', source: 'user' },
        ]);
    });
});

describe('markVerbatim', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('wraps the selection in a non-editable passage with a remove button', () => {
        const element = field('Keep this as it is');

        expect(markVerbatim(select(element.firstChild!, 5, 4), spanOptions)).toBe(true);

        const span = element.querySelector('[data-verbatim]')!;

        expect(span.getAttribute('contenteditable')).toBe('false');
        expect(span.querySelector('[data-verbatim-text]')?.textContent).toBe('this');
        expect(span.querySelector('button')?.getAttribute('aria-label')).toBe('Remove verbatim');
        expect(readParts(element).map((part) => part.type)).toEqual(['text', 'verbatim', 'text']);
    });

    it('refuses a selection that already holds a passage', () => {
        const element = field('Keep this as it is');

        markVerbatim(select(element.firstChild!, 5, 4), spanOptions);

        const range = document.createRange();

        range.selectNodeContents(element);

        expect(markVerbatim(range, spanOptions)).toBe(false);
        expect(element.querySelectorAll('[data-verbatim]')).toHaveLength(1);
    });

    it('unmarkVerbatim turns the passage back into plain text', () => {
        const element = field('Keep this as it is');

        markVerbatim(select(element.firstChild!, 5, 4), spanOptions);
        unmarkVerbatim(element.querySelector<HTMLElement>('[data-verbatim]')!);

        expect(element.querySelector('[data-verbatim]')).toBeNull();
        expect(readParts(element)).toEqual([{ type: 'text', text: 'Keep this as it is' }]);
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
