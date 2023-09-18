import { UntypedFormGroup, ValidatorFn } from '@angular/forms';

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

export function setControlsValidators(
    group: UntypedFormGroup,
    controls: string[],
    validators: ValidatorFn | ValidatorFn[] | null
): void {
    for (const ctlName of controls) {
        const ctl = group.get(ctlName);
        if (ctl == null) {
            continue;
        }

        ctl.setValidators(validators);
    }
}
