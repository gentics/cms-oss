/**
 * Components with boolean inputs may receive the value as an actual boolean (if data-bound `[prop]="false"`) or as
 * a string representation of a boolean (if passed as a regular attribute `prop="false"`).
 * In the latter case, we want to ensure that the string version is correctly coerced to its boolean counterpart.
 */

import { SimpleChanges } from '@angular/core';

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function coerceToBoolean(val: any, defaultValue: boolean = false): boolean {
    if (val == null) {
        return defaultValue;
    }

    return val === true || val === 'true' || val === '';
}

export type CoerceOption<T> = (keyof T | [key: keyof T, defaultValue?: boolean]);

export function coerceInstance<T>(instance: T, options: CoerceOption<T>[], changes: SimpleChanges): (keyof T)[] {
    const changed: (keyof T)[] = [];

    // Coerce all boolean inputs automatically
    for (const inputToConsider of options) {
        let name: keyof T;
        let defaultValue = false;

        if (typeof inputToConsider === 'string') {
            name = inputToConsider;
        } else if (Array.isArray(inputToConsider) && inputToConsider.length > 0) {
            name = inputToConsider[0];
            defaultValue = inputToConsider[1] ?? false;
        } else {
            console.warn(`Provided invalid boolean-input option: ${String(inputToConsider)}!`);
            continue;
        }

        if (!(name in changes)) {
            continue;
        }

        const newBool = coerceToBoolean(instance[name], defaultValue);
        // Nothing to do,same value
        if ((instance as any)[name] === newBool) {
            continue;
        }

        // Fix overly strict type checks
        (instance as any)[name] = newBool;
        changed.push(name);
    }

    return changed;
}
