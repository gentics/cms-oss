import { DynamicDropdownConfiguration, DynamicFormModalConfiguration } from '@gentics/cms-integration-api-models';
import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface AlohaContextButtonComponent<T> extends Omit<AlohaButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.CONTEXT_BUTTON;

    value?: T;
    contextType: 'dropdown' | 'modal';
    context: DynamicDropdownConfiguration<T>
    | DynamicFormModalConfiguration<T>
    | (() => DynamicDropdownConfiguration<T> | DynamicFormModalConfiguration<T>);

    closeContext: () => void;
    isOpen: () => boolean;
    contextResolve: (value: T) => void;
    contextReject: (error: any) => void;
}
