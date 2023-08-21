import { UntypedFormGroup, AbstractControl, ValidatorFn } from '@angular/forms';
import { JSON_VALUE_INVALID } from '../common';

export function setControlsEnabled(
    group: UntypedFormGroup,
    controls: string[],
    enabled: boolean,
    options?: { emitEvent?: boolean, onlySelf?: boolean },
): void {
    for (const ctlName of controls) {
        const ctl = group.get(ctlName);
        if (ctl == null) {
            continue;
        }

        // Nothing to change
        if ((enabled && ctl.enabled) || (!enabled && !ctl.enabled)) {
            continue;
        }

        if (enabled) {
            ctl.enable(options);
        } else {
            ctl.disable(options);
        }
    }
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

export const createJsonValidator: () => ValidatorFn = () => (control) => {
    if (control.value === JSON_VALUE_INVALID) {
        return { jsonInvalid: true };
    }
    return null;
}
