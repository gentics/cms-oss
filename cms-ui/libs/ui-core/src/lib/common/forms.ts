import { AbstractControl } from '@angular/forms';

/** A Symbol which inicates that the JSON value is invalid/could not be parsed correctly. */
export const JSON_VALUE_INVALID = Symbol('invald-json');

// Angular Validators
export const VALIDATOR_PATTERN_PROPERTY = 'pattern';

// Custom Validators
export const VALIDATOR_MULTI_VALUE_PATTERN_PROPERTY = 'gtxMultiValuePattern';
export const VALIDATOR_REGEX_ERROR_PROPERTY = 'gtxRegExp';
export const VALIDATOR_JSON_ERROR_PROPERTY = 'gtxJsonInvalid';

export interface PatternValidatorError {
    actualValue: any;
    requiredPattern: string;
}

export interface RegexValidatorError {
    hints: string[];
}

export interface JsonValidatorError {
    actualValue: any;
}

export interface PatternValidationErrorModel {
    [VALIDATOR_PATTERN_PROPERTY]: PatternValidatorError;
}

export interface MultiValuePatternValidationErrorModel extends PatternValidationErrorModel {
    [VALIDATOR_MULTI_VALUE_PATTERN_PROPERTY]?: number[];
}

export interface RegexValidationErrorModel extends PatternValidationErrorModel {
    [VALIDATOR_REGEX_ERROR_PROPERTY]: RegexValidatorError;
}

export interface JsonValidationErrorModel {
    [VALIDATOR_JSON_ERROR_PROPERTY]: JsonValidatorError;
}

/**
 * Utility type which is ment to be used on typed FormGroups, to properly
 * enforce the typings and required fields:
 * ```typescript
 * type MyEntity = {
 *      name: string;
 *      description: string;
 *      version: number;
 * }
 * const form = new FormGroup<FormProperties<MyEntity>>({
 *      // ...
 * });
 * ```
 */
export type FormProperties<T> = {
    [P in keyof T]: AbstractControl<T[P]>;
};
