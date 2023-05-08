/**
 * Compares simple hash objects by value.
 *
 * Intended for comparing same-structure objects.
 * Optimized for simplicity & speed, not covering all edge-cases.
 *
 * @example
 *     deepEqual({ a: { b: 1 } }, { a: { b: 2 } }) === false
 *     deepEqual({ a: 1 }, { a: 1, b: 2 }) === false
 *     deepEqual({ a: 1, b: 2 }, { a: 1 }) === false
 *     deepEqual({ a: { b: 2 } }, { a: { b: 2 } }) === true
 *     deepEqual({ a: 1, b: 2 }, { b: 2, a: 1 }) === true
 */
export function deepEqual<T extends Object>(left: T, right: T): boolean;
export function deepEqual(left: any, right: any): boolean {
    if (left === right) {
        return true;
    }

    if (left == null || right == null) {
        return false;
    }

    function areBothOfType(type: 'string' | 'number' | 'boolean'): boolean {
        return typeof left === type && typeof right === type;
    }

    if (areBothOfType('string') || areBothOfType('number') || areBothOfType('boolean')) {
        return left === right;
    }

    const leftKeys = Object.keys(left);
    const rightKeys = Object.keys(right);
    if (leftKeys.length !== rightKeys.length) {
        return false;
    }

    for (let key of leftKeys) {
        if (left[key] !== right[key]) {
            if (typeof left[key] !== typeof right[key]) {
                return false;
            } else if (typeof left[key] === 'object') {
                if (!deepEqual(left[key], right[key])) {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
    return true;
}
