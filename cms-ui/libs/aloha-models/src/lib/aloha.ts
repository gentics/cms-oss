import { AlohaComponent } from './components';

export enum ScreenSize {
    DESKTOP = 'desktop',
    TABLET = 'tablet',
    MOBILE = 'mobile',
}

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
        format: FormatPluginSettings;
        [key: string]: any;
    };
    proxyUrl?: string;
    readonly?: boolean;
    sanitizeCharacters?: Record<string, string>;
    toolbar?: any;
}

export type  AlohaToolbarSettings = {
    [size in ScreenSize]: AlohaToolbarSizeSettings;
}

export interface AlohaToolbarSizeSettings {
    tabs: AlohaToolbarTabsSettings[];
}

export interface AlohaToolbarTabsSettings {
    id: string;
    label: string;

    icon?: string;

    showOn?: AlohaScopeSettings;
    components: AlohaComponentSetting[] | AlohaComponentSetting[][];
}

export interface AlohaScopeSettings {
    scope: string | string[];
}

export interface AlohaFullComponentSetting extends Partial<AlohaScopeSettings> {
    slot: string;
}

export type AlohaComponentSetting = string | AlohaFullComponentSetting;

export declare class AlohaRangeObject {
    constructor();

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

export interface AlohaContentRules {
    isAllowed(editable: JQuery, nodeName: string): boolean;
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

export interface FormatPluginSettings {
    config: string[];
    editables?: Record<string, string[]>;
    removeFormats?: string[];
}

export interface AlohaPlugin {
    readonly name: string;
    getEditableConfig: (editable: JQuery) => string[];
}

export interface AlohaLinkPlugin extends AlohaPlugin {
    anchorLinks?: boolean;
    config: string[];
    title: string;
    titleregex?: string | null;
    targetregex?: string | null;
    target: string;
    cssclassregex?: string | null;
    cssclass: string;
    objectTypeFilter: string[];
    onHrefChange?(): void;
    ignoreNextSelectionChangeEvent: boolean;
    hrefValue: string;
    flags: boolean;
    nsSel(): string;
    nsClass(): string;
    toggleLinkScope(show: boolean): void;
    findLinkMarkup(range: AlohaRangeObject): HTMLAnchorElement | null;
    findAllLinkMarkup(range: AlohaRangeObject): HTMLAnchorElement[];
    insertLink(extendToWord?: boolean): boolean | void;
    removeLink(terminateLinkScope?: boolean): void;
}

export interface AlohaFormatPlugin extends AlohaPlugin {
    removeFormat(): void;
}

export type AlohaListType = 'ul' | 'ol' | 'dl';

export interface AlohaListPlugin extends AlohaPlugin {
    transformList(listType: AlohaListType): void;
    createList(listType: AlohaListType, element: HTMLElement): void;
    prepareNewList(listType: AlohaListType): HTMLElement;
    indentList(): boolean;
    outdentList(): boolean;
    refreshSelection(): void;
    transformListToParagraph(element: HTMLElement, listType: AlohaListType): void;
}

export interface AlohaTablePlugin extends AlohaPlugin {
    activeTable?: ActiveAlohaTable;
    parameters: Record<string, string>;

    toggleHeaderStatus(table: ActiveAlohaTable, scope: 'row' | 'col'): void;
    createTable(columnCount: number, rowCount: number): void;
    updateWaiImage(): void;
    makeCaptionEditable(captionElem: JQuery<HTMLTableCaptionElement>, defaultText: string): void;
}

export interface AlohaTablePluginParameters {
    className: string;
    classSelectionRow: string;
    classSelectionColumn: string;
    classLeftUpperCorner: string;
    classTableWrapper: string;
    classCellSelected: string;
    waiRed: string;
    waiGreen: string;
    selectionArea: number;
}

export interface ActiveAlohaTable {
    obj?: JQuery<HTMLTableElement>;
    cells: any[];
    clickedColumnId?: number;
    clickedRowId?: number;
    columnsToSelect?: number[]
    hasFocus: boolean;
    isActive: boolean;
    mouseDownColIdx: boolean;
    numCols: number;
    numRows: number;
    rowsToSelect?: number[];
    selection: AlohaTableSelection;

    deleteTable(): void;
    deleteRows(): void;
    deleteColumns(): void;

