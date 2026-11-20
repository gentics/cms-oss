import { describe, expect, it } from 'vitest';

import type { CmsTag } from '@/services/cmsApiService/cmsApiService';

import { applyPlan, editableIdOf, editLeafOf, planHtmlEdit, planImageEdit, planLinkEdit, planTextEdit, storedValueOf } from './cmsEditable';

// Page 32 of the devtest CMS as the preview renders it with tagmarkers=true (2026-10-28), the editables
// marked as the deployed CMS marks them: a title editable holding a headline block, and the page's text
// holding a paragraph and a link tag.
function page32() {
    document.body.innerHTML = `
        <h1><div data-gcn-pageid="32" data-gcn-tagid="889" data-gcn-tagname="title" data-gcn-i18n-constructname="Aloha Text" class="GENTICS_construct_alohatext GENTICS_tagname_title GENTICS_tagpart_text GENTICS_parttype_html_long" id="GENTICS_EDITABLE_3096"><div data-gcn-pageid="32" data-gcn-tagid="892" data-gcn-tagname="mantitle_e07e" data-gcn-i18n-constructname="[Handbuch] Unterüberschrift" class="aloha-block" id="GENTICS_BLOCK_892">
    <h2>headline</h2>
</div></div></h1>
        <section><div data-gcn-pageid="32" data-gcn-tagid="888" data-gcn-tagname="pagecontent" data-gcn-i18n-constructname="Aloha Text" class="GENTICS_construct_alohatext GENTICS_tagname_pagecontent GENTICS_tagpart_text GENTICS_parttype_html_long" id="GENTICS_EDITABLE_3095"><p>Lorem ipsum dolor sit amet.</p>
<p><a data-gcn-pageid="32" data-gcn-tagid="891" data-gcn-tagname="gtxalohapagelink_6cfd" data-gcn-i18n-constructname="Aloha Link" id="GENTICS_BLOCK_891" href="/alohapage?nodeid=1&amp;language=1&amp;real=newview&amp;realid=1">home</a></p></div></section>`;

    return {
        title: document.getElementById('GENTICS_EDITABLE_3096')!,
        headline: document.getElementById('GENTICS_BLOCK_892')!,
        content: document.getElementById('GENTICS_EDITABLE_3095')!,
        lorem: document.getElementById('GENTICS_EDITABLE_3095')!.firstElementChild!,
        linkTag: document.getElementById('GENTICS_BLOCK_891')!,
    };
}

// The tags of page 32 as `GET /rest/page/load/32?nodeId=1` answered them (shortened).
function tags32(): Record<string, CmsTag> {
    return {
        pagecontent: { id: 888, name: 'pagecontent', constructId: 1, active: true, properties: { text: { id: 3095, type: 'RICHTEXT', stringValue: '<p>Lorem ipsum dolor sit amet.</p>\n<p><node gtxalohapagelink_6cfd></p>' } } },
        title: { id: 889, name: 'title', constructId: 1, active: true, properties: { text: { id: 3096, type: 'RICHTEXT', stringValue: '<node mantitle_e07e>' } } },
        mantitle_e07e: { id: 892, name: 'mantitle_e07e', constructId: 16, active: true, properties: { title: { id: 3106, type: 'STRING', stringValue: 'headline' } } },
        gtxalohapagelink_6cfd: {
            id: 891,
            name: 'gtxalohapagelink_6cfd',
            constructId: 4,
            active: true,
            properties: {
                text: { id: 3098, type: 'STRING', stringValue: 'home' },
                url: { id: 3099, type: 'PAGE', pageId: 1 },
                anchor: { id: 3101, type: 'STRING', stringValue: '' },
            },
        },
    };
}

describe('storedValueOf', () => {
    it('gives an editable as the CMS stores it, its embedded tags as <node NAME>', () => {
        const { title, content } = page32();
        const tags = tags32();

        expect(storedValueOf(content)).toBe(tags.pagecontent!.properties!.text!.stringValue);
        expect(storedValueOf(title)).toBe(tags.title!.properties!.text!.stringValue);
        expect(editableIdOf(content)).toBe(3095);
        expect(editableIdOf(content.firstElementChild!)).toBeNull();
    });
});

describe('storedValueOf, while editing in place', () => {
    it('leaves out the attribute that makes an element editable in place', () => {
        const { content } = page32();

        content.firstElementChild!.setAttribute('contenteditable', 'plaintext-only');

        expect(storedValueOf(content)).toBe(tags32().pagecontent!.properties!.text!.stringValue);
    });
});

