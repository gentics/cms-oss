import { ExtractedLink } from '../models';
import { extractLinks, toLinkTemplate } from './link-extraction';

const LINK_ONE_PARSED: ExtractedLink = {
    type: 'PAGE',
    nodeId: 1,
    pageId: 42,
    langCode: 'en',
    displayText: 'Link Text hello world!',
    target: '_top',
};
const LINK_ONE_TEXT = `{{LINK|${LINK_ONE_PARSED.type}:${LINK_ONE_PARSED.nodeId}:${LINK_ONE_PARSED.pageId}:${LINK_ONE_PARSED.langCode}|${LINK_ONE_PARSED.displayText}|${LINK_ONE_PARSED.target}}}`;
const LINK_TWO_PARSED: ExtractedLink = {
    type: 'PAGE',
    nodeId: '0416.f3be4afb-6109-11ed-b808-024281b40002',
    pageId: 'AF54.b0ae171a-8241-11ed-9a49-0242ac18ff05',
    langCode: 'de',
    displayText: 'Grüße in die Welt!',
    target: '_blank',
};
const LINK_TWO_TEXT = `{{LINK|${LINK_TWO_PARSED.type}:${LINK_TWO_PARSED.nodeId}:${LINK_TWO_PARSED.pageId}:${LINK_TWO_PARSED.langCode}|${LINK_TWO_PARSED.displayText}|${LINK_TWO_PARSED.target}}}`;
const LINK_THREE_PARSED: ExtractedLink = {
    type: 'PAGE',
    nodeId: 9,
    pageId: 343,
    displayText: 'minimal',
    langCode: null,
    target: null,
};
const LINK_THREE_TEXT = `{{LINK|${LINK_THREE_PARSED.type}:${LINK_THREE_PARSED.nodeId}:${LINK_THREE_PARSED.pageId}|${LINK_THREE_PARSED.displayText}}}`;
const LINK_FOUR_PARSED: ExtractedLink = {
    type: 'PAGE',
    nodeId: 1,
    pageId: 1,
    langCode: null,
    displayText: 'Command & Conquer is a {RTS} Game. Maths shouldn\'t be used, neither | special chars',
    target: null,
};
const LINK_FOUR_TEXT = '{{LINK|PAGE:1:1|Command &amp; Conquer is a &lcub;RTS&rcub; Game. Maths shouldn&apos;t be used, neither &vert; special chars}}';

describe('toLinkTemplate', () => {

    it('should convert full links to correct strings', () => {
        expect(toLinkTemplate(LINK_ONE_PARSED)).toEqual(LINK_ONE_TEXT);
        expect(toLinkTemplate(LINK_TWO_PARSED)).toEqual(LINK_TWO_TEXT);
    });

    it('should not include optional values to the template', () => {
        expect(toLinkTemplate(LINK_THREE_PARSED)).toEqual(LINK_THREE_TEXT);
    });

    it('should escape the display text correctly', () => {
        expect(toLinkTemplate(LINK_FOUR_PARSED)).toEqual(LINK_FOUR_TEXT);
    });
});

describe('extractLinks', () => {

    it('should extract links in the middle correctly', () => {
        expect(extractLinks(`Foo bar before ${LINK_ONE_TEXT}text directly after`))
            .toEqual([
                'Foo bar before ',
                LINK_ONE_PARSED,
                'text directly after',
            ]);
    });

    it('should extract links in the beginning correctly', () => {
        expect(extractLinks(`${LINK_ONE_TEXT}text directly after`))
            .toEqual([
                LINK_ONE_PARSED,
                'text directly after',
            ]);
    });

    it('should extract links at the end correctly', () => {
        expect(extractLinks(`Foo bar before ${LINK_ONE_TEXT}`))
            .toEqual([
                'Foo bar before ',
                LINK_ONE_PARSED,
            ]);
    });

    it('should extract multiple links correctly', () => {
        expect(extractLinks(`HELLO ${LINK_ONE_TEXT} WORLD ${LINK_TWO_TEXT} FOO BAR`))
            .toEqual([
                'HELLO ',
                LINK_ONE_PARSED,
                ' WORLD ',
                LINK_TWO_PARSED,
                ' FOO BAR',
            ]);
    });

    it('should have `null` as value for optional values', () => {
        expect(extractLinks(LINK_THREE_TEXT)).toEqual([LINK_THREE_PARSED]);
    });

    it('should decode the escaped display text correctly', () => {
        expect(extractLinks(LINK_FOUR_TEXT)).toEqual([LINK_FOUR_PARSED]);
    });
});
