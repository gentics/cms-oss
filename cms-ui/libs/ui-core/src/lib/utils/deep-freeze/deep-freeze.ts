
/**
 * Freezes an object and all nested objects.
 *
 * Based on the `deepFreeze()` implementation presented at
 * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze
 */
export function deepFreeze<T>(obj: T): T {
    // Retrieve the property names defined on object
    const propNames = Object.getOwnPropertyNames(obj);

    // Determine if self is already frozen (which would not necessarily imply that it is deeply frozen).
    const objAlreadyFrozen = Object.isFrozen(obj);

    // Freeze properties before freezing self
    for (const name of propNames) {
        const value = obj[name];
        if (value && typeof value === 'object') {
            const frozenValue = deepFreeze(value);
            if (!objAlreadyFrozen) {
                obj[name] = frozenValue;
            }
        }
    }

    return Object.freeze(obj);
}
