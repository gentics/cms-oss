export function copySymbols<T extends object>(source: T, target: T, shallow = false): void {
    const symbols = Object.getOwnPropertySymbols(source);
    for (const name of symbols) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (target as any)[name] = (source as any)[name];
    }

    if (!shallow) {
        const properties = Object.getOwnPropertyNames(source);
        for (const prop of properties) {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            if ((source as any)[prop] != null && typeof (source as any)[prop] === 'object') {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                copySymbols((source as any)[prop], (target as any)[prop], shallow);
            }
        }
    }
}

type Writeable<T> = { -readonly [P in keyof T]: Writeable<T[P]> };

/**
 * Uses {@link structuredClone} to create a clone of the provided element.
 * Will also copy over all symbols which are attached to this or nested elements,
 * as a regular clone would not do that.
 *
 * @param source Source element to clone
 * @param shallow If it should only clone the symbols of the root element.
 */
export function cloneWithSymbols<T extends object>(source: T, shallow = false): Writeable<T> {
    const clone = structuredClone(source);
    copySymbols(source, clone, shallow);

    return clone;
}
