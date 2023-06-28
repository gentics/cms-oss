import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { CUSTOMER_CONFIG_PATH } from '@editor-ui/app/common/config/config';
import { ALOHAPAGE_URL, API_BASE_URL, IMAGESTORE_URL } from '@editor-ui/app/common/utils/base-urls';
import { deepEqual } from '@editor-ui/app/common/utils/deep-equal';
import { ApiBase } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { EditorOverlayService } from '@editor-ui/app/editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { AppState } from '@editor-ui/app/common/models/app-state';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TagEditorService } from '@editor-ui/app/tag-editor';
import {
    ExposedPartialState,
    GcmsUiBridge,
    ItemInNode,
    Page,
    Raw,
    RepositoryBrowserOptions,
    StateChangedHandler,
    Tag,
    TagType,
} from '@gentics/cms-models';
import { of as observableOf, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged, map } from 'rxjs/operators';
import { CNIFrameDocument, CNParentWindow, CNWindow } from '../../components/content-frame/common';
import { PostLoadScript } from '../../components/content-frame/custom-scripts/post-load';
import { PreLoadScript } from '../../components/content-frame/custom-scripts/pre-load';
import { CustomScriptHostService } from '../custom-script-host/custom-script-host.service';

const IFRAME_STYLES = require('../../components/content-frame/custom-styles/gcms-ui-styles.precompile-scss');

type ZoneType = any;
// eslint-disable-next-line @typescript-eslint/naming-convention
declare const Zone: ZoneType;

// Why?
export { ExposedPartialState, GcmsUiBridge as GCMSUI, StateChangedHandler } from '@gentics/cms-models';

const gcmsui_debugTool = (window as any).gcmsui_debugTool;

/**
 * Checks for the existence of a customer-defined JavaScript file which can be run in all iframes after the post-load.ts scripts
 * have run.
 *
 * Customer-defined scripts should be located in ../customer-config/scripts/index.js, and should export a default function which
 * will be passed a GCMSUI object which contains data about the current environment and methods of interacting with the UI.
 */
@Injectable()
export class CustomerScriptService implements OnDestroy {

    private customerScript: any;
    private entryPoint: (GCMSUI: GcmsUiBridge) => any;
    private customerScriptZone: ZoneType;
    private gcmsUiStylesForIFrameBlob: Blob;
    private gcmsUiStylesForIFrameBlobUrl: string;

