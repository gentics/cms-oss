import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaButtonComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.BUTTON;

    text?: string;
    html?: string;
    icon?: string;
    iconUrl?: string;
    tooltip?: string;

    closeTooltip: () => void;
    click: () => void;
    setIcon: (icon: null | string | { primary: string, secondary?: string }) => void;
    setText: (text: string) => void;
    setTooltip: (tooltip: string) => void;
}
