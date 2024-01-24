import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export type ButtonIcon = string | { primary: string, secondary?: string };

export interface AlohaButtonComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.BUTTON;

    text?: string;
    html?: string;
    icon?: ButtonIcon;
    iconUrl?: string;
    tooltip?: string;

    closeTooltip: () => void;
    click: () => void;
    setIcon: (icon?: ButtonIcon) => void;
    setText: (text: string) => void;
    setTooltip: (tooltip: string) => void;
}
