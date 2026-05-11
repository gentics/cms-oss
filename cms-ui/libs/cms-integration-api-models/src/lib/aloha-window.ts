import { AlohaEditable, AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { GcmsUiBridge } from './gcms-ui-bridge';
import { GCNJSLib, GCNPerformRESTRequestFunction } from './gcn';

export interface AlohaRequireFunction {
    (deps: string[], callback: (...deps: any[]) => any): void;
    (dep: string): any;
}

export interface CNIFrameDocument extends Document {
    // The tagfill form.
    tagfill: HTMLFormElement;
}

export type ContextMenuLayer = 'nodecontextmenu' | 'nodecontextsubmenu' | 'nodecontextsubmenu1' | 'nodesubmenu1';

/**
 * Defines what properties are available on the main window of the GCMS UI,
 * which can then be accessed by IFrame windows.
 */
export interface CNParentWindow extends Window {

    /**
     * This method must be called from an IFrame and it initializes the
     * communication with the parent GCMS UI window.
     *
     * For the main GCMS UI window, this property is set in the `CustomerScriptService`,
     * for IFrame windows, it is set inside the GCMSUI_childIFrameInit() method of the parent window.
     * @param iFrameWindow The IFrame's window object.
     * @param iFrameDocument The IFrame's document object.
     */
    GCMSUI_childIFrameInit: (iFrameWindow: CNWindow, iFrameDocument: CNIFrameDocument) => GcmsUiBridge;

}

export interface CNWindow extends CNParentWindow {
    Aloha?: AlohaGlobal;

    document: CNIFrameDocument;

    frameElement: HTMLFrameElement;

    /** The jQuery constructor inside the iframe */
    jQuery: JQueryStatic;

    /**
     * Methods which allow scripts in the iframe to interact with the UI app.
     * See customer-script.service.ts for implementation.
     */
    GCMSUI?: GcmsUiBridge;

    GCNREST?: {
        performRESTRequest: GCNPerformRESTRequestFunction;
    };

    /** Called inside the editor frame when a page is loading / being saved */
    progress?(myframe: any, doc: Document, url: string): void;

    /** Called inside the editor frame to open the context menu on object properties list items */
    displayContextMenu?(layer: ContextMenuLayer, menuItems: any[][], event: MouseEvent, depth?: number): void;

    /** Called by inline scripts in the object properties form to show the info popup */
    JSI3_objprop_new_ass?(obj: HTMLElement, title: string, text: string, assTitle: string): void;

    /** Called by inline scripts to set the background color of a row in the object properties list */
    JSI3_objprop_new_setBgCol?(obj: HTMLElement, styleClass: string, removeClass: string): void;
}

export interface AlohaGlobal {
    bind: <T = any>(eventName: string, handlerFn: (value?: T) => void) => void;
    unbind: <T = any>(eventName: string, handlerFn: (value?: T) => void) => void;
    GCN: GCNJSLib;
    require: AlohaRequireFunction;
    settings: AlohaSettings;
    Selection: {
        getRangeObject(): AlohaRangeObject;
        SelectionRange: typeof AlohaRangeObject;
        updateSelection(): void;
    };
    ready(fn: () => void): void;
    isModified(): boolean;
    trigger(eventName: string, data: any): void;
    activeEditable?: AlohaEditable;
    getEditableById(id: string | number): AlohaEditable | null;
    getEditableHost($element: JQuery): AlohaEditable | null;
    jQuery: JQueryStatic;
    scrollToSelection(): void;
}

export interface LinkBrowser {
    /** True if the LinkBrowser is open, false if closed */
    opened: boolean;

    init(config: LinkBrowserConfig): void;

    /** Shows the repository browser instance */
    show(): void;

    /** Hides the repository browser instance */
    close(): void;

    /** Called when an item is selected */
    onSelect(item: object): void;

    /** Copies the attributes specified in Links.settings.sidebar from the original item to the rendition. */
    extendRendition(item: object, rendition: any): any;

    /** Updates the object type filter option */
    setObjectTypeFilter(filter: string | string[]): void;

    /** Returns the value of the object type filter */
    getObjectTypeFilter(): string[];
}

export interface LinkBrowserPlugin {
    browser: LinkBrowser;
    init(config: LinkBrowserConfig): void;
}

export interface LinkBrowserConfig {
    repositoryFilter: string[];
    renditionFilter: string[];
    filter: Array<'language' | 'status' | 'inherited' | 'sizeX' | 'sizeY' | 'fileSize' | 'gisResizable'>;
    adaptPageSize: boolean;
}
