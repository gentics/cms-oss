import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface BaseToggleButton {
    active: boolean;

    onToggle?: (isActive: boolean) => void;
    setActive: (active: boolean) => void;
    toggleActivation: () => void;
    activate: () => void;
    deactivate: () => void;
}

export interface AlohaToggleButtonComponent extends Omit<AlohaButtonComponent, 'type'>, BaseToggleButton {
    type: AlohaCoreComponentNames.TOGGLE_BUTTON;

    /** @deprecated use `setValue` instead. */
    setState: (toggled: boolean) => void;
    /** @deprecated use `getValue` instead. */
    getState: () => void;
}
