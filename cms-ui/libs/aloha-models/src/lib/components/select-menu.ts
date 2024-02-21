import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface SelectMenuOption {
    id: string;
    label?: string;
    icon?: string;
    newTab?: boolean;
    isMultiStep?: boolean;
    multiStepContext?: MultiStepOptionContext<any>;
}

export interface MultiStepOptionContext<T> {
    label?: string;
    type: string;
    options?: Record<string, any>;
    initialValue?: T;
    requiresConfirm?: boolean;
    confirmLabel?: string;
}

export interface SelectMenuSelectEvent<T> {
    id: string;
    value?: T;
}

export interface AlohaSelectMenuComponent extends Omit<AlohaComponent, 'type'> {
    type: AlohaCoreComponentNames.SELECT_MENU;

    options: SelectMenuOption[];

    activeOption?: string;

    iconsOnly: boolean;

    onSelect: (event: SelectMenuSelectEvent<any>) => void;
    setOptions: (options: SelectMenuOption[]) => void;
    setIconsOnly: (iconsOnly: boolean) => void;
}
