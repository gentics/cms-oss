import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy } from '@angular/core';
import { ALOHAPAGE_URL, API_BASE_URL, IMAGESTORE_URL } from '@gentics/cms-components';
import {
    ExposedPartialState,
    GcmsUiBridge,
    ModalCloseError,
    RepositoryBrowserOptions,
    StateChangedHandler,
    TagEditorOptions,
} from '@gentics/cms-integration-api-models';
import {
    Construct,
    ItemInNode,
    Page,
    Raw,
    Tag,
    TagType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { Subscription, of } from 'rxjs';
import { catchError, distinctUntilChanged, map } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { AppState } from '../../../common/models/app-state';
import { deepEqual } from '../../../common/utils/deep-equal';
import { stripLeadingSlash } from '../../../common/utils/strip';
import { ApiBase } from '../../../core/providers/api';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { EditorOverlayService } from '../../../editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '../../../shared/providers';
import { ApplicationStateService } from '../../../state';
import { TagEditorService } from '../../../tag-editor';
import {
    UploadWithPropertiesModalComponent,
} from '../../../tag-editor/components/shared/upload-with-properties-modal/upload-with-properties-modal.component';
import { PostLoadScript } from '../../components/content-frame/custom-scripts/post-load';
import { PreLoadScript } from '../../components/content-frame/custom-scripts/pre-load';
import { CNIFrameDocument, CNParentWindow, CNWindow } from '../../models/content-frame';
import { AlohaIntegrationService } from '../aloha-integration/aloha-integration.service';
import { CustomScriptHostService } from '../custom-script-host/custom-script-host.service';
import { DynamicOverlayService } from '../dynamic-overlay/dynamic-overlay.service';


type ZoneType = any;
// eslint-disable-next-line @typescript-eslint/naming-convention
declare const Zone: ZoneType;

// eslint-disable-next-line @typescript-eslint/naming-convention
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

    constructor(
        private http: HttpClient,
        private state: ApplicationStateService,
        private apiBase: ApiBase,
        private client: GCMSRestClientService,
        private entityResolver: EntityResolver,
        private tagEditorService: TagEditorService,
        private editorOverlayService: EditorOverlayService,
        private errorHandlerService: ErrorHandler,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private aloha: AlohaIntegrationService,
        private overlays: DynamicOverlayService,
        private modals: ModalService,
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

        const iFrameStylesStr = '';
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
    loadCustomerScript(): Promise<void> {
        const customerScriptPath = CUSTOMER_CONFIG_PATH + 'index.js';
        const sourceMapComment = `//# sourceURL=${customerScriptPath}`;

        return new Promise<void>((resolve) => {
            this.http.get(customerScriptPath, { responseType: 'text' }).pipe(
                catchError(() => of(null) /* script not found, don't throw error */),
                map(script => {
                    if (script) {
                        // We don't catch parsing errors here, because we want them to be thrown by loadCustomerScript().
                        // eslint-disable-next-line @typescript-eslint/no-implied-eval, @typescript-eslint/restrict-template-expressions
                        return new Function('module', 'exports', 'window', 'document', `${script}; ${sourceMapComment}`);
                    } else {
                        return null;
                    }
                }),
            ).subscribe(script => {
                this.customerScript = script;
                resolve();
            });
        });
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
    createGCMSUIObject(scriptHost: CustomScriptHostService, iFrameWindow: CNWindow, document: CNIFrameDocument): GcmsUiBridge {
        if (iFrameWindow.GCMSUI) {
            return iFrameWindow.GCMSUI;
        }

        let preLoadScriptExecuted = false;
        let postLoadScriptExecuted = false;

        const executePreLoadScript = () => {
            if (!preLoadScriptExecuted) {
                this.runPreLoadScript(iFrameWindow, document);
                preLoadScriptExecuted = true;
            }
        };
        const executePostLoadScript = () => {
            if (!postLoadScriptExecuted) {
                this.runPostLoadScript(iFrameWindow, document, scriptHost);
                postLoadScriptExecuted = true;
            }
        };

        const restRequestGET = (endpoint: string, params: any): Promise<object> =>
            this.apiBase.get(stripLeadingSlash(endpoint), params).toPromise();
        const restRequestPOST = (endpoint: string, data: object, params?: object): Promise<object> =>
            this.apiBase.post(stripLeadingSlash(endpoint), data, params).toPromise();
        const restRequestDELETE = (endpoint: string, params?: object): Promise<void | object> =>
            this.apiBase.delete(stripLeadingSlash(endpoint), params).toPromise();
        const openTagEditor = (tag: Tag, tagType: TagType, page: Page<Raw>, options?: TagEditorOptions) =>
            this.tagEditorService.openTagEditor(tag, tagType, page, options);
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

            this.aloha.clearReferences();
            this.overlays.closeRemaining();

            iFrameWindow.GCMSUI = null;
            iFrameWindow.removeEventListener('unload', onUnload);
        };
        iFrameWindow.addEventListener('unload', onUnload);

        subscription = this.state.select(state => this.mapToPartialState(state)).pipe(
            distinctUntilChanged(deepEqual),
        ).subscribe(state => {
            if (typeof stateChangedHandler === 'function') {
                stateChangedHandler(state);
            }
        });

        // Make sure that child IFrames also have access to the GCMSUI init method.
        iFrameWindow.GCMSUI_childIFrameInit = (iFrameWindow.parent as CNParentWindow).GCMSUI_childIFrameInit;
        const loadedConstructs = new Promise<Record<string, Construct>>((resolve, reject) => {
            this.aloha.constructs$.subscribe({
                next(value) {
                    const dict: Record<string, Construct> = {};
                    (value || []).forEach(c => {
                        dict[c.keyword] = c;
                    });
                    resolve(dict);
                },
                error(err) {
                    reject(err)
                },
            });
        });

        const gcmsUi: GcmsUiBridge = {
            runPreLoadScript: executePreLoadScript,
            runPostLoadScript: executePostLoadScript,
            openRepositoryBrowser,
            gcmsUiStylesUrl: this.gcmsUiStylesForIFrameBlobUrl,
            appState: this.mapToPartialState(this.state.now),
            onStateChange: (handler) => stateChangedHandler = handler,
            getConstructs: () => loadedConstructs,
            paths: {
                apiBaseUrl: API_BASE_URL,
                alohapageUrl: ALOHAPAGE_URL,
                imagestoreUrl: IMAGESTORE_URL,
            },
            restRequestGET,
            restRequestPOST,
            restRequestDELETE,
            restClient: this.client.getClient(),

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
            openDynamicDropdown: (configuration, slot) => {
                return this.overlays.openDynamicDropdown(configuration, slot);
            },
            openDynamicModal: (configuration) => {
                return this.overlays.openDynamicModal(configuration);
            },
            openDialog: (configuration) => {
                return this.overlays.openDialog(configuration);
            },
            closeErrorClass: ModalCloseError,
            registerComponent: (slot, component) => {
                this.aloha.registerComponent(slot, component);
            },
            unregisterComponent: (slot) => {
                this.aloha.unregisterComponent(slot);
            },
            focusEditorTab: (tabId) => {
                this.aloha.changeActivePageEditorTab(tabId);
            },
            openUploadModal: (uploadType, destinationFolder, allowFolderSelection) => {
                return this.modals.fromComponent(
                    UploadWithPropertiesModalComponent,
                    { padding: true, width: '1000px' },
                    {
                        itemType: uploadType,
                        allowFolderSelection: allowFolderSelection ?? true,
                        destinationFolder,
                    },
                ).then(dialog => dialog.open());
            },
        };

        iFrameWindow.GCMSUI = gcmsUi;
        // Aloha might not be defined if the page as a rendering error
        if (iFrameWindow.Aloha != null) {
            iFrameWindow.Aloha.trigger('gcmsui.ready', {});
        }

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
    private runPreLoadScript(window: CNWindow, document: CNIFrameDocument): void {
        try {
            const script = new PreLoadScript(window, document);
            script.run();
        } catch (error) {
            this.errorHandlerService.catch(error, { notification: false });
        }
    }

    /** Runs the post-load script and the customer script, if it exists. */
    private runPostLoadScript(window: CNWindow, document: CNIFrameDocument, scriptHost: CustomScriptHostService): void {
        try {
            const script = new PostLoadScript(window, document, scriptHost, this.aloha);
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