describe('planTextEdit and applyPlan', () => {
    it('writes new text of a paragraph into its editable, the rest of the editable as stored', () => {
        const { content, lorem } = page32();
        const plan = planTextEdit(content, lorem, 'Neuer Text\nzweite Zeile');

        expect(plan).toEqual({ kind: 'value', propertyId: 3095, value: '<p>Neuer Text<br>zweite Zeile</p>\n<p><node gtxalohapagelink_6cfd></p>' });
        expect(applyPlan(tags32(), plan)).toEqual({
            tag: { id: 888, name: 'pagecontent', constructId: 1, active: true, properties: { text: { type: 'RICHTEXT', stringValue: '<p>Neuer Text<br>zweite Zeile</p>\n<p><node gtxalohapagelink_6cfd></p>' } } },
        });
        // The page itself is not changed.
        expect(lorem.textContent).toBe('Lorem ipsum dolor sit amet.');
    });

    it('writes the text of an editable with a single paragraph into that paragraph', () => {
        document.body.innerHTML = '<div data-gcn-tagid="12" data-gcn-tagname="content_2" id="GENTICS_EDITABLE_312">\n    <p>Ab dem 1. Juli gilt</p>\n</div>';
        const editable = document.getElementById('GENTICS_EDITABLE_312')!;

        expect(planTextEdit(editable, editable, 'Ab dem 1. August gilt')).toEqual({ kind: 'value', propertyId: 312, value: '\n    <p>Ab dem 1. August gilt</p>\n' });
    });

    it('does not replace text that holds formatting, links or embedded tags', () => {
        const { content } = page32();

        expect(planTextEdit(content, content, 'Alles neu')).toEqual({ kind: 'refused', reason: 'formatting' });
    });

    it('writes new text of an embedded block through its tag\'s only text part', () => {
        const { headline } = page32();
        const plan = planTextEdit(headline, headline, 'Neue Überschrift');

        expect(plan).toEqual({ kind: 'text', tagName: 'mantitle_e07e', text: 'Neue Überschrift' });
        expect(applyPlan(tags32(), plan)).toEqual({
            tag: { id: 892, name: 'mantitle_e07e', constructId: 16, active: true, properties: { title: { type: 'STRING', stringValue: 'Neue Überschrift' } } },
        });
    });

    it('saves nothing for an unchanged text', () => {
        const { headline } = page32();

        expect(applyPlan(tags32(), planTextEdit(headline, headline, 'headline'))).toEqual({ unchanged: true });
    });

    it('refuses a tag whose text part is not clear, and a part the page no longer has', () => {
        const { linkTag } = page32();

        expect(applyPlan(tags32(), planTextEdit(linkTag, linkTag, 'x'))).toEqual({ refused: 'unclearPart' });
        expect(applyPlan(tags32(), { kind: 'value', propertyId: 9999, value: 'x' })).toEqual({ refused: 'notFound' });
        expect(applyPlan(tags32(), { kind: 'text', tagName: 'gone', text: 'x' })).toEqual({ refused: 'notFound' });
    });

    it('refuses a text block of a part that is no editable', () => {
        document.body.innerHTML = '<div data-gcn-tagid="5" data-gcn-tagname="teaser"><p id="a">A</p><p id="b">B</p></div>';

        expect(planTextEdit(document.querySelector('[data-gcn-tagid]')!, document.getElementById('b')!, 'x')).toEqual({ kind: 'refused', reason: 'noEditable' });
    });

    it('writes a rich text part of a tag as escaped HTML while its value has no markup', () => {
        const tags = { teaser: { id: 5, name: 'teaser', active: false, properties: { text: { id: 7, type: 'RICHTEXT', stringValue: 'alt' } } } };

        expect(applyPlan(tags, { kind: 'text', tagName: 'teaser', text: 'a < b\nc' })).toEqual({
            tag: { id: 5, name: 'teaser', constructId: undefined, active: false, properties: { text: { type: 'RICHTEXT', stringValue: 'a &lt; b<br>c' } } },
        });
        expect(applyPlan({ teaser: { ...tags.teaser, properties: { text: { id: 7, type: 'RICHTEXT', stringValue: '<b>alt</b>' } } } }, { kind: 'text', tagName: 'teaser', text: 'x' })).toEqual({ refused: 'formatting' });
    });
});

// An editable holding a paragraph, a list (one item with a nested list, one with bold text) and a table.
function listsAndTable() {
    document.body.innerHTML = '<div data-gcn-tagid="12" data-gcn-tagname="content_2" id="GENTICS_EDITABLE_312">'
        + '<p id="intro">Intro</p>'
        + '<ul id="list"><li id="first">Erstens</li><li id="second">Zweitens<ul><li id="nested">Darunter</li></ul></li><li id="bold"><b>Fett</b></li></ul>'
        + '<table id="table"><tbody><tr><th id="head">Tag</th><td id="cell">Dienstag</td></tr></tbody></table></div>';

    const byId = (id: string) => document.getElementById(id)!;

    return { editable: byId('GENTICS_EDITABLE_312'), byId };
}

