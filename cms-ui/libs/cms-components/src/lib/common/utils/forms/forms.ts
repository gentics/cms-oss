import { ValidatorFn } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '../../models';

export function createNestedControlValidator(): ValidatorFn {
    return (control) => {
        if (control.value === CONTROL_INVALID_VALUE) {
            return { invalidValue: true };
        }

        return null;
    }
}

const DEFAULT_FLAGS = 'u';
const DEFAULT_VALIDITY: ValidityState = {
    valid: false,
    badInput: false,
    customError: false,
    patternMismatch: false,
    rangeOverflow: false,
    rangeUnderflow: false,
    stepMismatch: false,
    tooLong: false,
    tooShort: false,
    typeMismatch: false,
    valueMissing: false,
};

/**
 * Performs RegExp matching and checks if the value(s) match the provided pattern.
 * This Validator works different from the build in Angular one, in that it has better
 * control over invalid values, and works for controls which provide either single or
 * multiple values (`string | string[]`).
 *
 * Follows the naming from the ValidityState: https://developer.mozilla.org/en-US/docs/Web/API/ValidityState
 *
 * @param pattern Pattern to validate against. If it is a string, it'll be parsed to a RegExp with the `u` flag.
 * @param allowNull If it should allow null-values (`null`, `undefined`, and empty strings). Defaults to `false`.
 * @param strict If it should invalidate if there's a non-string value.
 * @returns `null` if the control is valid; otherwise an error object with the found validity issues.
 */
export function createPatternValidator(
    pattern: string | RegExp,
    allowNull: boolean = false,
    strict: boolean = true,
): ValidatorFn {
    if (pattern == null) {
        return null;
    }

    if (typeof pattern === 'string') {
        try {
            // Add start and end markers to test the whole string
            if (!pattern.startsWith('^')) {
                pattern = '^' + pattern;
            }
            if (!pattern.endsWith('$')) {
                pattern += '$';
            }
            pattern = new RegExp(pattern, DEFAULT_FLAGS);
        } catch (e) {
            console.error(e);
            return null;
        }
    }

    return (control) => {
        const value = control.value;

        const check: (value: any) => ValidityState | null = (element) => {
            if (element == null) {
                if (allowNull) {
                    return null;
                }
                return { ...DEFAULT_VALIDITY, valueMissing: true };
            }
            if (typeof element !== 'string') {
                if (strict) {
                    return { ...DEFAULT_VALIDITY, badInput: true };
                }
                // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
                element = '' + value;
            }
            if (element === '') {
                if (allowNull) {
                    return null;
                }
                return { ...DEFAULT_VALIDITY, valueMissing: true };
            }
            if (!(pattern as RegExp).test(element)) {
                return { ...DEFAULT_VALIDITY, patternMismatch: true };
            }

            return null;
        };

        if (!Array.isArray(value)) {
            return check(value);
        }

        let index = 0;
        let hasErrors = false;
        const errors = {};

        for (const element of value) {
            const err: any = check(element);
            if (err != null) {
                err.index = index;
                errors[index] = err;
                hasErrors = true;
            }
            index++;
        }

        if (hasErrors) {
            return {
                valid: false,
                errors,
            };
        }

        return null;
    };
}