import { describe, expect, it } from 'vitest';

import { blockOf, blocksOf, isImagePart, markedElementOf, partTextOf, previewElementOf } from './usePreviewElements';

// A page as the CMS renders it with tagmarkers=true: a marked text part inside a marked
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

    it('is false for a text part whose only image belongs to a marked part inside it', () => {
        // An image block added to a text editable: the image belongs to the block, not to the text.
        document.body.innerHTML = `
            <div data-gcn-tagid="890" data-gcn-tagname="pagecontent"><p>Text</p>
                <div data-gcn-tagid="893" data-gcn-tagname="manimage_1" class="aloha-block">
                    <figure><img src="/a.jpg" alt=""></figure>
                </div>
            </div>`;
        const part = (tagId: string) => document.querySelector(`[data-gcn-tagid="${tagId}"]`)!;

        expect(isImagePart(part('890'))).toBe(false);
        expect(isImagePart(part('893'))).toBe(true);
    });
});

describe('blocksOf', () => {
    // A text part holding a headline and a paragraph, as GenAIx writes a new page into one Aloha text.
    function textPart(html: string) {
        document.body.innerHTML = `<div data-gcn-tagid="932" data-gcn-tagname="pagecontent" data-gcn-i18n-constructname="Aloha Text (lang)">${html}</div>`;

        return document.querySelector('[data-gcn-tagid]')!;
    }

    it('splits a text part into its headings, paragraphs, quotes, code, lists and tables', () => {
        const part = textPart('<h1>headline 1</h1> loose text <p>Lorem</p><br><blockquote>Q</blockquote><pre>C</pre><ul><li>L</li></ul><table><tr><td>T</td></tr></table><div>other</div>');

        expect(blocksOf(part).map((block) => block.localName)).toEqual(['h1', 'p', 'blockquote', 'pre', 'ul', 'table']);
    });

    it('does not split a part with a single block, one whose only other block is a marked part, or one that shows an image', () => {
        expect(blocksOf(textPart('<p>Only one</p>'))).toEqual([]);
        expect(blocksOf(textPart('<p>Text</p><p data-gcn-tagid="933" data-gcn-tagname="content_4">Inner</p>'))).toEqual([]);
        expect(blocksOf(textPart('<p>Text</p><p><img src="/a.jpg" alt=""></p>'))).toEqual([]);
    });

    it('splits a text part around an image block in it, leaving out empty paragraphs and the image block', () => {
        // As the CMS renders a new page: an image block (a marked part of its own) between the text, with empty paragraphs around it.
        const part = textPart(`<p>Die Gemeinde Bruchtal unterstützt Kleinbetriebe.</p><p>Die Förderung übernimmt 50 %.</p><p></p><div data-gcn-tagid="970" data-gcn-tagname="manimage_1" data-gcn-i18n-constructname="[Handbuch] Bild" class="aloha-block">
            <figure><img src="/a.jpg" alt="Infografik"></figure>
        </div><p></p><h2>Wer kann einen Antrag stellen?</h2><ul><li>Betriebe</li></ul><p>Gemeindeamt Bruchtal<br>Hauptplatz 1</p>`);

        expect(blocksOf(part).map((block) => block.textContent)).toEqual([
            'Die Gemeinde Bruchtal unterstützt Kleinbetriebe.',
            'Die Förderung übernimmt 50 %.',
            'Wer kann einen Antrag stellen?',
            'Betriebe',
            'Gemeindeamt BruchtalHauptplatz 1',
        ]);
        // The image block stays a part of its own: a click there selects it, not a block of the text.
        expect(markedElementOf(part.querySelector('img'))?.getAttribute('data-gcn-tagname')).toBe('manimage_1');
    });

    it('finds the block a target is in, and none outside the blocks', () => {
        const part = textPart('<h1>headline <b id="bold">1</b></h1><p>Lorem</p>');

        expect(blockOf(document.getElementById('bold')!.firstChild, part)?.localName).toBe('h1');
        expect(blockOf(part, part)).toBeNull();
        expect(blockOf(null, part)).toBeNull();
    });

    it('describes a block by its place, its kind and the start of its text', () => {
        const part = textPart('<h1>headline 1</h1><p>Lorem ipsum dolor sit amet,   consectetur adipiscing elit, sed do</p>');
        const [headline, paragraph] = blocksOf(part);

        expect(previewElementOf(part, headline)).toEqual({
            tagName: 'pagecontent',
            constructName: 'Aloha Text (lang)',
            block: { index: 0, count: 2, kind: 'heading1', excerpt: 'headline 1' },
        });
        expect(previewElementOf(part, paragraph).block).toEqual({ index: 1, count: 2, kind: 'paragraph', excerpt: 'Lorem ipsum dolor sit amet, consectetur…' });
        expect(previewElementOf(part, null)).toEqual({ tagName: 'pagecontent', constructName: 'Aloha Text (lang)' });
    });
});