    addRowBeforeSelection(): void;
    addRowAfterSelection(): void;
    addColumnsLeft(): void;
    addColumnsRight(): void;
}

export interface AlohaTableSelectionRectangle {
    top: number;
    right: number;
    bottom: number;
    left: number;
}

export interface AlohaTableSelectionRows {
    rows: number[];
}

export interface AlohaTableSelectionColumns {
    columns: number[];
}

export declare class AlohaTableSelection {
    constructor();

    currentRectangle: AlohaTableSelectionRectangle | AlohaTableSelectionRows | AlohaTableSelectionColumns;
    selectedCells?: HTMLTableCellElement[];
    selectedColumnIdxs?: number[];
    selectedRowIdxs?: number[];
    selectionType: 'cell' | 'row' | 'column';
    mergeCells(): void;
    splitCells(): void;
    cellsAreMergeable(): boolean;
    cellsAreSplitable(): boolean;
    isHeader(): boolean;
}

export interface AlohaUiPlugin extends AlohaPlugin {
    adoptInto: (slot: string, component: AlohaComponent) => void;
    unadopt: (slot: string) => void;
    getToolbarSettings: () => AlohaToolbarSettings;
}

export interface AlohaDOM {
    /**
     * Apply the given markup additively to the given range. The given rangeObject will be modified if necessary
     * @param rangeObject range to which the markup shall be added
     * @param markup markup to be applied as jQuery object
     * @param nesting true when nesting of the added markup is allowed, false if not (default: false)
     */
    addMarkup(rangeObject: AlohaRangeObject, markup: any, nesting?: boolean): void;
    /**
     * Remove the given DOM object from the DOM and modify the given range to reflect the user expected range after the object was removed
     * @param object DOM object to remove
     * @param range range which eventually be modified
     * @param preserveContent true if the contents of the removed DOM object shall be preserved, false if not (default: false)
     */
    removeFromDOM(object: any, range: AlohaRangeObject, preserveContent): void;
    /**
     * Remove the given markup from the given range. The given rangeObject will be modified if necessary
     * @param rangeObject range from which the markup shall be removed
     * @param markup markup to be removed as jQuery object
     * @param limit Limiting node(s) as jQuery object
     * @param removeNonEditables Whether to remove nodes which are not content editable (default: true)
     */
    removeMarkup(rangeObject: AlohaRangeObject, markup: any, limit: any, removeNonEditables?: boolean): void;
    getEditingHostOf(obj: any): any;

    setCursorInto(element: HTMLElement): void;
    selectDomNode(element: HTMLElement): void;
}

export interface AlohaBlockManager {
    getBlock(idOrElement: string | HTMLElement): AlohaAbstractBlock;
}

export interface AlohaAbstractBlock {
    title: string;
    id: string;
    $element: JQuery;
    destroy(force: boolean): void;
    unblock(): void;
    free(): void;
    isDraggable(): boolean;
    activate(eventTarget: HTMLElement | JQuery, event?: Event): void;
    deactivate(): void;
    isActive(): boolean;
    update($element: JQuery, postProcessFn: () => void): void;
}

export interface AlohaEditable {
    smartContentChange(event: any): false | void;
    obj: JQuery;
    activate(): void;
}

export interface AlohaPubSub {
    sub: (eventName: string, handler: (eventData: any) => void) => void;
    pub: (eventName: string, eventData: any) => void;
    unsub: (eventName: string, handler: (eventData: any) => void) => void;
}

export interface AlohaLinkChangeEvent {
    href: string;
    element: JQuery<HTMLAnchorElement>;
}

export interface AlohaLinkInsertEvent {
    range: AlohaRangeObject;
    elements: JQuery<HTMLAnchorElement>;
}

export interface AlohaLinkRemoveEvent {
    range: AlohaRangeObject;
    text: string;
}

export interface AlohaScopes {
    registerScope: (scope: string, dependencies?: string[]) => boolean;
    removeScope: (scope: string, force?: boolean) => boolean;

    enterScope: (scope: string, temp?: boolean) => void;
    leaveScope: (scope: string) => void;
    setScope: (scope: string) => void;

    isActiveScope: (scope: string) => boolean;
    getActiveScopes: (withResolved?: boolean) => string[];
}

export interface AlohaScopeChangeEvent {
    previousScopes: string[];
    previousScopeList: string[];
    activeScopes: string[];
    activeScopeList: string[];
}

export interface AlohaSetEditableActiveEvent {
    editable: AlohaEditable | null;
}

export interface AlohaEditableActivatedEvent {
    old?: AlohaEditable;
    editable: AlohaEditable;
}
