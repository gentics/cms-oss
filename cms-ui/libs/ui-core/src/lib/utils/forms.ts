import { UntypedFormGroup } from '@angular/forms';

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