describe('editLeafOf', () => {
    it('gives the innermost list item or table cell the click is in', () => {
        const { byId } = listsAndTable();

        expect(editLeafOf(byId('list'), byId('first'))).toBe(byId('first'));
        expect(editLeafOf(byId('list'), byId('first').firstChild)).toBe(byId('first'));
        expect(editLeafOf(byId('list'), byId('nested'))).toBe(byId('nested'));
        expect(editLeafOf(byId('list'), byId('bold').firstElementChild)).toBe(byId('bold'));
        expect(editLeafOf(byId('table'), byId('cell'))).toBe(byId('cell'));
        expect(editLeafOf(byId('table'), byId('head'))).toBe(byId('head'));
    });

    it('gives the target itself without a click, for a click outside it, and outside a list or table', () => {
        const { editable, byId } = listsAndTable();

        expect(editLeafOf(byId('list'), null)).toBe(byId('list'));
        expect(editLeafOf(byId('list'), byId('cell'))).toBe(byId('list'));
        expect(editLeafOf(byId('intro'), byId('intro'))).toBe(byId('intro'));
        // A list item around the target is no leaf of it.
        expect(editLeafOf(byId('nested').parentElement!, byId('second'))).toBe(byId('nested').parentElement);
        expect(editLeafOf(editable, byId('first'))).toBe(byId('first'));
    });
});

describe('planTextEdit for a list item or table cell', () => {
    it('writes the new text into that item or cell only, the list or table and the rest of the editable as stored', () => {
        const { editable, byId } = listsAndTable();
        const stored = storedValueOf(editable);

        expect(planTextEdit(editable, byId('first'), 'Neu')).toEqual({ kind: 'value', propertyId: 312, value: stored.replace('>Erstens<', '>Neu<') });
        expect(planTextEdit(editable, byId('cell'), 'Mittwoch')).toEqual({ kind: 'value', propertyId: 312, value: stored.replace('>Dienstag<', '>Mittwoch<') });
        expect(planTextEdit(editable, byId('nested'), 'Unten')).toEqual({ kind: 'value', propertyId: 312, value: stored.replace('>Darunter<', '>Unten<') });
    });

    it('refuses an item with formatting or a nested list, and the whole list or table', () => {
        const { editable, byId } = listsAndTable();

        expect(planTextEdit(editable, byId('bold'), 'x')).toEqual({ kind: 'refused', reason: 'formatting' });
        expect(planTextEdit(editable, byId('second'), 'x')).toEqual({ kind: 'refused', reason: 'formatting' });
        expect(planTextEdit(editable, byId('list'), 'x')).toEqual({ kind: 'refused', reason: 'formatting' });
        expect(planTextEdit(editable, byId('table'), 'x')).toEqual({ kind: 'refused', reason: 'formatting' });
    });

    it('refuses an item of a list in a part that is no editable', () => {
        document.body.innerHTML = '<div data-gcn-tagid="7" data-gcn-tagname="facts"><h3>Fakten</h3><ul><li id="item">Drei Tage</li></ul></div>';

        expect(planTextEdit(document.querySelector('[data-gcn-tagid]')!, document.getElementById('item')!, 'x')).toEqual({ kind: 'refused', reason: 'noEditable' });
    });
});

describe('planHtmlEdit and applyPlan', () => {
    it('writes the editor\'s HTML into the paragraph, with its attributes from before, the rest of the editable as stored', () => {
        const { content, lorem } = page32();

        // What the editor adds to the element while it is on it.
        lorem.setAttribute('contenteditable', 'true');
        lorem.setAttribute('class', 'tiptap ProseMirror');

        const plan = planHtmlEdit(content, lorem, 'Neuer <b>Text</b><br>mit <a class="ext" href="https://example.com">Link</a>', []);

        expect(plan).toEqual({ kind: 'value', propertyId: 3095, value: '<p>Neuer <b>Text</b><br>mit <a class="ext" href="https://example.com">Link</a></p>\n<p><node gtxalohapagelink_6cfd></p>' });
        expect(applyPlan(tags32(), plan)).toEqual({
            tag: { ...tags32().pagecontent, properties: { text: { type: 'RICHTEXT', stringValue: (plan as { value: string }).value } } },
        });
    });

    it('writes a tag embedded in the paragraph, kept by the editor, back as <node NAME>', () => {
        const { content, linkTag } = page32();
        const paragraph = linkTag.parentElement!;

        expect(planHtmlEdit(content, paragraph, `Zur ${linkTag.outerHTML} Seite`, [['data-x', '1']])).toEqual({
            kind: 'value',
            propertyId: 3095,
            value: '<p>Lorem ipsum dolor sit amet.</p>\n<p data-x="1">Zur <node gtxalohapagelink_6cfd> Seite</p>',
        });
    });

    it('writes a list item on its own, and refuses outside an editable', () => {
        const { editable, byId } = listsAndTable();

        expect(planHtmlEdit(editable, byId('bold'), '<i>Kursiv</i>', [['id', 'bold']])).toEqual({
            kind: 'value',
            propertyId: 312,
            value: storedValueOf(editable).replace('<b>Fett</b>', '<i>Kursiv</i>'),
        });

        document.body.innerHTML = '<div data-gcn-tagid="7" data-gcn-tagname="facts"><p id="p">A</p></div>';
        expect(planHtmlEdit(document.querySelector('[data-gcn-tagid]')!, document.getElementById('p')!, 'B', [])).toEqual({ kind: 'refused', reason: 'noEditable' });
    });
});

