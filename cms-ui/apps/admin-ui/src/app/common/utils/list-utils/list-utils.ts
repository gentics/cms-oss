import { EntityIdType } from '@gentics/cms-models';

/**
 * Concatenate two Lists/arrays without duplicates, maintaining array order.
 * Can be passed a hashing function or a comparator to handle objects by value.
 * @example
 *   concatUnique([1, 2, 5, 7, 4], [2, 3, 9, 7])
 *   // result: [1, 2, 3, 5, 7, 9]
 *
 *   concatUnique([{id:5}, {id:7}], [{id:7}], obj => obj.id)
 *   // result: [{id: 5}, {id: 7}]
 */
export function concatUnique(left: number[], right: number[]): number[];
export function concatUnique(left: string[], right: string[]): string[];
export function concatUnique<T>(left: T[], right: T[], compareFn?: (a: T, b?: T) => boolean | string | number | object): T[];
export function concatUnique<T, H>(left: any, right: any, fn?: Function): T[] {
    const both: T[] = [].concat(left, right);

    let result: T[];

    if (!fn) {
        // compare by identity
        const resultSet = new Set<T>(both);
        result = Array.from(resultSet);
    } else if (fn.length === 1) {
        // fn is a hashing function
        const calcHash = fn as (o: T) => H;

        const resultMap = new Map<H, T>();
        for (const value of both) {
            const hash = calcHash(value);
            if (!resultMap.has(hash)) {
                resultMap.set(hash, value);
            }
        }

        result = Array.from(resultMap.values());
    } else if (fn.length === 2) {
        // fn is a comparator function
        const compare = fn as (a: T, b: T) => boolean;

        const resultArray: T[] = [];
        for (const r of both) {
            let alreadyIncluded = false;
            for (const l of resultArray) {
                if (compare(l, r) === true) {
                    alreadyIncluded = true;
                    break;
                }
            }
            if (!alreadyIncluded) {
                resultArray.push(r);
            }
        }
        result = resultArray;
    } else {
        throw new Error('Function parameter is not a hashing or comparator function');
    }

    return result.length !== left.length ? result : left;
}

/**
 * Removes entries from a List/array , maintaining array order.
 * Can be passed a hashing function or a comparator to handle objects by value.
 * @example
 *   removeEntries([1, 2, 5, 7, 4], [2, 5])
 *   // result: [1, 7, 4]
 *
 *   removeEntries([{id:5}, {id:7}], [{id:7}], obj => obj.id)
 *   // result: [{id: 5}]
 */
export function removeEntries(haystack: number[], needle: number[]): number[];
export function removeEntries(haystack: string[], needle: string[]): string[];
export function removeEntries<T>(haystack: T[], needle: T[], compareFn?: (a: T, b?: T) => boolean | string | number | object): T[];
export function removeEntries<T, H>(left: T[], right: T[], fn?: Function): T[] {
    let result: T[];

    if (left === undefined || !left.length) {
        return [];
    }

    if (!fn) {
        // compare by identity
        const removeSet = new Set<T>(right);
        result = left.filter(el => !removeSet.has(el));
    } else if (fn.length === 1) {
        // fn is a hashing function
        const calcHash = fn as (o: T) => H;

        const deleteHashes = new Set<H>(right.map(del => calcHash(del)));
        result = left.filter(el => !deleteHashes.has(calcHash(el)));
    } else if (fn.length === 2) {
        // fn is a comparator function
        const compare = fn as (a: T, b: T) => boolean;

        result = left.filter(l => right.every(r => compare(l, r) === false));
    } else {
        throw new Error('Function parameter is not a hashing or comparator function');
    }

    return result.length !== left.length ? result : left;
}

/**
 * Removes the `needle` from the `haystack` and returns a new haystack without the `needle`.
 * This does not modify the original `haystack`.
 *
 * This is a memory optimized, albeit slightly slower, version of `removeEntries()`, because it
 * first checks if the haystack contains the needle before creating a new haystack. It is
 * intended to be used if a single needle needs to be removed from multiple haystacks, but
 * with the likelihood of not being contained in many of them.
 *
 * @return A new haystack without `needle` if it was present in the original one, or
 * the `original` haystack if the needle was not present, or `undefined` if `haystack` was not defined or `null`.
 */
export function removeEntryIfPresent(haystack: EntityIdType[], needle: EntityIdType): EntityIdType[] {
    if (!haystack) {
        return undefined;
    }

    let result: EntityIdType[];
    const isPresent = haystack.findIndex(entry => entry === needle) !== -1;
    if (isPresent) {
        result = haystack.filter(entry => entry !== needle);
    } else {
        result = haystack;
    }
    return result;
}
