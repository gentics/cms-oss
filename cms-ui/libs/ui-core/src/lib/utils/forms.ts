// eslint-disable-next-line @typescript-eslint/no-unused-vars
import type { AbstractControl, Validators } from '@angular/forms';
import { FormGroup, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import {
    FormProperties,
    JSON_VALUE_INVALID,
    JsonValidationErrorModel,
    MultiValuePatternValidationErrorModel,
    PatternValidatorError,
    RegexValidationErrorModel,
    VALIDATOR_JSON_ERROR_PROPERTY,
    VALIDATOR_MULTI_VALUE_PATTERN_PROPERTY,
    VALIDATOR_PATTERN_PROPERTY,
    VALIDATOR_REGEX_ERROR_PROPERTY,
} from '../common';

export function setEnabled(ctl: AbstractControl, enabled: boolean, options?: { emitEvent?: boolean, onlySelf?: boolean }): void {
    if (ctl == null) {
        return;
    }

    // Nothing to change
    if ((enabled && ctl.enabled) || (!enabled && !ctl.enabled)) {
        return;
    }

    if (enabled) {
        ctl.enable(options);
    } else {
        ctl.disable(options);
    }
}

export function setControlsEnabled<T = any>(
    group: FormGroup<FormProperties<T>>,
    controls: (keyof T)[],
    enabled: boolean,
    options?: { emitEvent?: boolean, onlySelf?: boolean },
): void {
    for (const ctlName of controls) {
        setEnabled(group.controls[ctlName], enabled, options);
    }
}

export function setControlsValidators(
    group: UntypedFormGroup,
    controls: string[],
    validators: ValidatorFn | ValidatorFn[] | null,
): void {
    for (const ctlName of controls) {
        const ctl = group.get(ctlName);
        if (ctl == null) {
            continue;
        }

        ctl.setValidators(validators);
    }
}

export const createJsonValidator: () => ValidatorFn = () => (control): null | JsonValidationErrorModel => {
    if (control.value !== JSON_VALUE_INVALID) {
        return null;
    }

    return {
        [VALIDATOR_JSON_ERROR_PROPERTY]: {
            actualValue: control.value,
        },
    };
}

/**
 * Extension of angulars of angulars `Validator.pattern`, which will also add the provided hints
 * in the error object, so they can be used in error messages.
 *
 * @param pattern The pattern/regex to match against.
 * @param hints Hints for error messages.
 * @returns A validator for the pattern
 *
 * @see {@link Validators.pattern}
 */
export function createRegexValidator(pattern: string | RegExp, hints: string[] = []): ValidatorFn {
    const parsed = asRegExp(pattern);
    if (parsed == null) {
        return () => null;
    }

    return (control): null | RegexValidationErrorModel => {
        let value = control.value;

        // Allow empty values
        if (value == null) {
            return null;
        }

        if (typeof value !== 'string') {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            value = value.toString();
        }

        // Allow empty values or which match the regex
        if (value === '' || parsed.regex.test(value)) {
            return null;
        }

        const tmp: PatternValidatorError = {
            requiredPattern: parsed.str,
            actualValue: value,
        };

        return {
            [VALIDATOR_PATTERN_PROPERTY]: tmp,
            [VALIDATOR_REGEX_ERROR_PROPERTY]: {
                hints,
            },
        };
    }
}

/**
 * Creates a Regex-Validator for cms server-side properties.
 *
 * @param prefix The Environment-Variable prefix that needs to be present for the property.
 * @returns A validator for cms server-side properties
 *
 * @see createRegexValidator
 */
export function createPropertyPatternValidator(prefix: string): ValidatorFn {
    const regex = new RegExp(`^\\\${(?:env|sys):${prefix}_[\\w]+}$`);
    return createRegexValidator(regex, [`\${env:${prefix}_...}`, `\${sys:${prefix}_...}`]);
}

/**
 * Copy of angulars `Validator.pattern`, but which additionally allows `string[]` values to be validated.
 * The error object is going to be the same, with the addion of failed indices when it's a `string[]`.
 *
 * @param pattern Pattern to validate against. If it is a string, it'll be parsed to a RegExp.
 * @returns `null` if the control is valid; otherwise an error object with the found validity issues.
 * @see {@link Validators.pattern}
 */
export function createMultiValuePatternValidator(pattern: string | RegExp): ValidatorFn {
    const parsed = asRegExp(pattern);
    if (parsed == null) {
        return () => null;
    }

    function valid(value: any): boolean {
        if (value == null) {
            return true;
        }
        // Do not allow non-string values
        if (typeof value !== 'string') {
            return false;
        }

        return value.length === 0 || parsed.regex.test(value);
    }

    return (control): MultiValuePatternValidationErrorModel | null => {
        if (control.value == null) {
            return null;
        }

        if (typeof control.value === 'string') {
            return valid(control.value) ? null : {
                [VALIDATOR_PATTERN_PROPERTY]: {
                    actualValue: control.value,
                    requiredPattern: parsed.str,
                },
            };
        }

        // Allow invalid values?
        if (!Array.isArray(control.value)) {
            return null;
        }

        const failed: number[] = [];
        control.value.forEach((val, idx) => {
            if (!valid(val)) {
                failed.push(idx);
            }
        });

        return failed.length === 0 ? null : {
            [VALIDATOR_PATTERN_PROPERTY]: {
                actualValue: control.value,
                requiredPattern: parsed.str,
            },
            [VALIDATOR_MULTI_VALUE_PATTERN_PROPERTY]: failed,
        };
    };
}

function asRegExp(pattern: string | RegExp): null | { str: string, regex: RegExp } {
    // type checks
    if (pattern == null || (typeof pattern !== 'string' && !(pattern instanceof RegExp))) {
        return null;
    }

    let regex: RegExp;
    let str: string;

    if (typeof pattern === 'string') {
        str = '';

        if (pattern.charAt(0) !== '^') {
            str += '^';
        }

        str += pattern;

        if (pattern.charAt(pattern.length - 1) !== '$') {
            str += '$';
        }

        regex = new RegExp(str);
    } else {
        str = pattern.toString();
        regex = pattern;
    }

    return { str, regex };
}
