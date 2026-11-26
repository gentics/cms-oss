import { Editor } from '@tiptap/core';
import { afterEach, describe, expect, it } from 'vitest';

import { hrefOf, inlineEditorExtensions, type InlineEditorSchemaOptions, isSupportedContent, withHref } from './inlineEditorSchema';

const RICH: InlineEditorSchemaOptions = { marks: true, fixedSelector: '[data-fixed]' };
const editors: Editor[] = [];

// The HTML the editor writes for `html`, read with `options`.
function roundTrip(html: string, options: InlineEditorSchemaOptions = RICH): string {
    const editor = new Editor({ extensions: inlineEditorExtensions(options), content: html });

    editors.push(editor);

    return editor.getHTML();
}

afterEach(() => {
    editors.splice(0).forEach((editor) => editor.destroy());
});

describe('inlineEditorExtensions', () => {
    it('writes back the marks with the tags they were read from', () => {
        const html = 'a <b>b</b> <strong>c</strong> <i>d</i> <em>e</em> <u>f</u> <s>g</s> <del>h</del> <sub>i</sub> <sup>j</sup>';

        expect(roundTrip(html)).toBe(html);
    });

    it('writes back a link with all its attributes in their order, and line breaks', () => {
        const html = 'Ab <a class="ext" href="https://example.com/x" target="_blank">dem</a><br>1. Juli';

        expect(roundTrip(html)).toBe(html);
    });

    it('keeps a fixed element as it is, a fixed link included, with what it holds', () => {
        const html = 'Siehe <a data-fixed="" data-id="7" href="/page">Kontakt</a> und <span data-fixed=""><b>x</b></span>.';

        expect(roundTrip(html)).toBe(html);
    });

    it('reads only text and line breaks without marks', () => {
        expect(roundTrip('a <b>b</b><br>c', { marks: false, fixedSelector: '' })).toBe('a b<br>c');
    });
});

describe('isSupportedContent', () => {
    function element(html: string): Element {
        const div = document.createElement('div');

        div.innerHTML = html;

        return div;
    }

    it('takes text, line breaks, the marks, nested ones, links and fixed elements', () => {
        expect(isSupportedContent(element('a<br><b>b <i>c</i></b> <a href="/x"><sup>1</sup></a> <span data-fixed=""><img alt=""></span>'), RICH)).toBe(true);
    });

    it('refuses any other element, a mark with attributes, and marks or links without marks', () => {
        expect(isSupportedContent(element('a <span>b</span>'), RICH)).toBe(false);
        expect(isSupportedContent(element('a <b class="x">b</b>'), RICH)).toBe(false);
        expect(isSupportedContent(element('<b>a <img alt=""></b>'), RICH)).toBe(false);
        expect(isSupportedContent(element('<a name="anchor">a</a>'), RICH)).toBe(false);
        expect(isSupportedContent(element('a <b>b</b>'), { marks: false, fixedSelector: '' })).toBe(false);
        expect(isSupportedContent(element('a<br>b'), { marks: false, fixedSelector: '' })).toBe(true);
    });
});

describe('hrefOf and withHref', () => {
    it('reads a link\'s href and sets it in place, or adds it', () => {
        expect(hrefOf([['class', 'x'], ['href', '/a']])).toBe('/a');
        expect(hrefOf([])).toBe('');
        expect(withHref([['class', 'x'], ['href', '/a'], ['target', '_blank']], '/b')).toEqual([['class', 'x'], ['href', '/b'], ['target', '_blank']]);
        expect(withHref([], '/b')).toEqual([['href', '/b']]);
    });
});
