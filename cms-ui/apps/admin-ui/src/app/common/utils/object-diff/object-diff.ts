import { isEqual, transform } from 'lodash';

/**
 * Computes the differences (deep equality) between two objects.
 *
 * @returns a new object that contains those properties of `object`
 * that are different from the corresponding properties in `base`.
 *
 * @note If a property exists in `object`, but not in `base`, it will be included
 * in the result.
 * However, if a property exists in `base`, but not in `object`, it will not be included
 * in the result.
 */
export function objectDiff<T, U>(object: T, base: U): Partial<T> {
    return transform(object as any, (result, value, key) => {
        if (!isEqual(value, base[key])) {
            result[key] = value;
        }

    });
}
