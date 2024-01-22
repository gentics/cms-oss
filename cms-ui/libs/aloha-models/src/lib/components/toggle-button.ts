import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface AlohaToggleButtonComponent extends Omit<AlohaButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.TOGGLE_BUTTON;

    active: boolean;

    /** @deprecated use `setValue` instead. */
    setState: (toggled: boolean) => void;
    /** @deprecated use `getValue` instead. */
    getState: () => void;
}
