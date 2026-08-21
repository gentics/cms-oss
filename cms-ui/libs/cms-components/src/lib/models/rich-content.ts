/**
 * Enum of all available rich-content elements.
 * Bit of overhead with only one type, but future proofing just in case.
 */
export enum RichContentType {
    LINK = 'LINK',
}

export interface RichContentElement {
    type: RichContentType;
}

export enum RichContentLinkType {
    PAGE = 'PAGE',
    FILE = 'FILE',
    URL = 'URL',
}

export type RichContent = RichContentLink;

export interface RichContentLink extends RichContentElement {
    type: RichContentType.LINK;

    linkType: RichContentLinkType;
    nodeId?: string | number;
    itemId?: string | number;
    url?: string;
    displayText: string;
    target?: string;
}

export const CLASS_RICH_ELEMENT = 'rich-element';
export const CLASS_ITEM_LINK = 'item-link';

export const ATTR_CONTENT_TYPE = 'data-rich-content-type';
export const ATTR_LINK_TYPE = 'data-link-type';
export const ATTR_NODE_ID = 'data-node-id';
export const ATTR_ITEM_ID = 'data-item-id';
export const ATTR_URL = 'data-url';
export const ATTR_TARGET = 'data-link-target';

export const LINK_DEFAULT_DISPLAY_VALUE = 'Link';