    constructor(private http: HttpClient,
        private state: ApplicationStateService,
        private apiBase: ApiBase,
        private entityResolver: EntityResolver,
        private tagEditorService: TagEditorService,
        private editorOverlayService: EditorOverlayService,
        private errorHandlerService: ErrorHandler,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {
        // Create a new Zone to be able to track async errors originating from the customer script.
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.customerScriptZone = Zone.root.fork({
            name: 'customerScript',
            onHandleError(): boolean {
                console.warn('Customer script threw an error:');
                return true;
            },
        });

        const iFrameStylesStr = IFRAME_STYLES && IFRAME_STYLES.default ? IFRAME_STYLES.default : IFRAME_STYLES;
        this.gcmsUiStylesForIFrameBlob = new Blob([iFrameStylesStr], { type: 'text/css' });
        this.gcmsUiStylesForIFrameBlobUrl = window.URL.createObjectURL(this.gcmsUiStylesForIFrameBlob);
    }

    ngOnDestroy(): void {
        if (this.gcmsUiStylesForIFrameBlobUrl) {
            window.URL.revokeObjectURL(this.gcmsUiStylesForIFrameBlobUrl);
            this.gcmsUiStylesForIFrameBlobUrl = null;
            this.gcmsUiStylesForIFrameBlob = null;
        }
    }

    /**
     * Check to see if a customer script has been defined in the customer-config/scripts folder, and if so attempt to
     * load and parse it.
     */
    loadCustomerScript(): void {
        const customerScriptPath = CUSTOMER_CONFIG_PATH + 'index.js';
        const sourceMapComment = `//# sourceURL=${customerScriptPath}`;

        this.http.get(customerScriptPath, { responseType: 'text' }).pipe(
            catchError(err => observableOf(null) /* script not found, don't throw error */),
            map(script => {
                if (script) {
                    // We don't catch parsing errors here, because we want them to be thrown by loadCustomerScript().
                    // eslint-disable-next-line @typescript-eslint/no-implied-eval, @typescript-eslint/restrict-template-expressions
                    return new Function('module', 'exports', 'window', 'document', `${script}; ${sourceMapComment}`);
                } else {
                    return null;
                }
            }),
        ).subscribe(script => this.customerScript = script);
    }

    /**
     * If a customer script has been loaded, invoke it's exported function.
     */
    invokeCustomerScript(window: CNWindow, document: CNIFrameDocument, scriptHost: CustomScriptHostService): void {
        if (this.customerScript) {
            const moduleObject = { exports: {} as any };
            try {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                this.customerScript.call(window, moduleObject, moduleObject.exports, window, document);
            } catch (err) {
                console.warn('Customer script could not be parsed:');
                throw err;
            }

            if (moduleObject.exports) {
                this.entryPoint = moduleObject.exports.default || moduleObject.exports;
            }

            if (!window.GCMSUI) {
                window.GCMSUI = this.createGCMSUIObject(scriptHost, window, document);
            }

            if (typeof this.entryPoint === 'function') {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                this.customerScriptZone.runGuarded(() => {
                    this.entryPoint.call(window, window.GCMSUI);
                });
            } else {
                throw new Error(`Customer script should export a function, got "${typeof this.entryPoint}"`);
            }
        }
    }


    /**
     * Create an instance of the GCMSUI object for use by customer scripts.
     */
    createGCMSUIObject(scriptHost: CustomScriptHostService, window: CNWindow, document: CNIFrameDocument): GcmsUiBridge {
        if (window.GCMSUI) {
            return window.GCMSUI;
        }

        let preLoadScriptExecuted = false;
        let postLoadScriptExecuted = false;

        const executePreLoadScript = () => {
            if (!preLoadScriptExecuted) {
                this.runPreLoadScript(window, document, scriptHost);
                preLoadScriptExecuted = true;
            }
        };
        const executePostLoadScript = () => {
            if (!postLoadScriptExecuted) {
                this.runPostLoadScript(window, document, scriptHost);
                postLoadScriptExecuted = true;
            }
        };
        const stripLeadingSlash = (url: string): string => url.replace(/^\//, '');
        const restRequestGET = (endpoint: string, params: any): Promise<object> =>
            this.apiBase.get(stripLeadingSlash(endpoint), params).toPromise();
        const restRequestPOST = (endpoint: string, data: object, params?: object): Promise<object> =>
            this.apiBase.post(stripLeadingSlash(endpoint), data, params).toPromise();
        const openTagEditor = (tag: Tag, tagType: TagType, page: Page<Raw>) =>
            this.tagEditorService.openTagEditor(tag, tagType, page);
        const openRepositoryBrowser = (options: RepositoryBrowserOptions): Promise<ItemInNode | ItemInNode[]> =>
            this.repositoryBrowserClient.openRepositoryBrowser(options);

        let subscription: Subscription;
        let stateChangedHandler: StateChangedHandler;

        // Make sure that the subscription is destroyed when the window unloads.
        const onUnload = () => {
            if (subscription) {
                subscription.unsubscribe();
                subscription = null;
            }
            window.GCMSUI = null;
            window.removeEventListener('unload', onUnload);
        };
        window.addEventListener('unload', onUnload);

        subscription = this.state.select(state => this.mapToPartialState(state)).pipe(
            distinctUntilChanged(deepEqual),
        ).subscribe(state => {
            if (typeof stateChangedHandler === 'function') {
                stateChangedHandler(state);
            }
        });

        // Make sure that child IFrames also have access to the GCMSUI init method.
        window.GCMSUI_childIFrameInit = (window.parent as CNParentWindow).GCMSUI_childIFrameInit;

        const gcmsUi: GcmsUiBridge = {
            runPreLoadScript: executePreLoadScript,
            runPostLoadScript: executePostLoadScript,
            openRepositoryBrowser,
            gcmsUiStylesUrl: this.gcmsUiStylesForIFrameBlobUrl,
            appState: this.mapToPartialState(this.state.now),
            onStateChange: (handler) => stateChangedHandler = handler,
            paths: {
                apiBaseUrl: API_BASE_URL,
                alohapageUrl: ALOHAPAGE_URL,
                imagestoreUrl: IMAGESTORE_URL,
            },
            restRequestGET,
            restRequestPOST,
            setContentModified(modified: any): void {
                if (typeof modified !== 'boolean') {
                    console.warn('setContentModified expects a boolean value as its argument');
                    return;
                }
                scriptHost.setContentModified(!!modified);
            },
            editImage: (nodeId: number, imageId: number) => {
                return this.editorOverlayService.editImage({ nodeId: nodeId, itemId: imageId });
            },
            callDebugTool: gcmsui_debugTool,
            openTagEditor,
        };

        window.GCMSUI = gcmsUi;
        return gcmsUi;
    }

    /**
     * Maps the full AppState to the ExposedPartialState, which is a sub-set of the app state which
     * may be of interest to customer scripts.
     */
    private mapToPartialState(state: AppState): ExposedPartialState {
        const item = this.entityResolver.getEntity(state.editor.itemType, state.editor.itemId);
        const language = item.type === 'page' ? this.entityResolver.getLanguage(item.contentGroupId) : undefined;
        return {
            currentItem: item,
            editMode: state.editor.editMode,
            pageLanguage: language,
            sid: state.auth.sid,
            uiLanguage: state.ui.language,
            uiVersion: state.ui.uiVersion,
            userId: state.auth.currentUserId,
        };
    }

    /** Runs the pre-load script */
    private runPreLoadScript(window: CNWindow, document: CNIFrameDocument, scriptHost: CustomScriptHostService): void {
        try {
            const script = new PreLoadScript(window, document, scriptHost);
            script.run();
        } catch (error) {
            this.errorHandlerService.catch(error, { notification: false });
        }
    }

    /** Runs the post-load script and the customer script, if it exists. */
    private runPostLoadScript(window: CNWindow, document: CNIFrameDocument, scriptHost: CustomScriptHostService): void {
        try {
            const script = new PostLoadScript(window, document, scriptHost);
            script.run();
        } catch (error) {
            this.errorHandlerService.catch(error, { notification: false });
        }

        try {
            this.invokeCustomerScript(window, document, scriptHost);
        } catch (error) {
            this.errorHandlerService.catch(error, { notification: false });
        }
    }

}
