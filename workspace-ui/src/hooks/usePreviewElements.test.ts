import { describe, expect, it } from 'vitest';

import { isImagePart, markedElementOf, partTextOf, previewElementOf } from './usePreviewElements';

// A page as the CMS renders it in read-only mode with tagmarkers=true: a marked text part inside a marked
// container part, and unmarked page structure around them.
function page() {
    document.body.innerHTML = `
        <header id="nav"><a href="/">Home</a></header>
        <div data-gcn-pageid="9142" data-gcn-tagid="11" data-gcn-tagname="content_1" data-gcn-i18n-constructname="Section">
            <p data-gcn-pageid="9142" data-gcn-tagid="12" data-gcn-tagname="content_2" data-gcn-i18n-constructname="Text">
                Ab dem <b id="bold">1. Juli</b> gilt
            </p>
        </div>`;
}

describe('markedElementOf', () => {
    it('finds the innermost marked part of a target deep inside it', () => {
        page();

        expect(markedElementOf(document.getElementById('bold'))?.getAttribute('data-gcn-tagname')).toBe('content_2');
    });

    it('finds the part of a text node', () => {
        page();

        const text = document.getElementById('bold')!.firstChild;

        expect(markedElementOf(text)?.getAttribute('data-gcn-tagname')).toBe('content_2');
    });

    it('finds nothing outside the marked parts', () => {
        page();

        expect(markedElementOf(document.getElementById('nav'))).toBeNull();
        expect(markedElementOf(null)).toBeNull();
    });
});

describe('previewElementOf', () => {
    it('reads the tag name and the type name the CMS annotated', () => {
        page();

        expect(previewElementOf(document.querySelector('[data-gcn-tagid="12"]')!)).toEqual({ tagName: 'content_2', constructName: 'Text' });
    });

    it('leaves the type name empty when the CMS sent none', () => {
        document.body.innerHTML = '<div data-gcn-tagid="13" data-gcn-tagname="content_3"></div>';

        expect(previewElementOf(document.querySelector('[data-gcn-tagid]')!)).toEqual({ tagName: 'content_3', constructName: '' });
    });
});

describe('partTextOf', () => {
    it('reads the part\'s text, inner markup included, without the white space at its ends', () => {
        page();

        expect(partTextOf(document.querySelector('[data-gcn-tagid="12"]')!)).toBe('Ab dem 1. Juli gilt');
    });

    it('takes the text as the browser renders it where the browser has that', () => {
        document.body.innerHTML = '<p data-gcn-tagid="13" data-gcn-tagname="content_3"></p>';
        const element = document.querySelector('[data-gcn-tagid]')!;

        Object.defineProperty(element, 'innerText', { value: ' Erste Zeile\nZweite Zeile ' });

        expect(partTextOf(element)).toBe('Erste Zeile\nZweite Zeile');
    });
});

describe('isImagePart', () => {
    it('is true for an image part and for a part that holds an image, and false for a text part', () => {
        document.body.innerHTML = `
            <img data-gcn-tagid="1" data-gcn-tagname="image_1" src="/a.jpg" alt="">
            <div data-gcn-tagid="2" data-gcn-tagname="hero"><figure><img src="/b.jpg" alt=""></figure></div>
            <p data-gcn-tagid="3" data-gcn-tagname="content_2">Text</p>`;
        const part = (tagId: string) => document.querySelector(`[data-gcn-tagid="${tagId}"]`)!;

        expect(isImagePart(part('1'))).toBe(true);
        expect(isImagePart(part('2'))).toBe(true);
        expect(isImagePart(part('3'))).toBe(false);
    });
});
