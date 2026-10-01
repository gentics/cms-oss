import { describe, expect, it } from 'vitest';

import { applyMergePatch } from './mergePatch';

describe('applyMergePatch', () => {
    // The test cases of RFC 7386, appendix A.
    it.each([
        [{ a: 'b' }, { a: 'c' }, { a: 'c' }],
        [{ a: 'b' }, { b: 'c' }, { a: 'b', b: 'c' }],
        [{ a: 'b' }, { a: null }, {}],
        [{ a: 'b', b: 'c' }, { a: null }, { b: 'c' }],
        [{ a: ['b'] }, { a: 'c' }, { a: 'c' }],
        [{ a: 'c' }, { a: ['b'] }, { a: ['b'] }],
        [{ a: { b: 'c' } }, { a: { b: 'd', c: null } }, { a: { b: 'd' } }],
        [{ a: [{ b: 'c' }] }, { a: [1] }, { a: [1] }],
        [['a', 'b'], ['c', 'd'], ['c', 'd']],
        [{ a: 'b' }, ['c'], ['c']],
        [{ a: 'foo' }, null, null],
        [{ a: 'foo' }, 'bar', 'bar'],
        [{ e: null }, { a: 1 }, { e: null, a: 1 }],
        [[1, 2], { a: 'b', c: null }, { a: 'b' }],
        [{}, { a: { bb: { ccc: null } } }, { a: { bb: {} } }],
    ])('patches %j with %j to %j', (target, patch, expected) => {
        expect(applyMergePatch(target, patch)).toEqual(expected);
    });

    it('leaves the target unchanged', () => {
        const target = { a: { b: 'c' } };

        applyMergePatch(target, { a: { b: 'd' } });

        expect(target).toEqual({ a: { b: 'c' } });
    });
});
