import {sanitizeElasticQuery} from './sanitize-elastic-query';

describe('sanitizeElasticQuery()', () => {

    it('strips invalid ~ char', () => {
        expect(sanitizeElasticQuery('~')).toBe('');
        expect(sanitizeElasticQuery('~~')).toBe('');
        expect(sanitizeElasticQuery('~foo')).toBe('foo');
        expect(sanitizeElasticQuery('~~foo')).toBe('foo');
        expect(sanitizeElasticQuery('~ foo')).toBe(' foo');
        expect(sanitizeElasticQuery('~ foo ~')).toBe(' foo ~');
        expect(sanitizeElasticQuery('~ foo ~bar')).toBe(' foo bar');
        expect(sanitizeElasticQuery('name:~foo')).toBe('name:foo');
        expect(sanitizeElasticQuery('name:~ foo')).toBe('name: foo');
    });

    it('strips invalid + char', () => {
        expect(sanitizeElasticQuery('+')).toBe('');
        expect(sanitizeElasticQuery('    +')).toBe('    ');
        expect(sanitizeElasticQuery('/\d+/')).toBe('/d+/');
    });

    it('strips invalid ^ char', () => {
        expect(sanitizeElasticQuery('^')).toBe('');
        expect(sanitizeElasticQuery('    ^')).toBe('    ');
        expect(sanitizeElasticQuery('/^foo$/')).toBe('/^foo$/');
    });

    it('strips invalid : char', () => {
        expect(sanitizeElasticQuery(':')).toBe('');
        expect(sanitizeElasticQuery('    :')).toBe('    ');
        expect(sanitizeElasticQuery(' : ')).toBe('  ');
        expect(sanitizeElasticQuery(':foo')).toBe('foo');
        expect(sanitizeElasticQuery(' : foo')).toBe('  foo');
        expect(sanitizeElasticQuery('/abc:d/')).toBe('/abc:d/');
        expect(sanitizeElasticQuery('name: foo')).toBe('name: foo');
    });

    it('allows integer id searches', () => {
        expect(sanitizeElasticQuery('id:2')).toBe('id:2');
        expect(sanitizeElasticQuery('id: 4')).toBe('id: 4');
        expect(sanitizeElasticQuery('id: 4 id:32')).toBe('id: 4 id:32');
        expect(sanitizeElasticQuery('id:2 name:bar*')).toBe('id:2 name:bar*');
        expect(sanitizeElasticQuery('id: 665 +name:bar*')).toBe('id: 665 +name:bar*');
        expect(sanitizeElasticQuery('+name:bar* id:44')).toBe('+name:bar* id:44');
        expect(sanitizeElasticQuery('id:  4 +name:bar* id:44')).toBe('id:  4 +name:bar* id:44');
    });

    it('removes non-numeric id searches', () => {
        expect(sanitizeElasticQuery('id:foo')).toBe('');
        expect(sanitizeElasticQuery('id: foo')).toBe('');
        expect(sanitizeElasticQuery('id:   foo')).toBe('');
        expect(sanitizeElasticQuery('id: foo bar*')).toBe('bar*');
        expect(sanitizeElasticQuery('id: foo +name:bar*')).toBe('+name:bar*');
        expect(sanitizeElasticQuery('id: 2 id: foo +name:bar*')).toBe('id: 2 +name:bar*');
    });

    it('escapes non-regex / char', () => {
        expect(sanitizeElasticQuery('/')).toBe('\\/');
        expect(sanitizeElasticQuery(' / ')).toBe(' \\/ ');
        expect(sanitizeElasticQuery('a/b')).toBe('a\\/b');
        expect(sanitizeElasticQuery('(a/b) name:/a/')).toBe('(a\\/b) name:/a/');
    });

    it('escapes non-regex square brackets', () => {
        expect(sanitizeElasticQuery('[')).toBe('\\[');
        expect(sanitizeElasticQuery('[]')).toBe('\\[\\]');
        expect(sanitizeElasticQuery(' [foo] ')).toBe(' \\[foo\\] ');
        expect(sanitizeElasticQuery('a /[a-z]+/ name:[a]')).toBe('a /[a-z]+/ name:\\[a\\]');
    });

    it('escapes non-regex curly braces', () => {
        expect(sanitizeElasticQuery('{')).toBe('\\{');
        expect(sanitizeElasticQuery('{}')).toBe('\\{\\}');
        expect(sanitizeElasticQuery(' {foo} ')).toBe(' \\{foo\\} ');
        expect(sanitizeElasticQuery('a /[234]{4}+/ name:{a}')).toBe('a /[234]{4}+/ name:\\{a\\}');
    });

    it('does not escape valid regex', () => {
        expect(sanitizeElasticQuery('/a/ bar /b/')).toBe('/a/ bar /b/');
        expect(sanitizeElasticQuery('a/b /awd')).toBe('a/b /awd');
        expect(sanitizeElasticQuery('name:/abc\d/')).toBe('name:/abcd/');
        expect(sanitizeElasticQuery('(foo/bar) +/[a-zA-Z]+/')).toBe('(foo\\/bar) +/[a-zA-Z]+/');
    });

    it('escapes unmatched parentheses', () => {
        expect(sanitizeElasticQuery('()')).toBe('\\(\\)');
        expect(sanitizeElasticQuery('(awd)')).toBe('(awd)');
        expect(sanitizeElasticQuery('(awd')).toBe('\\(awd');
        expect(sanitizeElasticQuery('awd)')).toBe('awd\\)');
        expect(sanitizeElasticQuery('awd rrr )')).toBe('awd rrr \\)');
        expect(sanitizeElasticQuery('(awd +add) rrr )')).toBe('(awd +add) rrr \\)');
        expect(sanitizeElasticQuery('(awd +add (rrr )')).toBe('\\(awd +add (rrr )');
        expect(sanitizeElasticQuery('(abc def)')).toBe('(abc def)');
        expect(sanitizeElasticQuery('(abc /abc\\(/')).toBe('\\(abc /abc\\(/');
    });

});
