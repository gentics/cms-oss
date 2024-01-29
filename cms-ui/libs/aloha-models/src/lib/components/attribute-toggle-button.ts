import { AlohaAttributeButtonComponent } from "./attribute-button";
import { AlohaCoreComponentNames } from "./base-component";

export interface AlohaAttributeToggleButtonComponent extends Omit<AlohaAttributeButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON;

    active: boolean;

    onToggle?: (isActive: boolean) => void;
    toggleActivation: () => void;
    activate: () => void;
    deactivate: () => void;
}
