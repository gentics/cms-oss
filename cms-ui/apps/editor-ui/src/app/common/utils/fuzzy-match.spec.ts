import { fuzzyHighlight, fuzzyMatch, fuzzyMatches } from './fuzzy-match';

describe('fuzzyHighlight', () => {

    it('returns the full text for non-matches', () => {
        const actual = fuzzyHighlight('needle', 'haystack');
        const expected = `haystack`;
        expect(actual).toEqual(expected);
    });

    it('wraps matched text in the passed element', () => {
        const actual = fuzzyHighlight('ex', 'test examples', 'i');
        const expected = `test <i>ex</i>amples`;
        expect(actual).toEqual(expected);
    });

    it('uses <em> elements by default', () => {
        const actual = fuzzyHighlight('x', 'a, x, b');
        const expected = `a, <em>x</em>, b`;
        expect(actual).toEqual(expected);
    });

    it('escapes html in matched texts', () => {
        const input = `</body>hax!<script>`;
        const actual = fuzzyHighlight('hax', input);
        const expected = `&lt;/body&gt;<em>hax</em>!&lt;script&gt;`;
        expect(actual).toEqual(expected);
    });

    it('can be passed a class name', () => {
        const actual = fuzzyHighlight('pizza', 'I <3 pizza', 'div', 'highlighted');
        const expected = `I &lt;3 <div class="highlighted">pizza</div>`;
        expect(actual).toEqual(expected);
    });

});

describe('fuzzyMatch', () => {

    it('returns true for exact matches', () => {
        expect(fuzzyMatch('oneword', 'oneword')).toBe(true, 'oneword');
        expect(fuzzyMatch('two words', 'two words')).toBe(true, 'two words');
        expect(fuzzyMatch('two words', 'two interrupted words')).toBe(true, 'two interrupted words');
        expect(fuzzyMatch('beginning', 'beginning of string')).toBe(true, 'beginning');
        expect(fuzzyMatch('end', 'this string has an end')).toBe(true, 'end');
    });

    it('matches multiple words separated by whitespace', () => {
        expect(fuzzyMatch('twowords', 'two words')).toBe(true, 'two words');
        expect(fuzzyMatch('twowords', 'two interrupted words')).toBe(true, 'two interrupted words');
        expect(fuzzyMatch('bdeg', 'a b c d e g')).toBe(true, 'bdeg');
    });

    it('matches multiple words separated by punctuation', () => {
        expect(fuzzyMatch('twowords', 'two-words')).toBe(true, 'two words');
        expect(fuzzyMatch('twowords', 'two,interrupted,words')).toBe(true, 'two interrupted words');
        expect(fuzzyMatch('bdeg', 'a%b"c"d$e-g~')).toBe(true, 'bdeg');
        expect(fuzzyMatch('prode', 'project designs')).toBe(true, 'project designs');
        expect(fuzzyMatch('recar', 'red sports car')).toBe(true, 'red sports car');
    });

    it('does not match substrings in the middle of a word', () => {
        expect(fuzzyMatch('piap', 'pineapple')).toBe(false, 'pineapple');
        expect(fuzzyMatch('nothis', 'shouldnotfindthis')).toBe(false, 'shouldnotfindthis');
        expect(fuzzyMatch('est', 'test')).toBe(false, 'test');
        expect(fuzzyMatch('prod', 'project end')).toBe(false, 'project end');
        expect(fuzzyMatch('blucar', 'blue ocarina')).toBe(false, 'blue ocarina');
    });

    it('returns false for empty patterns', () => {
        expect(fuzzyMatch('', 'test')).toBe(false, 'non-empty string');
        expect(fuzzyMatch('', '')).toBe(false, 'empty string');
    });

});

describe('fuzzyMatches', () => {

    it('returns the full text for non-matches', () => {
        const actual = fuzzyMatches('needle', 'haystack');
        const expected = [{ text: 'haystack', highlight: false }];
        expect(actual).toEqual(expected);
    });

    it('returns the full text for complete matches', () => {
        const actual = fuzzyMatches('needle', 'needle');
        const expected = [{ text: 'needle', highlight: true}];
        expect(actual).toEqual(expected);
    });

    it('returns the full text with highlight=false for an empty pattern', () => {
        const actual = fuzzyMatches('', 'haystack');
        const expected = [{ text: 'haystack', highlight: false}];
        expect(actual).toEqual(expected);
    });

    it('works with an empty text', () => {
        const actual = fuzzyMatches('needle', '');
        const expected = [{ text: '', highlight: false}];
        expect(actual).toEqual(expected);
    });

    it('works with an empty text and empty pattern', () => {
        const actual = fuzzyMatches('', '');
        const expected = [{ text: '', highlight: false}];
        expect(actual).toEqual(expected);
    });

    it('includes the text before a match as first element', () => {
        const actual = fuzzyMatches('x', 'before-x');
        const expected = [
            { text: 'before-', highlight: false },
            { text: 'x', highlight: true }
        ];
        expect(actual).toEqual(expected);
    });

    it('includes the text after a match as last element', () => {
        const actual = fuzzyMatches('x', 'x-after');
        const expected = [
            { text: 'x', highlight: true },
            { text: '-after', highlight: false }
        ];
        expect(actual).toEqual(expected);
    });

    it('includes both surrounding "before" and "after" text', () => {
        const actual = fuzzyMatches('x', 'before-x-after');
        const expected = [
            { text: 'before-', highlight: false },
            { text: 'x', highlight: true },
            { text: '-after', highlight: false }
        ];
        expect(actual).toEqual(expected);
    });

    it('includes text between two matches', () => {
        const actual = fuzzyMatches('xy', 'x-between-y');
        const expected = [
            { text: 'x', highlight: true },
            { text: '-between-', highlight: false },
            { text: 'y', highlight: true }
        ];
        expect(actual).toEqual(expected);
    });

    it('combines multiple matched characters', () => {
        const actual = fuzzyMatches('abcdef', 'abc-between-def');
        const expected = [
            { text: 'abc', highlight: true },
            { text: '-between-', highlight: false },
            { text: 'def', highlight: true }
        ];
        expect(actual).toEqual(expected);
    });

    it('returns all matches and surrounding text', () => {
        const actual = fuzzyMatches('webproj2016', 'Archived Website Projects 2016 / 2017');
        const expected = [
            { text: 'Archived ', highlight: false },
            { text: 'Web', highlight: true },
            { text: 'site ', highlight: false },
            { text: 'Proj', highlight: true },
            { text: 'ects ', highlight: false },
            { text: '2016', highlight: true },
            { text: ' / 2017', highlight: false }
        ];
        expect(actual).toEqual(expected);
    });

});
