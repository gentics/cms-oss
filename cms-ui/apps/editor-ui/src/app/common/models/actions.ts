export const PRODUCT_TOOL_KEYS = [
    'linkchecker',
    'formgenerator',
];

export const ADMIN_TOOL_KEY = 'administration';

export enum ActionButtonIconType {
    URL = 'url',
    FONT = 'font',
    TEXT = 'text',
}

export enum ActionButtonType {
    ACTION = 'action',
    TOOL = 'tool',
}

interface BaseActionButton {
    /** ID of the button */
    id: string;
    /** What type of button it is */
    type: ActionButtonType;

    /** Already translated name which should be printed to the user. */
    label?: string;
    /** Translation string which still needs to be translated before being shown. */
    i18nLabel?: string;

    /** The type of icon to display */
    iconType: ActionButtonIconType;
    /** The icon value if it's needed */
    icon?: string;
}

interface ToolButton extends BaseActionButton {
    type: ActionButtonType.TOOL;
    toolKey: string;
    toolLink: string;
    newTab: boolean;
}

interface BasicActionButton extends BaseActionButton {
    type: ActionButtonType.ACTION;
}

export type ActionButton = ToolButton | BasicActionButton;

export interface ActionButtonGroup {
    i18nLabel: string;
    buttons: ActionButton[][];
}
