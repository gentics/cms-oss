import { AlohaCoreComponentNames } from './base-component';
import { AlohaSplitButtonComponent } from './split-button';
import { BaseToggleButton } from './toggle-button';

export interface AlohaToggleSplitButtonComponent extends Omit<AlohaSplitButtonComponent, 'type'>, BaseToggleButton {
    type: AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON;

    alwaysSecondary: boolean;
}
