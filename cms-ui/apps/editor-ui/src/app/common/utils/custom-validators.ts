import { AbstractControl, ValidatorFn } from '@angular/forms';

export function numberBetween(min: number, max: number): ValidatorFn {
    if (max < min) {
        throw new Error('numberBetween validator created with a maximum below minimum');
    }

    return (control: AbstractControl) => {
        if (typeof control.value !== 'number') {
            return { valid: false, NaN: true };
        }
        if (control.value < min) {
            return { valid: false, belowMinimum: true };
        }
        if (control.value > max) {
            return { valid: false, aboveMaximum: true };
        }

        return null;
    };
}
