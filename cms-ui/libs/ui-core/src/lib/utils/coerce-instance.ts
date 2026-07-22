import { SimpleChanges } from '@angular/core';
import { coerceToBoolean } from '@gentics/common';

/**
 * @deprecated Deprecated since {@link coerceInstance} is deprecated
 */
export type CoerceOption<T> = (keyof T | [key: keyof T, defaultValue?: boolean]);

/**
 * Applies the {@link coerceToBoolean} function for each instance with the provided optios on change,
 * and corrects the values.
 * @example
 * ```ts
 * class MyComponent implements OnChanges {
 *      ngOnChanges(changes) {
 *          coerceInstance(this, ['disabled', 'pure', ['excludeExtras', true]], changes);
 *      }
 * }
 * ```
 * @deprecated Use the {@link Input.transform} option with the {@link coerceToBoolean}/{@link coerceToTruelean} transformer instead.
 * Will be removed in the next major verison.
 */
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

        // Nothing to do, same value
        if (newBool === changes[name as any].previousValue) {
            continue;
        }

        // Fix overly strict type checks
        (instance as any)[name] = newBool;
        changed.push(name);
    }

    return changed;
}
