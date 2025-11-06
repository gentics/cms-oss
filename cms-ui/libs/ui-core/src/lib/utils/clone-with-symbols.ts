export function copySymbols<T extends object>(source: T, target: T, shallow: boolean = false): void {
    const symbols = Object.getOwnPropertySymbols(source);
    for (const name of symbols) {
        target[name] = source[name];
    }

    if (!shallow) {
        const properties = Object.getOwnPropertyNames(source);
        for (const prop of properties) {
            if (source[prop] != null && typeof source[prop] === 'object') {
                copySymbols(source[prop], target[prop], shallow);
            }
        }
    }
}

/**
 * Uses {@link structuredClone} to create a clone of the provided element.
 * Will also copy over all symbols which are attached to this or nested elements,
 * as a regular clone would not do that.
 *
 * @param source Source element to clone
 * @param shallow If it should only clone the symbols of the root element.
 */
export function cloneWithSymbols<T extends object>(source: T, shallow: boolean = false): T {
    const clone = structuredClone(source);
    copySymbols(source, clone, shallow);

    return clone;
}
