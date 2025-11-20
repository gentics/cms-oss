import { Validators } from '@angular/forms';

// *************************************************************************************************
/**
 * In multiple place user handling might require the following methods to use.
 */
// *************************************************************************************************

/**
 * Default password reactive form input validators
 */
export const PASSWORD_VALIDATORS = [
    Validators.required,
    Validators.minLength(4),
    Validators.maxLength(20),
    Validators.pattern('^[\\w\\-\\.@]+$'),
];
