import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Returns `null` if `!control.value` or valid JSON, otherwise `{ errorIsInvalidJson: true }`.
 */
export const GtxJsonValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const _value = control.value;
    control.hasError('errorIsInvalidJson')
    const _error = { errorIsInvalidJson: true };

    // if input is empty, there is no error
    if (!_value) {
        return null;
    }

    if (typeof _value === 'string') {
        let _parsedValue: object;
        try {
            _parsedValue = JSON.parse(_value);
            if (_parsedValue instanceof Object && Object.keys(_parsedValue).length) {
                return null;
            }
        } catch (error) {
            return _error;
        }
    }

    // if in doubt, return error
    return _error;
}
