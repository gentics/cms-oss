import {isSameOrigin} from './is-same-origin';

/**
 * Test cases based on examples from https://en.wikipedia.org/wiki/Same-origin_policy#Origin_determination_rules
 */
describe('isSameOrigin', () => {

    it('should return true for same origins', () => {
        const urlA = 'http://www.example.com/dir/page.html';

        expect(isSameOrigin(urlA, 'http://www.example.com/dir/page2.html')).toBe(true);
        expect(isSameOrigin(urlA, 'http://www.example.com/dir2/other.html')).toBe(true);
        expect(isSameOrigin(urlA, 'http://username:password@www.example.com/dir2/other.html')).toBe(true);
    });


    it('should return false for different origins', () => {
        const urlA = 'http://www.example.com/dir/page.html';

        expect(isSameOrigin(urlA, 'http://www.example.com:81/dir/other.html')).toBe(false);
        expect(isSameOrigin(urlA, 'https://www.example.com/dir/other.html')).toBe(false);
        expect(isSameOrigin(urlA, 'http://en.example.com/dir/other.html')).toBe(false);
        expect(isSameOrigin(urlA, 'http://example.com/dir/other.html')).toBe(false);
        expect(isSameOrigin(urlA, 'http://v2.www.example.com/dir/other.html')).toBe(false);
    });

    it('should handle urls without the protocol', () => {
        expect(isSameOrigin('http://www.example.com/foo.html', 'www.example.com/bar.html')).toBe(true);
        expect(isSameOrigin('http://example.com/foo.html', 'example.com/bar.html')).toBe(true);
        expect(isSameOrigin('http://example.com/foo.html', 'www.anotherone.com/bar.html')).toBe(false);
        expect(isSameOrigin('http://example.com/foo.html', 'anotherone.com/bar.html')).toBe(false);
    });

});
