import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface AlohaAttributeButtonComponent extends Omit<AlohaButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.ATTRIBUTE_BUTTON;

    targetElement: HTMLElement | JQuery;
    targetAttribute: string;
    inputLabel: string;
    panelLabel: string;
    panelActiveOn?: string;

    updateTargetElement: (element: null | HTMLElement | JQuery) => void;
}
