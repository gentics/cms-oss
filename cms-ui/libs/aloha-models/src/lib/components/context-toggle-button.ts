import { AlohaCoreComponentNames } from './base-component';
import { AlohaContextButtonComponent } from './context-button';
import { BaseToggleButton } from './toggle-button';

export interface AlohaContextToggleButtonComponent<T> extends Omit<AlohaContextButtonComponent<T>, 'type'>, BaseToggleButton {
    type: AlohaCoreComponentNames.CONTEXT_TOGGLE_BUTTON;
}
