import { ValidatorFn } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '../../models';

/**
 * @deprecated Not needed anymore, as forms are now self-validating. Will be removed next feature-release.
 */
export function createNestedControlValidator(): ValidatorFn {
    return (control) => {
        if (control.value === CONTROL_INVALID_VALUE) {
            return { invalidValue: true };
        }

        return null;
    };
}
