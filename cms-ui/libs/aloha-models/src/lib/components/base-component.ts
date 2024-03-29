export interface AlohaComponent {
    id: number;
    isInstance: boolean;
    type: string;
    name: string;
    renderContext: 'dropdown' | 'modal' | null;
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
    COLOR_PICKER = 'color-picker',
    CONTEXT_BUTTON = 'context-button',
    CONTEXT_TOGGLE_BUTTON = 'context-toggle-button',
    DATE_TIME_PICKER = 'date-time-picker',
    IFRAME = 'iframe',
    INPUT = 'input',
    LINK_TARGET = 'link-target',
    SELECT_MENU = 'select-menu',
    SPLIT_BUTTON = 'split-button',
    SYMBOL_GRID = 'symbol-grid',
    SYMBOL_SEARCH_GRID = 'symbol-search-grid',
    TOGGLE_BUTTON = 'toggle-button',
    TOGGLE_SPLIT_BUTTON = 'toggle-split-button',
    TABLE_SIZE_SELECT = 'table-size-select',
}
