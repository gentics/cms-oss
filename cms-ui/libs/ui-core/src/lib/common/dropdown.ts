export type DropdownAlignment = 'left' | 'right';
export type DropdownWidth = 'contents' | 'trigger' | 'expand' | number;

export const DROPDOWN_FOCUSABLE_ITEMS_SELECTOR = `gtx-dropdown-item, a[href], area[href], input:not([disabled]), select:not([disabled]),
    textarea:not([disabled]), button:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]`;
