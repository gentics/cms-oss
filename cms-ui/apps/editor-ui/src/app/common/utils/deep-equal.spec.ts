import {deepEqual} from './deep-equal';

describe('deepEqual', () => {

    it('compares primitive values', () => {
        expect(deepEqual(1, 1)).toBe(true);
        expect(deepEqual(1, 2)).toBe(false);

        expect(deepEqual('foo', 'foo')).toBe(true);
        expect(deepEqual('foo', 'bar')).toBe(false);

        expect(deepEqual(true, true)).toBe(true);
        expect(deepEqual(true, false)).toBe(false);

    });

    it('compares objects by-value', () => {
        const a = { color: 'red', fruits: { apple: true, cherry: true } };
        const b = { color: 'red', fruits: { apple: true, cherry: true } };
        expect(deepEqual(a, a)).toBe(true);
        expect(deepEqual(a, b)).toBe(true);
        expect(deepEqual(b, b)).toBe(true);
    });

    it('does not check key order of objects', () => {
        const a = { color: 'red', fruits: { apple: true, cherry: true } };
        const b = { fruits: { cherry: true, apple: true }, color: 'red' };
        expect(deepEqual(a, a)).toBe(true);
        expect(deepEqual(a, b)).toBe(true);
        expect(deepEqual(b, b)).toBe(true);
    });

    it('detects shallow differences of objects', () => {
        const a = { name: 'Alice' };
        const b = { name: 'Bob' };
        expect(deepEqual(a, b)).toBe(false);

        const c: any = { prop: null };
        const d: any = { prop: undefined };
        expect(deepEqual(c, d)).toBe(false);

        const e = { shouldBeANumber: 15 };
        const f = { shouldBeANumber: '15' };
        expect(deepEqual(e, f as any)).toBe(false);
    });

    it('detects deep differences of objects', () => {
        const a = {
            some: {
                deep: {
                    property: {
                        equal: true
                    }
                }
            }
        };
        const b = {
            some: {
                deep: {
                    property: {
                        equal: false
                    }
                }
            }
        };
        expect(deepEqual(a, b)).toBe(false);
    });

    it('detects shallow differences in arrays', () => {
        const a = [1, 2, 3];
        const b = [1, 2, 3];
        expect(deepEqual(a, b)).toBe(true);

        const c = [1, 2, 5];
        const d = [1, 2, 9];
        expect(deepEqual(c, d)).toBe(false);

        const e = [1, 2, 3];
        const f = [1, 3, 2];
        expect(deepEqual(e, f)).toBe(false);
    });

    it('detects deep differences in arrays', () => {
        const a = [{ id: 111 }, { id: 222 }];
        const b = [{ id: 111 }, { id: 222 }];
        expect(deepEqual(a, b)).toBe(true);

        const c = [[0, 1, 2], [3, 4, 5], [6, 7, 8]];
        const d = [[0, 1, 2], [5, 4, 3], [6, 7, 8]];
        expect(deepEqual(c, d)).toBe(false);
    });

    it('detects differences in nested arrays', () => {
        const a = [[[[1, 2]]]];
        const b = [[[[1, 2]]]];
        expect(deepEqual(a, b)).toBe(true);

        const c = [[[[1, 2]]]];
        const d = [[[[2, 1]]]];
        expect(deepEqual(c, d)).toBe(false);
    });

    it('checks for missing keys on both inputs', () => {
        const a = { name: 'Alice' };
        const b = { name: 'Bob', anotherProp: true };
        expect(deepEqual(a, b)).toBe(false);

        const c = [{ name: 'Eve' }, { name: 'John' }];
        const d = [{ name: 'Eve' }, { name: 'John' }, { name: 'Whoops!' }];
        expect(deepEqual(c, d)).toBe(false);
    });

    it('does not throw on null or undefined inputs', () => {
        expect(deepEqual(null, null)).toBe(true);
        expect(deepEqual(undefined, undefined)).toBe(true);

        expect(deepEqual(null, undefined)).toBe(false);
        expect(deepEqual(undefined, null)).toBe(false);

        const a = { someProps: true };

        expect(deepEqual(null, a)).toBe(false);
        expect(deepEqual(a, null)).toBe(false);

        expect(deepEqual(undefined, a)).toBe(false);
        expect(deepEqual(a, undefined)).toBe(false);
    });

    it('does not differentiate between object or array', () => {
        const a = [1, 'apple', true];
        const b = { 0: 1, 1: 'apple', 2: true } as any;
        expect(deepEqual(a, b)).toBe(true);

        const c = [1, 'not an apple', true];
        const d = { 0: 1, 1: 'apple', 2: true } as any;
        expect(deepEqual(c, d)).toBe(false);
    });

    it('does not differentiate between object prototypes / "classes"', () => {
        class Person { name: string; age: number; }
        const a = { name: 'John', age: 35 };
        const b = new Person(); b.name = 'John'; b.age = 35;
        expect(deepEqual(a, b)).toBe(true);

        b.age = 99;
        expect(deepEqual(a, b)).toBe(false);
    });

    it('does not compare unenumerable properties', () => {
        const a = { color: 'green' };
        const b = { color: 'green' };

        Object.defineProperty(a, 'brightness', { value: 'light', enumerable: false });
        Object.defineProperty(b, 'brightness', { value: 'dark', enumerable: false });

        expect(deepEqual(a, b)).toBe(true);
    });

    it('covers some edge-cases', () => {
        const a = [1, 2, 3, 4];
        const b = [1, 2, 3, 4];
        (b as any).nonArrayProp = true;
        expect(deepEqual(a, b)).toBe(false);
    });

    it('does not cover all edge-cases', () => {
        const a = { weirdProp: undefined } as any;
        const b = { differentProp: undefined } as any;
        expect(deepEqual(a, b)).toBe(true);

        const c = { type: 'file', pattern: /\.jpg$/ };
        const d = { type: 'file', pattern: /\.txt$/ };
        expect(deepEqual(c, d)).toBe(true);

        const e = { when: new Date(1234567890) };
        const f = { when: new Date(9876543210) };
        expect(deepEqual(e, f)).toBe(true);
    });
});
