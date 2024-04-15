import { AbstractControl, FormGroup, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { JSON_VALUE_INVALID } from '../common';

export function setControlsEnabled<T = any>(
    group: FormGroup<FormProperties<T>>,
    controls: (keyof T)[],
    enabled: boolean,
    options?: { emitEvent?: boolean, onlySelf?: boolean },
): void {
    for (const ctlName of controls) {
        const ctl = group.get(ctlName as any);
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
