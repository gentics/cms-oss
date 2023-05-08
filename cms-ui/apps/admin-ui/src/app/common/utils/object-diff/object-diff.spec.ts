import { cloneDeep } from 'lodash';
import { objectDiff } from './object-diff';

const TEST_OBJ = {
    a: 1,
    b: 2,
    nestedRoot: {
        nestedChild: {
            c: 'test',
            d: [1, 2, 3, 4],
        },
    },
    e: [{ f: 1, g: 2 }, { f: 3, g: 4 }],
};

describe('objectDiff()', () => {

    function cloneTestObj(): typeof TEST_OBJ {
        return cloneDeep(TEST_OBJ);
    }

    function runTest<T, U>(version2: T, version1: U, expectedResult: Partial<T>): void {
        const result = objectDiff(version2, version1);
        expect(result).toEqual(expectedResult);
    }

    it('returns the difference between two object versions', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        version2.b = 3;

        const expected = {
            b: 3,
        };
        runTest(version2, version1, expected);
    });

    it('works for equal objects', () => {
        const expected = {};
        runTest(cloneTestObj(), cloneTestObj(), expected);
    });

    it('works if the difference is inside a nested object', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        version2.nestedRoot.nestedChild.c = 'change';

        const expected = {
            nestedRoot: cloneDeep(version2.nestedRoot),
        };
        runTest(version2, version1, expected);
    });

    it('works if a nested array has a different length', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        version2.nestedRoot.nestedChild.d = version2.nestedRoot.nestedChild.d.slice(1);

        const expected = {
            nestedRoot: cloneDeep(version2.nestedRoot),
        };
        runTest(version2, version1, expected);
    });

    it('works if the difference is inside an object that is part of an array', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        version2.e[1].f = 10;

        const expected = {
            e: cloneDeep(version2.e),
        };
        runTest(version2, version1, expected);
    });

    it('returns properties that are present in version2, but not in version1', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        delete version1.a;

        const expected = {
            a: version2.a,
        };
        runTest(version2, version1, expected);
    });

    it('does not return properties that are present in version1, but not in version2', () => {
        const version1 = cloneTestObj();
        const version2 = cloneTestObj();
        delete version2.a;

        const expected = {};
        runTest(version2, version1, expected);
    });

});
