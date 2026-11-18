import { AbstractControl, ValidationErrors, Validators } from '@angular/forms';

/**
 * Validators for a new password. The backend applies the same rule when a password is set with a password link.
 */
export const PASSWORD_VALIDATORS = [
    Validators.required,
    Validators.minLength(4),
    Validators.maxLength(20),
    Validators.pattern('^[\\w\\-\\.@]+$'),
];

/**
 * Group validator which checks that the controls `password1` and `password2` contain equal values.
 */
export function passwordsAreEqual(group: AbstractControl): ValidationErrors | null {
    return group.get('password1').value === group.get('password2').value ? null : { areEqual: true };
}
