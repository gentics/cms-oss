import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaButtonComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.BUTTON;

    text?: string;
    tooltip?: string;
    icon?: string;
    iconOnly: boolean;
    iconHollow: boolean;

    closeTooltip: () => void;
    click: () => void;
    setText: (text: string) => void;
    setTooltip: (tooltip: string) => void;
    setIcon: (icon?: string) => void;
    setIconOnly: (iconOnly: boolean) => void;
    setIconHollow: (hollow: boolean) => void;
}
