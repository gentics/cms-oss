import { AlohaCoreComponentNames } from './base-component';
import { AlohaSplitButtonComponent } from './split-button';

export interface AlohaToggleSplitButtonComponent extends Omit<AlohaSplitButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON;

    active: boolean;
    alwaysSecondary: boolean;

    onToggle?: (isActive: boolean) => void;
    toggleActivation: () => void;
    activate: () => void;
    deactivate: () => void;
}
