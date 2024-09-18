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

export interface RichContentLink extends RichContentElement {
    type: RichContentType.LINK;

    linkType: RichContentLinkType;
    nodeId?: string | number;
    itemId?: string | number;
    langCode?: string;
    url?: string;
    displayText: string;
    target?: string;
}
