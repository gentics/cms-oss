export interface AlohaSettings {
    base: string;
    baseUrl: string;
    bundles: Record<string, string>;
    contentHandler: Record<string, string[]>;
    i18n: {
        current: string;
    };
    loadedPlugins: string[];
    locale: string;
    plugins: {
        block?: BlockPluginSettings;
        gcn?: GcnPluginSettings;
        'gcn-linkbrowser'?: GcnLinkBrowserPluginSettings;
        gcnlinkchecker?: GcnLinkCheckerPluginSettings;
        [key: string]: any;
    };
    proxyUrl?: string;
    readonly?: boolean;
    sanitizeCharacters?: Record<string, string>;
    toolbar?: any;
}

export interface BlockPluginSettings {
    defaults?: any;
    sidebarAttributeEditor?: boolean;
    dragdrop?: boolean;
    config?: BlockPluginEditableConfig;
    editables?: Record<string, BlockPluginEditableConfig>;
    rootTags?: string[];
}

export interface BlockPluginEditableConfig {
    toggleDragdrop?: boolean;
    dropzones?: string[];
    [key: string]: any;
}

export interface BlockPluginGlobalConfig extends BlockPluginEditableConfig {
    toggleDragdropGlobal?: boolean;
}

export interface GcnPluginSettings {
    blocks?: GcnPluginBlockDefintion[];
    buildRootTimestamp?: string;
    constructCategories?: GcnPluginCategoryDefinition[];
    copy_tags?: boolean;
    description?: string;
    devtools?: boolean;
    editableNode?: GcnPluginEditableNodeDefinition[];
    enabled?: boolean;
    fileName?: string;
    folderId?: number;
    forms?: boolean;
    gcnLibVersion?: string;
    id?: number;
    isPublicationPermitted?: boolean;
    languageMenu?: GcnPluginLanguageDefinition[];
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
    tags?: GcnPluginTagDefinition[];
    templateId?: number;
    webappPrefix?: string;
}

export interface GcnPluginBlockDefintion {
    constructid: string;
    editdo: string;
    icontitle: string;
    iconurl: string;
    id: string;
    tagid: string;
    tagname: string;
}

export interface GcnPluginCategoryDefinition {
    id: number;
    name: string;
    constructs: {
        id: string;
        icon: string;
        keyword: string;
        name: string;
    }[];
}

export interface GcnPluginEditableNodeDefinition {
    id: string;
    tagname: string;
    partname: string;
}

export interface GcnPluginLanguageDefinition {
    name: string;
    code: string;
    id: string;
}

export interface GcnPluginTagDefinition {
    element: string;
    onlyeditables: boolean;
    tagname: string;
    editables?: GcnPluginEditableDefinition[];
    editablesNode?: GcnPluginEditableDefinition[];
}

export interface GcnPluginEditableDefinition {
    _gcnContainedBlocks?: GcnPluginTagDefinition[];
    element: string;
    partname: string;
    tagname: string;
}

export interface GcnLinkBrowserPluginSettings {
    enabled?: boolean;
    objectTypeFilter?: string[];
}

export interface GcnLinkCheckerPluginSettings {
    absoluteBase?: string;
    defaultProtocol?: string;
    enabled?: boolean;
    relativeBase?: string;
}
