import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface AlohaAttributeButtonComponent extends Omit<AlohaButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.ATTRIBUTE_BUTTON;

    targetElement: HTMLElement | JQuery;
    targetAttribute: string;
    inputLabel: string;
    inputActive: boolean;
    panelLabel: string;
    panelActiveOn?: string;

    activateInput: (active: boolean) => void;
    setTargetElement: (element: null | JQuery) => void;
}
