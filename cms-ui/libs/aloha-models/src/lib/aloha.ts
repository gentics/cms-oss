import { Item } from '@gentics/cms-models';

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
        format: FormatPluginSettings;
        [key: string]: any;
    };
    proxyUrl?: string;
    readonly?: boolean;
    sanitizeCharacters?: Record<string, string>;
    toolbar?: any;
}

export interface AlohaRangeObject {
    commonAncestorContainer: HTMLElement;
    endContainer: HTMLElement;
    endOffset: number;
    inselection: boolean;
    limitObject: HTMLElement;
    markupEffectiveAtStart: HTMLElement[];
    selectionTree?: any;
    splitObject: HTMLElement;
    startContainer: HTMLElement;
    startOffset: number;
    unmodifiableMarkupAtStart: HTMLElement[];
    select(): void;
    isCollapsed(): boolean;
    findMarkup(comparator: (this: HTMLElement) => boolean, limit: JQuery, atEnd?: boolean): any | false;
}

export interface AlohaContextChangeEvent {
    event: Event;
    range: AlohaRangeObject;
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

export interface FormatPluginSettings {
    config: string[];
    editables?: Record<string, string[]>;
}

export interface AlohaUiComponent {
    id: number;
    isInstance: boolean;
    container: any | null;
    type: string | null;
    visible: boolean;
    show(showOptions: any): void;
    hide(): void;
    focus(): void;
    foreground(): void;
    enable(enableOptions: any): void;
    disable(): void;
}

export interface AlohaTextUiComponent extends AlohaUiComponent {
    setValue(value: string): void;
    getValue(): string;
    updateValue(value: string): void;
    element: JQuery<HTMLElement>;
}

export interface AlohaAttributeFieldUiComponent extends AlohaUiComponent  {
    addAdditionalTargetObject(target: HTMLElement): void;
    addListener(eventName: string, handler: (data: any) => void): void;
    disableInput(): void;
    enableInput(): void;
    finishEditing(selectElement: boolean): void;
    getInputElem(): HTMLElement | null;
    getInputId(): string;
    getInputJQuery(): JQuery<HTMLElement>;
    getItem(): Item;
    getTargetObject(): JQuery<HTMLElement>;
    getValue(allowModification: boolean): void;
    hasInputElem(): true;
    /**
     * Sets an attribute optionally based on a regex on reference
     * @param attribute The Attribute name which should be set. Ex. "lang"
     * @param value The value to set. Ex. "de-AT"
     * @param regex The regex when the attribute should be set. The regex is applied to the value of refernece.
     * @param reference The value for the regex.
     */
    setAttribute(attribute: string, value: string, regex: string, reference: string): void;
    setItem(item: Item): void;
    setObjectTypeFilter(filter: string[]): void;
    setValue(value: string): void;
}

export interface AlohaPlugin {
    readonly name: string;
}

export interface AlohaLinkPlugin extends AlohaPlugin {
    anchorField?: AlohaTextUiComponent & {
        clear(): void;
    },
    anchorLinks?: boolean;
    hrefField: AlohaAttributeFieldUiComponent;
    config: string[];
    title: string;
    titleregex?: string | null;
    targetregex?: string | null;
    target: string;
    cssclassregex?: string | null;
    cssclass: string;
    objectTypeFilter: string[];
    onHrefChange?: () => void;
    ignoreNextSelectionChangeEvent: boolean;
    hrefValue: string;
    flags: boolean;
    nsSel: () => string;
    nsClass: () => string;
    toggleLinkScope: (show: boolean) => void;
    findLinkMarkup: (range: AlohaRangeObject) => HTMLAnchorElement | null;
    findAllLinkMarkup: (range: AlohaRangeObject) => HTMLAnchorElement[];
    insertLink: (extendToWord?: boolean) => boolean | void;
    removeLink: (terminateLinkScope?: boolean) => void;
}

export interface AlohaDOM {
    /**
     * Apply the given markup additively to the given range. The given rangeObject will be modified if necessary
     * @param rangeObject range to which the markup shall be added
     * @param markup markup to be applied as jQuery object
     * @param nesting true when nesting of the added markup is allowed, false if not (default: false)
     */
    addMarkup: (rangeObject: AlohaRangeObject, markup: any, nesting?: boolean) => void;
    /**
     * Remove the given DOM object from the DOM and modify the given range to reflect the user expected range after the object was removed
     * @param object DOM object to remove
     * @param range range which eventually be modified
     * @param preserveContent true if the contents of the removed DOM object shall be preserved, false if not (default: false)
     */
    removeFromDOM: (object: any, range: AlohaRangeObject, preserveContent) => void;
    /**
     * Remove the given markup from the given range. The given rangeObject will be modified if necessary
     * @param rangeObject range from which the markup shall be removed
     * @param markup markup to be removed as jQuery object
     * @param limit Limiting node(s) as jQuery object
     * @param removeNonEditables Whether to remove nodes which are not content editable (default: true)
     */
    removeMarkup: (rangeObject: AlohaRangeObject, markup: any, limit: any, removeNonEditables?: boolean) => void;
}

export interface AlohaEditable {
    smartContentChange: (event: any) => false | void;
    obj: JQuery;
}

export interface AlohaPubSub {
    sub: (eventName: string, handler: (eventData: any) => void) => void;
    pub: (eventName: string, eventData: any) => void;
    unsub: (eventName: string, handler: (eventData: any) => void) => void;
}
