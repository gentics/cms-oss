/* eslint-disable @typescript-eslint/naming-convention */
import { AlohaPlugin } from '@gentics/aloha-models';
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
}

export interface GCNAlohaPlugin extends AlohaPlugin {
    settings: GCNPluginSettings;
    createTag(constructId: number, async?: boolean, successCallback?: (html: string, tag: any, data: any) => any): void;
    handleBlock(data: any, insert: boolean, onInsert: () => void, content?: any): void;
    openTagFill(tagId: string | number, pageId: string | number, withDelete?: boolean): void;
}

export interface GCNTags {
    insert(data: any, callback?: (data: any) => void): JQuery;
    decorate(tag: any, data: any, callback?: () => void): void;
}

export interface GCNLinkCheckerAlohaPluigin extends AlohaPlugin {
    settings: GCNLinkCheckerPluginSettings;
    brokenLinks: HTMLElement[];
    validLinks: HTMLElement[];
    uncheckedLinks: HTMLElement[];

    clearBrokenLinks: () => void;
    initializeBrokenLinks: (links: ExternalLink[]) => HTMLElement[];
    refreshLinksFromDom: () => void;

    addLink: (element: HTMLElement, res?: LinkCheckerCheckResponse) => void;
    addUncheckedLink: (element: HTMLElement) => boolean;
    addValidLink: (element: HTMLElement) => boolean;
    addBrokenLink: (element: HTMLElement) => boolean;

    updateLinkStatus: (element: HTMLElement, res?: LinkCheckerCheckResponse) => void;
    removeLink: (element: HTMLElement) => boolean;

    selectLinkElement: (element: HTMLElement) => void;
    editLink: (element: HTMLElement) => Promise<void>;
    deleteLink: (element: HTMLElement) => Promise<void>;

    deleteTag: (element: HTMLElement | JQuery) => void;
}