describe('planLinkEdit and applyPlan', () => {
    it('writes a link tag\'s text and URL through its own parts, the URL without a page', () => {
        const { content, linkTag } = page32();
        const plan = planLinkEdit(linkTag, linkTag, { text: 'Start', target: { type: 'url', url: 'https://example.com' } });

        // A link tag is written as one, also when the editable around it is passed as the part.
        expect(planLinkEdit(content, linkTag, {})).toEqual({ kind: 'linkTag', tagName: 'gtxalohapagelink_6cfd' });
        expect(applyPlan(tags32(), plan)).toEqual({
            tag: {
                id: 891,
                name: 'gtxalohapagelink_6cfd',
                constructId: 4,
                active: true,
                properties: { text: { type: 'STRING', stringValue: 'Start' }, url: { type: 'PAGE', stringValue: 'https://example.com' } },
            },
        });
    });

    it('points a link tag to another CMS page, and saves nothing for the same page', () => {
        const { linkTag } = page32();

        expect(applyPlan(tags32(), planLinkEdit(linkTag, linkTag, { target: { type: 'page', pageId: 9, nodeId: 1 } }))).toEqual({
            tag: { id: 891, name: 'gtxalohapagelink_6cfd', constructId: 4, active: true, properties: { url: { type: 'PAGE', pageId: 9, nodeId: 1 } } },
        });
        expect(applyPlan(tags32(), planLinkEdit(linkTag, linkTag, { text: 'home', target: { type: 'page', pageId: 1 } }))).toEqual({ unchanged: true });
    });

    it('changes a plain link in an editable there, and refuses a CMS page or formatted link text', () => {
        document.body.innerHTML = '<div data-gcn-tagid="888" data-gcn-tagname="pagecontent" id="GENTICS_EDITABLE_3095"><p>Siehe <a id="plain" href="/alt">alt</a> und <a id="bold" href="/b"><b>fett</b></a></p></div>';
        const editable = document.getElementById('GENTICS_EDITABLE_3095')!;

        expect(planLinkEdit(editable, document.getElementById('plain')!, { text: 'neu', target: { type: 'url', url: 'https://example.com/neu' } })).toEqual({
            kind: 'value',
            propertyId: 3095,
            value: '<p>Siehe <a id="plain" href="https://example.com/neu">neu</a> und <a id="bold" href="/b"><b>fett</b></a></p>',
        });
        expect(planLinkEdit(editable, document.getElementById('plain')!, { target: { type: 'page', pageId: 1 } })).toEqual({ kind: 'refused', reason: 'pageTarget' });
        expect(planLinkEdit(editable, document.getElementById('bold')!, { text: 'x' })).toEqual({ kind: 'refused', reason: 'formatting' });
    });
});

describe('planImageEdit and applyPlan', () => {
    it('writes another image into the image tag\'s only image part', () => {
        document.body.innerHTML = '<div data-gcn-tagid="7" data-gcn-tagname="image_1"><img src="/a.jpg" alt=""></div>';
        const part = document.querySelector('[data-gcn-tagid]')!;
        const tags = { image_1: { id: 7, name: 'image_1', constructId: 3, active: true, properties: { src: { id: 70, type: 'IMAGE', imageId: 4411, nodeId: 1 } } } };

        expect(applyPlan(tags, planImageEdit(part, { imageId: 4412, nodeId: 1 }))).toEqual({
            tag: { id: 7, name: 'image_1', constructId: 3, active: true, properties: { src: { type: 'IMAGE', imageId: 4412, nodeId: 1 } } },
        });
        expect(applyPlan(tags, planImageEdit(part, { imageId: 4411, nodeId: 1 }))).toEqual({ unchanged: true });
        expect(applyPlan({ image_1: { ...tags.image_1, properties: {} } }, planImageEdit(part, { imageId: 1, nodeId: 1 }))).toEqual({ refused: 'noImagePart' });
    });
});
