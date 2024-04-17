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
