export interface ExtractedLink {
    type: 'PAGE' | 'FILE';
    nodeId: string | number;
    pageId: string | number;
    langCode?: string;
    displayText: string;
    target?: string;
}
