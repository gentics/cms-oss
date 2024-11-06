import { DynamicControlConfiguration } from '../overlay-elements';
import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

interface BaseSelectMenuOption {
    id: string;
    label?: string;
    icon?: string;
    iconHollow?: boolean;
    isMultiStep?: boolean;
}

export interface MultiStepOptionContext<T> extends DynamicControlConfiguration<T> {
    label?: string;
    initialValue?: T;
    requiresConfirm?: boolean;
    confirmLabel?: string;
}

export interface SimpleSelectMenuOption extends BaseSelectMenuOption {
    isMultiStep: false;
}

export interface MultiStepSelectMenuOption<T> extends BaseSelectMenuOption {
    isMultiStep: true,
    multiStepContext: MultiStepOptionContext<T>;
}

export type SelectMenuOption<T = any> = SimpleSelectMenuOption | MultiStepSelectMenuOption<T>;

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
