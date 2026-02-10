import { AlohaCoreComponentNames } from '@gentics/aloha-models';
import type { createRange, KEYCLOAK_LOGIN, selectRange, selectText, updateAlohaRange } from '@gentics/e2e-utils';
export interface HelperWindow extends Window {
    createRange: typeof createRange;
    selectRange: typeof selectRange;
    selectText: typeof selectText;
    updateAlohaRange: typeof updateAlohaRange;
}

export const AUTH = {
    /** @deprecated Use a dedicated user from your test instead. */
    admin: {
        username: 'node',
        password: 'node',
    },
    /** @deprecated Use the {@link KEYCLOAK_LOGIN} instead */
    keycloak: {
        username: 'node',
        password: 'node',
    },
};

export const ACTION_FORMAT_BOLD = 'bold';
export const ACTION_FORMAT_ITALIC = 'italic';
export const ACTION_FORMAT_UNDERLINE = 'underline';
export const ACTION_FORMAT_STRIKETHROUGH = 'strikethrough';
export const ACTION_FORMAT_SUBSCRIPT = 'subscript';
export const ACTION_FORMAT_SUPERSCRIPT = 'superscript';
export const ACTION_FORMAT_CODE = 'code';
export const ACTION_FORMAT_QUOTE = 'quote';
export const ACTION_FORMAT_CITE = 'cite';
export const ACTION_FORMAT_ABBR = 'formatAbbr';
export const ACTION_FORMAT_DELETED = 'deleted';
export const ACTION_FORMAT_INSERTED = 'inserted';
export const ACTION_FORMAT_STRONG = 'strong';
export const ACTION_FORMAT_EMPHASIZE = 'emphasize';

export const ACTION_REMOVE_FORMAT = 'removeFormat';

export const FORMAT_BOLD = 'b';
export const FORMAT_ITALIC = 'i';
export const FORMAT_UNDERLINE = 'u';
export const FORMAT_STRIKETHROUGH = 's';
export const FORMAT_SUBSCRIPT = 'sub';
export const FORMAT_SUPERSCRIPT = 'sup';
export const FORMAT_CODE = 'code';
export const FORMAT_QUOTE = 'q';
export const FORMAT_CITE = 'cite';
export const FORMAT_BLOCK_QUOTE = 'blockquote';
export const FORMAT_ABBR = 'abbr';
export const FORMAT_DELETED = 'del';
export const FORMAT_INSERTED = 'ins';
export const FORMAT_STRONG = 'strong';
export const FORMAT_EMPHASIZE = 'em';

export const AUTH_MESH = 'mesh';

export const ACTION_SIMPLE_FORMAT_MAPPING: Record<string, string> = {
    [ACTION_FORMAT_BOLD]: FORMAT_BOLD,
    [ACTION_FORMAT_ITALIC]: FORMAT_ITALIC,
    [ACTION_FORMAT_UNDERLINE]: FORMAT_UNDERLINE,
    [ACTION_FORMAT_STRIKETHROUGH]: FORMAT_STRIKETHROUGH,
    [ACTION_FORMAT_SUBSCRIPT]: FORMAT_SUBSCRIPT,
    [ACTION_FORMAT_SUPERSCRIPT]: FORMAT_SUPERSCRIPT,
    [ACTION_FORMAT_CODE]: FORMAT_CODE,
    [ACTION_FORMAT_CITE]: FORMAT_CITE,
    [ACTION_FORMAT_QUOTE]: FORMAT_QUOTE,
    [ACTION_FORMAT_DELETED]: FORMAT_DELETED,
    [ACTION_FORMAT_INSERTED]: FORMAT_INSERTED,
    [ACTION_FORMAT_STRONG]: FORMAT_STRONG,
    [ACTION_FORMAT_EMPHASIZE]: FORMAT_EMPHASIZE,
};
export const ACTION_SIMPLE_FORMAT_KEYS = Object.keys(ACTION_SIMPLE_FORMAT_MAPPING);

export const RENDERABLE_ALOHA_COMPONENTS: Record<string, string> = [
    AlohaCoreComponentNames.ATTRIBUTE_BUTTON,
    AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON,
    AlohaCoreComponentNames.BUTTON,
    AlohaCoreComponentNames.CHECKBOX,
    AlohaCoreComponentNames.COLOR_PICKER,
    AlohaCoreComponentNames.CONTEXT_BUTTON,
    AlohaCoreComponentNames.CONTEXT_TOGGLE_BUTTON,
    AlohaCoreComponentNames.DATE_TIME_PICKER,
    AlohaCoreComponentNames.IFRAME,
    AlohaCoreComponentNames.INPUT,
    AlohaCoreComponentNames.LINK_TARGET,
    AlohaCoreComponentNames.SELECT,
    AlohaCoreComponentNames.SELECT_MENU,
    AlohaCoreComponentNames.SPLIT_BUTTON,
    AlohaCoreComponentNames.SYMBOL_GRID,
    AlohaCoreComponentNames.SYMBOL_SEARCH_GRID,
    AlohaCoreComponentNames.TABLE_SIZE_SELECT,
    AlohaCoreComponentNames.TOGGLE_BUTTON,
    AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON,
].reduce((acc, name) => {
    acc[name] = `gtx-aloha-${name}-renderer`;
    return acc;
}, {});
