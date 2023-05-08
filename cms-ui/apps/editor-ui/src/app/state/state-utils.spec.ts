import {schema} from 'normalizr';
import {concatUnique, removeEntries, normalizeEntities} from './state-utils';

describe('state-utils', () => {

    describe('concatUnique():', () => {

        describe('for Array<number>:', () => {

            it('concatenates arrays', () => {
                let a = [1, 2, 3];
                let b = [4, 5, 6];
                expect(concatUnique(a, b)).toEqual([1, 2, 3, 4, 5, 6]);
            });

            it('keeps sorting', () => {
                let a = [1, 9, 7, 4, 8];
                let b = [6, 2, 5];
                expect(concatUnique(a, b)).toEqual([1, 9, 7, 4, 8, 6, 2, 5]);
            });

            it('omits duplicates of the left argument', () => {
                let a = [1, 2, 2, 1, 2, 5];
                let b = [6];
                expect(concatUnique(a, b)).toEqual([1, 2, 5, 6]);
            });

            it('omits duplicates of the right argument', () => {
                let a = [1, 2, 3];
                let b = [5, 5, 6, 6];
                expect(concatUnique(a, b)).toEqual([1, 2, 3, 5, 6]);
            });

            it('omits elements that exist in both arrays', () => {
                let a = [1, 2, 3, 4];
                let b = [3, 5, 4, 2, 6];
                expect(concatUnique(a, b)).toEqual([1, 2, 3, 4, 5, 6]);
            });

            it('keeps sorting and omits duplicates', () => {
                let a = [1, 9, 7, 4, 8];
                let b = [7, 9, 6, 2, 5];
                expect(concatUnique(a, b)).toEqual([1, 9, 7, 4, 8, 6, 2, 5]);
            });

            it('returns the original array when possible', () => {
                let a = [1, 2, 3, 4, 5];
                let b = [1, 2];
                expect(concatUnique(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<string>:', () => {

            it('concatenates arrays', () => {
                let a = ['apple', 'banana'];
                let b = ['carrot', 'orange'];
                expect(concatUnique(a, b)).toEqual(['apple', 'banana', 'carrot', 'orange']);
            });

            it('keeps sorting and omits duplicates', () => {
                let a = ['carrot', 'apple', 'papaya', 'apple'];
                let b = ['mango', 'banana', 'papaya', 'banana'];
                expect(concatUnique(a, b)).toEqual(['carrot', 'apple', 'papaya', 'mango', 'banana']);
            });

            it('returns the original array when possible', () => {
                let a = ['first', 'second', 'third'];
                let b = ['first'];
                expect(concatUnique(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> without hash/compare function:', () => {

            const APPLE = { id: 1, fruit: 'apple' };
            const BANANA = { id: 2, fruit: 'banana' };
            const CARROT = { id: 3, fruit: 'carrot' };
            const ORANGE = { id: 4, fruit: 'orange' };

            it('concatenates two arrays (by reference)', () => {
                let a = [APPLE, BANANA];
                let b = [CARROT, ORANGE];
                expect(concatUnique(a, b)).toEqual([APPLE, BANANA, CARROT, ORANGE]);
            });

            it('removes duplicates (by reference)', () => {
                let a = [APPLE, BANANA, APPLE];
                let b = [CARROT, ORANGE, APPLE, ORANGE];
                expect(concatUnique(a, b)).toEqual([APPLE, BANANA, CARROT, ORANGE]);
            });

            it('does not remove duplicates (by identity)', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];
                expect(concatUnique(a, b)).not.toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [APPLE, BANANA, CARROT];
                let b = [APPLE];
                expect(concatUnique(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> with hash function:', () => {

            it('concatenates two arrays', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];

                expect(concatUnique(a, b, fruit => fruit.id)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('removes duplicates by identity', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];

                expect(concatUnique(a, b, fruit => fruit.id)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('keeps the order and removes duplicates by identity', () => {
                let a = [
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 1, fruit: 'apple' }
                ];
                let b = [
                    { id: 5, fruit: 'papaya' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' },
                    { id: 5, fruit: 'papaya' },
                    { id: 3, fruit: 'carrot' }
                ];

                expect(concatUnique(a, b, fruit => fruit.id)).toEqual([
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 5, fruit: 'papaya' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 1, fruit: 'apple' }
                ];

                expect(concatUnique(a, b, fruit => fruit.id))
                    .toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> with compare function:', () => {

            it('concatenates two arrays', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];

                expect(concatUnique(a, b, (fruitA, fruitB) => fruitA.id === fruitB.id)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('removes duplicates by equality', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];

                expect(concatUnique(a, b, (fruitA, fruitB) => fruitA.id === fruitB.id)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('keeps the order and removes duplicates by equality', () => {
                let a = [
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 1, fruit: 'apple' }
                ];
                let b = [
                    { id: 5, fruit: 'papaya' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' },
                    { id: 5, fruit: 'papaya' },
                    { id: 3, fruit: 'carrot' }
                ];

                expect(concatUnique(a, b, (fruitA, fruitB) => fruitA.id === fruitB.id)).toEqual([
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 5, fruit: 'papaya' },
                    { id: 4, fruit: 'orange' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 1, fruit: 'apple' }
                ];

                expect(concatUnique(a, b, (fruitA, fruitB) => fruitA.id === fruitB.id))
                    .toBe(a, 'not the same reference');
            });

        });

    });

    describe('removeEntries():', () => {

        describe('for Array<number>:', () => {

            it('removes existing elements', () => {
                let a = [1, 2, 3, 4, 5];
                let b = [2, 3, 5];
                expect(removeEntries(a, b)).toEqual([1, 4]);
            });

            it('ignores remove targets that are not in the target array', () => {
                let a = [1, 2, 3, 4, 5];
                let b = [-4, 9, 15];
                expect(() => removeEntries(a, b)).not.toThrow();
                expect(removeEntries(a, b)).toEqual(a);
            });

            it('keeps the element ordering', () => {
                let a = [1, 9, 4, 7, 3, 2];
                let b = [4, 2];
                expect(removeEntries(a, b)).toEqual([1, 9, 7, 3]);
            });

            it('does not change the input arrays', () => {
                let a = [1, 9, 4, 7, 3, 2];
                let b = [4, 2];
                let A = a.slice();
                let B = b.slice();
                removeEntries(a, b);
                expect(a).toEqual(A);
                expect(b).toEqual(B);
            });

            it('returns the original array when possible', () => {
                let a = [1, 2, 3, 4, 5];
                let b = [99999];
                expect(removeEntries(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<string>:', () => {

            it('removes existing elements', () => {
                let a = ['apple', 'banana', 'carrot', 'orange'];
                let b = ['banana', 'orange'];
                expect(removeEntries(a, b)).toEqual(['apple', 'carrot']);
            });

            it('ignores remove targets that are not in the target array', () => {
                let a = ['apple', 'banana', 'carrot', 'orange'];
                let b = ['pineapple', 'coconut'];
                expect(() => removeEntries(a, b)).not.toThrow();
                expect(removeEntries(a, b)).toEqual(a);
            });

            it('keeps the element ordering', () => {
                let a = ['coconut', 'apple', 'orange', 'banana', 'lime'];
                let b = ['orange'];
                expect(removeEntries(a, b)).toEqual(['coconut', 'apple', 'banana', 'lime']);
                expect(removeEntries(a, b)).not.toEqual(['apple', 'banana', 'coconut', 'lime']);
            });

            it('does not change the input arrays', () => {
                let a = ['coconut', 'apple', 'orange', 'banana', 'lime'];
                let b = ['orange'];
                let A = a.slice();
                let B = b.slice();
                removeEntries(a, b);
                expect(a).toEqual(A);
                expect(b).toEqual(B);
            });

            it('returns the original array when possible', () => {
                let a = ['one', 'two', 'three', 'four'];
                let b = ['not a number'];
                expect(removeEntries(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> without hash/compare function:', () => {

            const APPLE = { id: 1, fruit: 'apple' };
            const BANANA = { id: 2, fruit: 'banana' };
            const CARROT = { id: 3, fruit: 'carrot' };
            const ORANGE = { id: 4, fruit: 'orange' };

            it('removes elements (by reference)', () => {
                let a = [APPLE, BANANA, CARROT, ORANGE];
                let b = [BANANA, ORANGE];
                expect(removeEntries(a, b)).toEqual([APPLE, CARROT]);
            });

            it('ignores non-existing elements', () => {
                let a = [APPLE, BANANA, CARROT];
                let b = [ORANGE];
                expect(removeEntries(a, b)).toEqual([APPLE, BANANA, CARROT]);
            });

            it('ignores elements with a different reference', () => {
                let a = [APPLE, BANANA, CARROT];
                let b = [{ id: 1, fruit: 'apple' }];
                expect(removeEntries(a, b)).toEqual([APPLE, BANANA, CARROT]);
                expect(removeEntries(a, b)).not.toEqual([BANANA, CARROT]);
            });

            it('does not remove by identity without callback', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' }
                ];
                expect(removeEntries(a, b)).toEqual(a);
                expect(removeEntries(a, b)).not.toEqual([
                    { id: 1, fruit: 'apple' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [APPLE, BANANA, CARROT];
                let b = [ORANGE];
                expect(removeEntries(a, b)).toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> with hash function:', () => {

            it('removes elements (by hash)', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' },
                    { id: 5, fruit: 'papaya' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 4, fruit: 'orange' }
                ];
                expect(removeEntries(a, b, el => `${el.id}-${el.fruit}`)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 3, fruit: 'carrot' },
                    { id: 5, fruit: 'papaya' }
                ]);
            });

            it('ignores non-existing elements', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' },
                    { id: 5, fruit: 'papaya' }
                ];
                let b = [
                    { id: 6, fruit: 'coconut' },
                    { id: 7, fruit: 'lime' }
                ];
                expect(removeEntries(a, b, el => `${el.id}-${el.fruit}`)).toEqual(a);
            });

            it('does not reorder the inputs', () => {
                let a = [
                    { id: 5, fruit: 'papaya' },
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 3, fruit: 'carrot' }
                ];
                expect(removeEntries(a, b, el => `${el.id}-${el.fruit}`)).toEqual([
                    { id: 5, fruit: 'papaya' },
                    { id: 1, fruit: 'apple' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 5, fruit: 'papaya' }
                ];

                expect(removeEntries(a, b, el => `${el.id}-${el.fruit}`))
                    .toBe(a, 'not the same reference');
            });

        });

        describe('for Array<Object> with compare function:', () => {

            it('removes elements (by comparison)', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' },
                    { id: 5, fruit: 'papaya' }
                ];
                let b = [
                    { id: 2, fruit: 'banana' },
                    { id: 4, fruit: 'orange' }
                ];
                expect(removeEntries(a, b, (e1, e2) => e1.id === e2.id)).toEqual([
                    { id: 1, fruit: 'apple' },
                    { id: 3, fruit: 'carrot' },
                    { id: 5, fruit: 'papaya' }
                ]);
            });

            it('ignores non-existing elements', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' },
                    { id: 3, fruit: 'carrot' },
                    { id: 4, fruit: 'orange' },
                    { id: 5, fruit: 'papaya' }
                ];
                let b = [
                    { id: 6, fruit: 'coconut' },
                    { id: 7, fruit: 'lime' }
                ];
                expect(removeEntries(a, b, (e1, e2) => e1.id === e2.id)).toEqual(a);
            });

            it('does not reorder the inputs', () => {
                let a = [
                    { id: 5, fruit: 'papaya' },
                    { id: 3, fruit: 'carrot' },
                    { id: 1, fruit: 'apple' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 3, fruit: 'carrot' }
                ];
                expect(removeEntries(a, b, (e1, e2) => e1.id === e2.id)).toEqual([
                    { id: 5, fruit: 'papaya' },
                    { id: 1, fruit: 'apple' },
                    { id: 4, fruit: 'orange' },
                    { id: 2, fruit: 'banana' }
                ]);
            });

            it('returns the original array when possible', () => {
                let a = [
                    { id: 1, fruit: 'apple' },
                    { id: 2, fruit: 'banana' }
                ];
                let b = [
                    { id: 5, fruit: 'papaya' }
                ];

                expect(removeEntries(a, b, (e1, e2) => e1.id === e2.id))
                    .toBe(a, 'not the same reference');
            });

        });

    });

    describe('normalizeEntities():', () => {

        const folderSchema = new schema.Entity('folder');

        it('should return a new object reference', () => {
            let res: any = { folder: { id: 1 } };
            let result = normalizeEntities(res, res.folder, folderSchema);

            expect(res).not.toBe(result);
        });

        it('should add normalized entities to response', () => {
            let res: any = {
                folders: [
                    { id: 1, name: 'folder 1' },
                    { id: 2, name: 'folder 2' }
                ],
                numItems: 2
            };
            let result = normalizeEntities(res, res.folders, new schema.Array(folderSchema));

            expect(result).toEqual({
                folders: [
                    { id: 1, name: 'folder 1' },
                    { id: 2, name: 'folder 2' }
                ],
                numItems: 2,
                _normalized: {
                    result: [1, 2],
                    entities: {
                        folder: {
                            1: { id: 1, name: 'folder 1' },
                            2: { id: 2, name: 'folder 2' }
                        }
                    }
                }
            });
        });
    });

});
