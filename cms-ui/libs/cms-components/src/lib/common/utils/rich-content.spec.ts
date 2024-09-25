import { RichContentLink, RichContentLinkType, RichContentType } from '../models';
import { extractRichContent, toRichContentTemplate } from './rich-content';

const LINK_ONE_PARSED: RichContentLink = {
    type: RichContentType.LINK,
    linkType: RichContentLinkType.PAGE,
    nodeId: 1,
    itemId: 42,
    displayText: 'Link Text hello world!',
    target: '_top',
    url: null,
};
const LINK_ONE_TEXT = `{{LINK|${LINK_ONE_PARSED.linkType}:${LINK_ONE_PARSED.nodeId}:${LINK_ONE_PARSED.itemId}|${LINK_ONE_PARSED.displayText}|${LINK_ONE_PARSED.target}}}`;

const LINK_TWO_PARSED: RichContentLink = {
    type: RichContentType.LINK,
    linkType: RichContentLinkType.PAGE,
    nodeId: '0416.f3be4afb-6109-11ed-b808-024281b40002',
    itemId: 'AF54.b0ae171a-8241-11ed-9a49-0242ac18ff05',
    displayText: 'Grüße in die Welt!',
    target: '_blank',
    url: null,
};
const LINK_TWO_TEXT = `{{LINK|${LINK_TWO_PARSED.linkType}:${LINK_TWO_PARSED.nodeId}:${LINK_TWO_PARSED.itemId}|${LINK_TWO_PARSED.displayText}|${LINK_TWO_PARSED.target}}}`;

const LINK_THREE_PARSED: RichContentLink = {
    type: RichContentType.LINK,
    linkType: RichContentLinkType.PAGE,
    nodeId: 9,
    itemId: 343,
    displayText: 'minimal',
    target: null,
    url: null,
};
const LINK_THREE_TEXT = `{{LINK|${LINK_THREE_PARSED.linkType}:${LINK_THREE_PARSED.nodeId}:${LINK_THREE_PARSED.itemId}|${LINK_THREE_PARSED.displayText}}}`;

const LINK_FOUR_PARSED: RichContentLink = {
    type: RichContentType.LINK,
    linkType: RichContentLinkType.FILE,
    nodeId: 1,
    itemId: 1,
    displayText: 'Command & Conquer is a {RTS} Game. Maths shouldn\'t be used, neither | special chars',
    target: null,
    url: null,
};
const LINK_FOUR_TEXT = '{{LINK|FILE:1:1|Command &amp; Conquer is a &lcub;RTS&rcub; Game. Maths shouldn&apos;t be used, neither &vert; special chars}}';

const LINK_FIVE_PARSED: RichContentLink = {
    type: RichContentType.LINK,
    linkType: RichContentLinkType.URL,
    url: 'https://somewhere.com/example-page.html',
    displayText: 'Foo bar',
    target: '_top',
    itemId: null,
    nodeId: null,
};
const LINK_FIVE_TEXT = '{{LINK|URL:https&col;//somewhere.com/example-page.html|Foo bar|_top}}';

describe('toRichContentTemplate', () => {

    it('should convert full links to correct strings', () => {
        expect(toRichContentTemplate(LINK_ONE_PARSED)).toEqual(LINK_ONE_TEXT);
        expect(toRichContentTemplate(LINK_TWO_PARSED)).toEqual(LINK_TWO_TEXT);
    });

    it('should not include optional values to the template', () => {
        expect(toRichContentTemplate(LINK_THREE_PARSED)).toEqual(LINK_THREE_TEXT);
    });

    it('should escape the display text correctly', () => {
        expect(toRichContentTemplate(LINK_FOUR_PARSED)).toEqual(LINK_FOUR_TEXT);
    });

    it('should escape the url correctly', () => {
        expect(toRichContentTemplate(LINK_FIVE_PARSED)).toEqual(LINK_FIVE_TEXT);
    });
});

describe('extractRichContent', () => {

    it('should extract links in the middle correctly', () => {
        expect(extractRichContent(`Foo bar before ${LINK_ONE_TEXT}text directly after`))
            .toEqual([
                'Foo bar before ',
                LINK_ONE_PARSED,
                'text directly after',
            ]);
    });

    it('should extract links in the beginning correctly', () => {
        expect(extractRichContent(`${LINK_ONE_TEXT}text directly after`))
            .toEqual([
                LINK_ONE_PARSED,
                'text directly after',
            ]);
    });

    it('should extract links at the end correctly', () => {
        expect(extractRichContent(`Foo bar before ${LINK_ONE_TEXT}`))
            .toEqual([
                'Foo bar before ',
                LINK_ONE_PARSED,
            ]);
    });

    it('should extract multiple links correctly', () => {
        expect(extractRichContent(`HELLO ${LINK_ONE_TEXT} WORLD ${LINK_TWO_TEXT} FOO BAR`))
            .toEqual([
                'HELLO ',
                LINK_ONE_PARSED,
                ' WORLD ',
                LINK_TWO_PARSED,
                ' FOO BAR',
            ]);
    });

    it('should have `null` as value for optional values', () => {
        expect(extractRichContent(LINK_THREE_TEXT)).toEqual([LINK_THREE_PARSED]);
    });

    it('should decode the escaped display text correctly', () => {
        expect(extractRichContent(LINK_FOUR_TEXT)).toEqual([LINK_FOUR_PARSED]);
    });

    it('should decode the url correctly', () => {
        expect(extractRichContent(LINK_FIVE_TEXT)).toEqual([LINK_FIVE_PARSED]);
    });
});
