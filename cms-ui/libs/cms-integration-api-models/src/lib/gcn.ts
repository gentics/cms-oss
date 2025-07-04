/* eslint-disable @typescript-eslint/naming-convention */
import { AlohaPlugin, AlohaRangeObject } from '@gentics/aloha-models';
import { ExternalLink, LinkCheckerCheckResponse } from '@gentics/cms-models';

export interface GCNPluginSettings {
    blocks?: GCNPluginBlockDefintion[];
    buildRootTimestamp?: string;
    constructCategories?: GCNPluginCategoryDefinition[];
    copy_tags?: boolean;
    description?: string;
    devtools?: boolean;
    config?: {
        tagtypeWhitelist?: string[];
    };
    editables?: {
        [cssQuery: string]: {
            tagtypeWhitelist?: string[];
        };
    };
    editableNode?: GCNPluginEditableNodeDefinition[];
    enabled?: boolean;
    fileName?: string;
    folderId?: number;
    forms?: boolean;
    gcnLibVersion?: string;
    id?: number;
    isPublicationPermitted?: boolean;
    languageMenu?: GCNPluginLanguageDefinition[];
    languageid?: string;
    lastAction?: string;
    links?: string;
    magiclinkconstruct?: number;
    metaeditables?: any[];
    modified?: boolean;
    name?: string;
    nodeFolderId?: string;
    nodeId?: string;
    online?: boolean;
    pagelanguage?: string;
    portletapp_prefix?: string;
    priority?: number;
    renderMessages?: any[];
    sid?: string;
    stag_prefix?: string;
    tags?: GCNPluginTagDefinition[];
    templateId?: number;
    webappPrefix?: string;
}

export interface GCNPluginBlockDefintion {
    constructid: string;
    editdo: string;
    icontitle: string;
    iconurl: string;
    id: string;
    tagid: string;
    tagname: string;
}

export interface GCNPluginCategoryDefinition {
    id: number;
    name: string;
    constructs: {
        id: string;
        icon: string;
        keyword: string;
        name: string;
    }[];
}

export interface GCNPluginEditableNodeDefinition {
    id: string;
    tagname: string;
    partname: string;
}

export interface GCNPluginLanguageDefinition {
    name: string;
    code: string;
    id: string;
}

export interface GCNPluginTagDefinition {
    element: string;
    onlyeditables: boolean;
    tagname: string;
    editables?: GCNPluginEditableDefinition[];
    editablesNode?: GCNPluginEditableDefinition[];
}

export interface GCNPluginEditableDefinition {
    _gcnContainedBlocks?: GCNPluginTagDefinition[];
    element: string;
    partname: string;
    tagname: string;
}

export interface GCNLinkBrowserPluginSettings {
    enabled?: boolean;
    objectTypeFilter?: string[];
}

export interface GCNLinkCheckerPluginSettings {
    absoluteBase?: string;
    defaultProtocol?: string;
    enabled?: boolean;
    relativeBase?: string;
    livecheck?: boolean;
    delay?: number;
    tagtypeWhitelist?: string[];
}

export interface GCNAlohaPlugin extends AlohaPlugin {
    settings: GCNPluginSettings;
    createTag(
        constructId: number,
        async?: boolean,
        successCallback?: (html: string, tag: any, data: any) => any,
        range?: Range | AlohaRangeObject,
    ): void;
    handleBlock(
        data: any,
        insert: boolean,
        onInsert: () => void,
        content?: any,
        range?: Range | AlohaRangeObject,
    ): void;
    openTagFill(tagId: string | number, pageId: string | number, withDelete?: boolean): void;
    insertNewTag(
        constructId: number,
        range: Range | AlohaRangeObject,
    ): Promise<any>;
}

export interface GCNTags {
    insert(
        data: any,
        callback?: (data: any) => void,
        range?: Range | AlohaRangeObject,
    ): JQuery;
    decorate(tag: any, data: any, callback?: () => void): void;
}

export interface GCNLinkCheckerAlohaPluigin extends AlohaPlugin {
    settings: GCNLinkCheckerPluginSettings;

    brokenLinks: HTMLElement[];
    validLinks: HTMLElement[];
    uncheckedLinks: HTMLElement[];

    /** Sets up the link-checker pluign with the initial data from the backend. */
    initializeBrokenLinks: (links: ExternalLink[]) => HTMLElement[];
    /** Updates all elements in the arrays from the DOM. */
    refreshLinksFromDom: () => void;

    /**
     * Updates (or adds if not present yet) the element to the provided status.
     * If no status is provided, the link will be marked as unchecked.
     */
    updateLinkStatus: (element: HTMLElement, res?: ExternalLink | LinkCheckerCheckResponse) => void;
    /**
     * Removes the link from the link-checker.
     * @param element The element to remove the link element from
     * @returns If the link has been successfully removed.
     */
    removeLink: (element: HTMLElement) => boolean;

    /* Toolbar interaction */
    /** Selects the specified link/tag */
    selectLinkElement: (element: HTMLElement) => void;
    /** Opens the link-modal or tag-fill for the element */
    editLink: (element: HTMLElement) => Promise<void>;
    /** Deletes the entire tag/link from DOM and from the page. */
    deleteLink: (element: HTMLElement) => Promise<void>;
}
