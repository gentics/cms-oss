import { AlohaCoreComponentNames } from './base-component';
import { AlohaButtonComponent } from './button';

export interface AlohaSplitButtonComponent extends Omit<AlohaButtonComponent, 'type'> {
    type: AlohaCoreComponentNames.SPLIT_BUTTON;

    secondaryLabel?: string;
    secondaryClick: () => void;
    secondaryVisible: boolean;

    setSecondaryVisible: (visible: boolean) => void;
}
