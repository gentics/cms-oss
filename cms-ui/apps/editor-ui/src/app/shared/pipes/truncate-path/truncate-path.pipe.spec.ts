import {TruncatePathPipe} from './truncate-path.pipe';

describe('TruncatePathPipe:', () => {

    const longPath = 'my-project/some/overly/long/path/to/just-some/file.ext';

    let truncatePathPipe: TruncatePathPipe;
    beforeEach(() => truncatePathPipe = new TruncatePathPipe());

    it('should short path intact', () => {
        expect(truncatePathPipe.transform(longPath, longPath.length)).toBe(longPath);
    });

    it('should truncate, maxLength = 50', () => {
        expect(truncatePathPipe.transform(longPath, 50)).toBe('my-project/.../long/path/to/just-some/file.ext');
    });

    it('should truncate, maxLength = 40', () => {
        expect(truncatePathPipe.transform(longPath, 40)).toBe('my-project/.../to/just-some/file.ext');
    });

    it('should truncate, maxLength = 30', () => {
        expect(truncatePathPipe.transform(longPath, 30)).toBe('my-project/.../file.ext');
    });

    it('should truncate, maxLength = 20', () => {
        expect(truncatePathPipe.transform(longPath, 20)).toBe('my-proje.../file.ext');
    });

    it('should truncate, maxLength = 10', () => {
        expect(truncatePathPipe.transform(longPath, 10)).toBe('...ile.ext');
    });

    it('should handle a trailing delimiter with no truncation', () => {
        let folderPath = 'Corporate Blog/Blogposts/';
        expect(truncatePathPipe.transform(folderPath, 50)).toBe('Corporate Blog/Blogposts/');
    });

    it('should handle a trailing delimiter with truncation', () => {
        let folderPath = 'Corporate Blog/Blogposts/';
        expect(truncatePathPipe.transform(folderPath, 20)).toBe('Corpor.../Blogposts/');
    });

    it('should handle a trailing delimiter and whitespace with truncation', () => {
        let folderPath = 'Corporate Blog/Blogposts/   ';
        expect(truncatePathPipe.transform(folderPath, 20)).toBe('Corpor.../Blogposts/');
    });

    /**
     * This particular value caused an infinite loop and browser crash in production due to the single long
     * path segment.
     */
    it('should not go into an infinite loop with long single segment', () => {
        let folderPath = 'FG UBIT Wien: Systemische Unternehmensberatung und Projektmanagement/';
        expect(truncatePathPipe.transform(folderPath, 50)).toBe('...che Unternehmensberatung und Projektmanagement/');
    });



    describe('unexpected input:', () => {

        it('should handle undefined', () => {
            expect(truncatePathPipe.transform((<any> undefined))).toBeUndefined();
        });

        it('should handle null', () => {
            expect(truncatePathPipe.transform((<any> null))).toBeNull();
        });

        it('should handle a number', () => {
            expect(truncatePathPipe.transform((<any> 42))).toBe(42);
        });

        it('should handle an object', () => {
            expect(truncatePathPipe.transform((<any> {}))).toEqual({});
        });

        it('should handle a function', () => {
            let fn = () => {};
            expect(truncatePathPipe.transform((<any> fn))).toBe(fn);
        });

        it('should handle an array', () => {
            let arr: any[] = [];
            expect(truncatePathPipe.transform((<any> arr))).toBe(arr);
        });

    });
});
