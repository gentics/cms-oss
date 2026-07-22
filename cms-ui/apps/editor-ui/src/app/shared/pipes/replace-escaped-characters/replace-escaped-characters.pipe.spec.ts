import { ReplaceEscapedCharactersPipe } from './replace-escaped-characters.pipe';

describe('ReplaceEscapedCharactersPipe', () => {
    let pipe: ReplaceEscapedCharactersPipe;
    let input: string;

    beforeEach(() => {
        pipe = new ReplaceEscapedCharactersPipe();
    });

    it('returns right result if &nbsp; appears in string', () => {
        input = 'test&nbsp;';
        expect(pipe.transform(input)).toEqual('test ');
    });

    it('returns right result if &lt; appears in string', () => {
        input = 'test&lt;';
        expect(pipe.transform(input)).toEqual('test<');
    });

    it('returns right result if &gt; appears in string', () => {
        input = 'test&gt;';
        expect(pipe.transform(input)).toEqual('test>');
    });

    it('returns right result if &amp; appears in string', () => {
        input = 'test&amp;';
        expect(pipe.transform(input)).toEqual('test&');
    });

    it('returns right result if &quot; appears in string', () => {
        input = 'test&quot;';
        expect(pipe.transform(input)).toEqual('test"');
    });
});
