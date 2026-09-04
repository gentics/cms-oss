/**
 * Components may receive boolean inputs in the following ways:
 * ```html
 * <my-component multiple />
 * <my-component multiple="true" />
 * <my-component [multiple]="true" />
 * ```
 *
 * In the code example, only the last one would actually be a correct boolean value for the input,
 * while the other two examples would yield `''` and `'true'`.
 * To get correct boolean values, this helper method can be used to get the expected result:
 *
 * ```ts
 * coerceToBoolean('') // true
 * coerceToBoolean('true') // true
 * coerceToBoolean('false') // false
 * coerceToBoolean('invalid') // false
 * coerceToBoolean(true) // true
 * ```
 *
 * You may also provide a `defaultValue` to change the behaviour for missing values:
 *
 * ```ts
 * coerceToBoolean(null) // false
 * coerceToBoolean(null, true) // true
 * ```
 *
 * It's best used as the {@link Input.transform} function,
 * to perform the coercion as soon and integrated as possible.
 * When using it as a `transform` function, you may not alter/provide a `defaultValue` however,
 * which is why a dedicated {@link coerceToTruelean} function exists:
 *
 * ```ts
 * class MyComponent {
 *      \@Input({
 *          transform: coerceToBoolean
 *      })
 *      public disabled = false;
 *
 *      \@Input({
 *          transform: coerceToTruelean
 *      })
 *      public showDetails = true;
 * }
 * ```
 *
 * @param val The value to coerce/parse to a boolean
 * @param defaultValue The value to retrun when `val` is `null`.
 * @return The coerced/parsed boolean of `val`, or `defaultValue` when `null`.
 * @deprecated Use the angular `booleanAttribute` function instead
 */
// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function coerceToBoolean(val: any, defaultValue: boolean = false): boolean {
    if (val == null) {
        return defaultValue;
    }

    return val === true || val === 'true' || val === '';
}

/**
 * @see {@link coerceToBoolean}
 * @deprecated Use the angular `booleanAttribute` function instead
 */
// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function coerceToTruelean(val: any): boolean {
    if (val == null) {
        return true;
    }

    return val === true || val === 'true' || val === '';
}
