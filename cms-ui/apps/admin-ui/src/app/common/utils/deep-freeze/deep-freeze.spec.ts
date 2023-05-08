import { cloneDeep as _cloneDeep } from 'lodash';
import { deepFreeze } from './deep-freeze';

const TEST_OBJ = {
    numericProp: 1,
    stringProp: 'test',
    deeplyNestedObj: {
        a: 1,
        b: {
            c: 'test',
            simpleArray: [ 1, 2, 3 ],
            complexArray: [
                { x: 1, y: 2 },
                { x: 3, y: 4 },
            ],
        },
    },
};

describe('deepFreeze()', () => {

    function assertIsDeeplyFrozen(orig: typeof TEST_OBJ, frozen: typeof TEST_OBJ): void {
        expect(frozen).toBe(orig);
        expect(Object.isFrozen(frozen)).toBe(true, 'root object was not frozen');
        expect(Object.isFrozen(frozen.deeplyNestedObj)).toBe(true, 'obj.deeplyNestedObj was not frozen');
        expect(Object.isFrozen(frozen.deeplyNestedObj.b)).toBe(true, 'obj.deeplyNestedObj.b was not frozen');
        expect(Object.isFrozen(frozen.deeplyNestedObj.b.simpleArray)).toBe(true, 'obj.deeplyNestedObj.b.simpleArray was not frozen');
        expect(Object.isFrozen(frozen.deeplyNestedObj.b.complexArray)).toBe(true, 'obj.deeplyNestedObj.b.complexArray was not frozen');
        frozen.deeplyNestedObj.b.complexArray.forEach(
            (item, index) => expect(Object.isFrozen(item)).toBe(true, `obj.deeplyNestedObj.b.complexArray[${index}] was not frozen`),
        );
    }

    it('recursively freezes a complex object', () => {
        const orig = _cloneDeep(TEST_OBJ);
        const frozen = deepFreeze(orig);
        assertIsDeeplyFrozen(orig, frozen);
    });

    it('recursively freezes a complex object that is already partially frozen', () => {
        const orig = _cloneDeep(TEST_OBJ);
        Object.freeze(orig.deeplyNestedObj);
        const frozen = deepFreeze(orig);
        assertIsDeeplyFrozen(orig, frozen);
    });

});
