import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaSelectOption {
    id: string;
    label: string;
    icon?: string;
    iconHollow?: boolean;
}

export interface AlohaSelectComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.SELECT;

    label?: string;
    value: string | string[] | null;
    options: AlohaSelectOption[];
    multiple: boolean;
    clearable: boolean;
    placeholder?: string;

    setLabel: (label: string) => void;
    setOptions: (options: AlohaSelectOption[]) => void;
    setMultiple: (multiple: boolean) => void;
    setClearable: (clearable: boolean) => void;
    setPlaceholder: (placeholder: string) => void;
}
