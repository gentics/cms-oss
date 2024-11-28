
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { SimpleChanges, type Input } from '@angular/core';

/**
 * Components with boolean inputs may receive the value as an actual boolean (if data-bound `[prop]="false"`) or as
 * a string representation of a boolean (if passed as a regular attribute `prop="false"`).
 * In the latter case, we want to ensure that the string version is correctly coerced to its boolean counterpart.
 */
// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function coerceToBoolean(val: any, defaultValue: boolean = false): boolean {
    if (val == null) {
        return defaultValue;
    }

    return val === true || val === 'true' || val === '';
}

/**
 * Transformer wrapper for {@link coerceToBoolean}, to be used in {@link Input.transform}.
 * @param defaultValue The default value to apply when the value could not be determined.
 * @returns A boolean value based on the input value.
 *
 * @example
 * ```ts
 * class MyComponent {
 *      \@Input({
 *          transform: transformToBoolean()
 *      })
 *      public disabled = false;
 *
 *      \@Input({
 *          transform: transformToBoolean(true)
 *      })
 *      public showDetails = true;
 * }
 * ```
 */
export function transformToBoolean(defaultValue: boolean = false): (value: any) => boolean {
    return (value) => coerceToBoolean(value, defaultValue);
}

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
 *
 * @deprecated Use the {@link Input.transform} option with the {@link transformToBoolean} transformer instead.
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
