import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Returns `null` if `!control.value` or valid JSON, otherwise `{ errorIsInvalidJson: true }`.
 */
export const GtxJsonValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    control.hasError('errorIsInvalidJson')
    const validationError = { errorIsInvalidJson: true };

    // if input is empty, there is no error
    if (!value) {
        return null;
    }

    if (typeof value === 'string') {
        let parsed: object;
        try {
            parsed = JSON.parse(value);
            if (parsed != null && typeof parsed === 'object') {
                return null;
            }
        } catch (error) {
            return validationError;
        }
    }

    // if in doubt, return error
    return validationError;
}
