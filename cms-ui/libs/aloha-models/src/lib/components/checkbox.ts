import { AlohaComponent, AlohaCoreComponentNames } from './base-component';

export interface AlohaCheckboxComponent extends AlohaComponent {
    type: AlohaCoreComponentNames.CHECKBOX;

    checked: boolean;
    label: string;
}
