export const TAG_ALIASES: Record<string, string> = {
    /* eslint-disable @typescript-eslint/naming-convention */
    STRONG: 'B',
    EM: 'I',
    /* eslint-enable @typescript-eslint/naming-convention */
}

export const COMMAND_STYLE_BOLD = 'bold';
export const COMMAND_STYLE_ITALIC = 'italic';
export const COMMAND_STYLE_UNDERLINE = 'underline';
export const COMMAND_STYLE_STRIKE_THROUGH = 'strikethrough';
export const COMMAND_STYLE_CODE = 'code';
export const COMMAND_STYLE_QUOTE = 'quote';
export const COMMAND_STYLE_CITATION = 'citation';
export const COMMAND_STYLE_ABBREVIATION = 'abbreviation';
export const COMMAND_STYLE_SUBSCRIPT = 'subscript';
export const COMMAND_STYLE_SUPERSCRIPT = 'superscript';

export const COMMAND_SPECIAL_STYLE_REMOVE_FORMAT = 'removeFormat';

export const COMMAND_LIST_UNORDERED = 'list_unordered';
export const COMMAND_LIST_ORDERED = 'list_ordered';
export const COMMAND_LIST_DESCRIPTION = 'list_description';

export const COMMAND_TYPOGRAPHY_PARAGRAPH = 'paragraph';
export const COMMAND_TYPOGRAPHY_HEADING1 = 'h1';
export const COMMAND_TYPOGRAPHY_HEADING2 = 'h2';
export const COMMAND_TYPOGRAPHY_HEADING3 = 'h3';
export const COMMAND_TYPOGRAPHY_HEADING4 = 'h4';
export const COMMAND_TYPOGRAPHY_HEADING5 = 'h5';
export const COMMAND_TYPOGRAPHY_HEADING6 = 'h6';
export const COMMAND_TYPOGRAPHY_PREFORMATTED = 'pre';

export const COMMAND_LINK = 'link';
export const COMMAND_TABLE = 'table';

export const TEXT_NODE_NAME = '#text';

export const LINK_NODE_NAME = 'A';
export const INLINE_LINK_CLASS = 'aloha-link-text';
export const INLINE_LINK_URL_ATTRIBUTE = 'data-gentics-gcn-url';
export const INLINE_LINK_ANCHOR_ATTRIBUTE = 'data-gentics-gcn-anchor';
export const INLINE_LINK_OBJECT_ID_ATTRIBUTE = 'data-gentics-aloha-object-id';
export const INLINE_LINK_OBJECT_NODE_ATTRIBUTE = 'data-gcn-channelid';
export const INLINE_LINK_LANGUAGE_ATTRIBUTE = 'hreflang';
export const INLINE_LINK_TITLE_ATTRIBUTE = 'title';
export const INLINE_LINK_HREF_ATTRIBUTE = 'href';
export const INLINE_LINK_TARGET_ATTRIBUTE = 'target';
export const LINK_TARGET_NEW_TAB = '_blank';

export const TABLE_NODE_NAME = 'TABLE';
export const TABLE_CLASS = 'aloha-table';
export const TABLE_CELL_NODE_NAME = 'TD';
export const TABLE_CELL_HEADER_NODE_NAME = 'TH';
export const TABLE_ROW_NODE_NAME = 'TR';
export const TABLE_CAPTION_NODE_NAME = 'CAPTION';
export const TABLE_SUMMARY_ATTRIBUTE = 'summary';

export const SCOPE_TEXT = 'Aloha.continuoustext';
export const SCOPE_LINK = 'link';
export const SCOPE_CITATION = 'cite';
export const SCOPE_IMAGE = 'image';
export const SCOPE_ABBRIVIATION = 'abbr';
export const SCOPE_TABLE_CELL = 'table.cell';
export const SCOPE_TABLE_COLUMN = 'table.column';
export const SCOPE_TABLE_ROW = 'table.row';

export const NODE_NAME_TO_COMMAND: Record<string, string> = {
    /* eslint-disable @typescript-eslint/naming-convention */
    B: COMMAND_STYLE_BOLD,
    I: COMMAND_STYLE_ITALIC,
    U: COMMAND_STYLE_UNDERLINE,
    S: COMMAND_STYLE_STRIKE_THROUGH,
    CODE: COMMAND_STYLE_CODE,
    Q: COMMAND_STYLE_QUOTE,
    CITE: COMMAND_STYLE_CITATION,
    ABBR: COMMAND_STYLE_ABBREVIATION,
    SUB: COMMAND_STYLE_SUBSCRIPT,
    SUP: COMMAND_STYLE_SUPERSCRIPT,

    UL: COMMAND_LIST_UNORDERED,
    OL: COMMAND_LIST_ORDERED,
    DL: COMMAND_LIST_DESCRIPTION,

    P: COMMAND_TYPOGRAPHY_PARAGRAPH,
    [TEXT_NODE_NAME]: COMMAND_TYPOGRAPHY_PARAGRAPH,
    H1: COMMAND_TYPOGRAPHY_HEADING1,
    H2: COMMAND_TYPOGRAPHY_HEADING2,
    H3: COMMAND_TYPOGRAPHY_HEADING3,
    H4: COMMAND_TYPOGRAPHY_HEADING4,
    H5: COMMAND_TYPOGRAPHY_HEADING5,
    H6: COMMAND_TYPOGRAPHY_HEADING6,
    PRE: COMMAND_TYPOGRAPHY_PREFORMATTED,

    A: COMMAND_LINK,
    TABLE: COMMAND_TABLE,
    /* eslint-enable @typescript-eslint/naming-convention */
};

export const SPECIAL_NAME_TO_COMMAND: Record<string, string> = {
    removeFormat: COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
};

export const COMMAND_TO_NODE_NAME: Record<string, string> = Object.entries(NODE_NAME_TO_COMMAND).reduce((acc, [name, command]) => {
    acc[command] = name;
    return acc;
}, {});

export const STYLE_COMMANDS = [
    COMMAND_STYLE_BOLD,
    COMMAND_STYLE_ITALIC,
    COMMAND_STYLE_UNDERLINE,
    COMMAND_STYLE_STRIKE_THROUGH,
    COMMAND_STYLE_CODE,
    COMMAND_STYLE_QUOTE,
    COMMAND_STYLE_CITATION,
    COMMAND_STYLE_ABBREVIATION,
    COMMAND_STYLE_SUBSCRIPT,
    COMMAND_STYLE_SUPERSCRIPT,
];

export const SPECIAL_STYLE_COMMANDS = [
    COMMAND_SPECIAL_STYLE_REMOVE_FORMAT,
];

export const LIST_COMMANDS = [
    COMMAND_LIST_UNORDERED,
    COMMAND_LIST_ORDERED,
    COMMAND_LIST_DESCRIPTION,
];

export const TYPOGRAPHY_COMMANDS = [
    COMMAND_TYPOGRAPHY_PARAGRAPH,
    COMMAND_TYPOGRAPHY_HEADING1,
    COMMAND_TYPOGRAPHY_HEADING2,
    COMMAND_TYPOGRAPHY_HEADING3,
    COMMAND_TYPOGRAPHY_HEADING4,
    COMMAND_TYPOGRAPHY_HEADING5,
    COMMAND_TYPOGRAPHY_HEADING6,
    COMMAND_TYPOGRAPHY_PREFORMATTED,
];

export const DEFAULT_COMMANDS = [
    ...STYLE_COMMANDS,
    ...SPECIAL_STYLE_COMMANDS,
    ...LIST_COMMANDS,
    ...TYPOGRAPHY_COMMANDS,
];
