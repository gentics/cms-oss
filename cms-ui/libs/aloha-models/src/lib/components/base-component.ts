export interface AlohaComponent {
    id: number;
    isInstance: boolean;
    type: string;
    visible: boolean;
    disabled: boolean;
    touched: boolean;
    validationErrors: Record<string, any> | null;
    changeNotify?: (value: any) => void;
    touchNotify?: () => void;

    adoptParent: (container: HTMLElement) => void;

    init: () => void;
    destroy: () => void;

    show: () => void;
    hide: () => void;
    enable: () => void;
    disable: () => void;
    touch: () => void;
    untouched: () => void;

    focus: () => void;
    foreground: () => void;

    isVisible: () => boolean;
    isValid: () => boolean;

    getValue: () => any;
    setValue: (value: any) => void;

    triggerTouchNotification: () => void;
    triggerChangeNotification: () => void;
}

export enum AlohaCoreComponentNames {
    ATTRIBUTE_BUTTON = 'attribute-button',
    ATTRIBUTE_TOGGLE_BUTTON = 'attribute-toggle-button',
    BUTTON = 'button',
    CHECKBOX = 'checkbox',
    CONTEXT_BUTTON = 'context-button',
    INPUT = 'input',
    LINK_TARGET = 'link-target',
    SELECT_MENU = 'select-menu',
    SPLIT_BUTTON = 'split-button',
    TOGGLE_BUTTON = 'toggle-button',
    TOGGLE_SPLIT_BUTTON = 'toggle-split-button',
    TABLE_SIZE_SELECT = 'table-size-select',
}
