/* eslint-disable @typescript-eslint/naming-convention */
import { AlohaEditable, AlohaRangeObject, AlohaSettings } from '@gentics/aloha-models';
import { GcmsUiBridge } from '@gentics/cms-integration-api-models';
import { Page } from '@gentics/cms-models';

/** Requests that can be sent via `Aloha.GCN.performRESTRequest()` */
export interface GCNRestRequestArgs {
    type?: 'GET' | 'POST' | 'PUT' | 'DELETE';
    timeout?: number;
    body?: object;
    url: string;
    success?: (result: object) => any;
    error?: (error: object) => any;
}
export type GCNPerformRESTRequestFunction = (config: GCNRestRequestArgs) => void;

/** Requests as sent via `Aloha.GCN.savePage()` */
export interface GCNJsLibRequestOptions {
    cache?: boolean;
    contentType: string;
    data: string;
    dataType: 'json' | '';
    type: 'GET' | 'POST' | 'DELETE';
    url: string;
    complete(): void;
    error(error: Error): void;
    success(data: any): void;
}

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
        performRESTRequest: GCNPerformRESTRequestFunction
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

export interface GCNJSLib {
    /** JavaScript representation of the page */
    page: {
        _data: Page;

        /** Page properties that were updated */
        _shadow: Partial<Page>;

        /** Sends AJAX requests to the server */
        _ajax(options: GCNJsLibRequestOptions): void;

        /** Gets called every time the JS lib updates a property of a page. */
        _update(path: string, value: any, error?: any, force?: boolean): void;

        /** Fetches a tag (if necessary) and calls the callback with it. */
        tag(tagName: string, callback: (tag: any) => void): void;
    };
    performRESTRequest: GCNPerformRESTRequestFunction;
    savePage(options: {
        createVersion?: boolean;
        unlock?: boolean;
        onsuccess(returnValue: Page): void;
        onfailure(data: any, error: Error): void;
    }): void;
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
    onSelect(item: Object): void;

    /** Copies the attributes specified in Links.settings.sidebar from the original item to the rendition. */
    extendRendition(item: Object, rendition: any): any;

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

/**
 * URL to load when we want to unload a GCMS document, either when closing the frame or switching
 * to a difference url. Required to trigger the beforeunload & unload events which allow
 * us to run some logic to check whether it is safe to navigate away or not.
 */
export const BLANK_PAGE = 'about:blank';

/**
 * This html document is loaded into the iframe when the folder/page/etc properties form is being displayed.
 * It is never directly viewed by the user, but is needed in order for the existing system of "beforeunload"
 * dialogs to work reliably cross browser.
 */
export const BLANK_PROPERTIES_PAGE = 'assets/properties-blank.html';
