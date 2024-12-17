import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaInputComponent extends Omit<AlohaComponent, 'type'> {
    type: AlohaCoreComponentNames.INPUT;

    value?: string;
    label?: string;
    hint?: string;
    inputType?: string;

    setLabel: (label: string) => void;
    setHint: (hint: string) => void;
}
