import { AlohaAttributeButtonComponent } from './attribute-button';
import { AlohaCoreComponentNames } from './base-component';
import { BaseToggleButton } from './toggle-button';

export interface AlohaAttributeToggleButtonComponent extends Omit<AlohaAttributeButtonComponent, 'type'>, BaseToggleButton {
    type: AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON;
}
