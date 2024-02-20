import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaButtonComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.BUTTON;

    text?: string;
    icon?: string;
    iconUrl?: string;
    tooltip?: string;

    closeTooltip: () => void;
    click: () => void;
    setIcon: (icon?: string) => void;
    setText: (text: string) => void;
    setTooltip: (tooltip: string) => void;
}
