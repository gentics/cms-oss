import { Injectable, NgZone } from '@angular/core';
import { PRIMARY_OUTLET, Router } from '@angular/router';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { ResourceUrlBuilder } from '@editor-ui/app/core/providers/resource-url-builder/resource-url-builder';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService, MarkObjectPropertiesAsModifiedAction } from '@editor-ui/app/state';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import {
    CropResizeParameters,
    EditMode,
    File as FileModel,
    Folder,
    Form,
    Image,
    ItemInNode,
    Language,
    Node,
    Page,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map, publishReplay, refCount, take } from 'rxjs/operators';
import { ContentFrameComponent } from '../../components/content-frame/content-frame.component';

/**
 * This service acts as a bridge between the ContentFrame and the custom scripts
 * (pre- & post-load, customer-scripts). The idea is to isolate this bridging functionality
 * here so that the ContentFrame itself only needs to deal with the logic it needs to handle
 * updates to and interactions with its own template.
 */
@Injectable()
export class CustomScriptHostService {

    get contentFrame(): ContentFrameComponent {
        if (!this.contentFrameInstance) {
            throw new Error('ContentFrame has not been set. Please call initialize() before calling other methods');
        }
        return this.contentFrameInstance;
    }

    get currentItem(): Page | FileModel | Folder | Form | Image | Node {
        return this.contentFrame.currentItem;
    }

    get editMode(): EditMode {
        return this.contentFrame.editMode;
    }

    private contentFrameInstance: ContentFrameComponent;
    private saveObjectPropertyHandler: () => Promise<any>;
    private getFocalPointHandler: () => { fpX: number, fpY: number };
    private getCropResizeParamsHandler: () => CropResizeParameters;
    private pageSavedHandler = (): void => {};
    private pageStartsSavingHandler = (): void => {};

    constructor(
        private appState: ApplicationStateService,
        private notification: I18nNotification,
        private i18n: I18nService,
        private errorHandler: ErrorHandler,
        private resourceUrlBuilder: ResourceUrlBuilder,
        private folderActions: FolderActionsService,
        private navigationService: NavigationService,
        private router: Router,
        private ngZone: NgZone,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {}

    /**
     * Set a reference to the ContentFrame compoenent.
     * Should be called immediately in the ContentFrame constructor.
     */
    initialize(contentFrame: ContentFrameComponent): void {
        this.contentFrameInstance = contentFrame;
    }

    /**
     * Returns the language object for the given page.
     */
    getItemLanguage(): Language {
        return this.contentFrame.getItemLanguage();
    }

    /**
     * Returns the language of the language variant which a page is being compared to.
     */
    getPageComparisonLanguage(): Language {
        return this.contentFrame.getPageComparisonLanguage();
    }

    /**
     * Set the value of the "requesting" flag.
     */
    setRequesting(val: boolean): void {
        this.contentFrame.requesting = val;
        this.contentFrame.runChangeDetection();
    }

    runChangeDetection(): void {
        this.contentFrame.runChangeDetection();
    }

    /**
     * Set the value of contentModified and run change detection.
     */
    setContentModified(modified: boolean): void {
        return this.contentFrame.setContentModified(modified, true);
    }

    /**
     * Called when an image is resized/cropped to distinguish from changed properties.
     */
    setImageResizedOrCropped(): void {
        this.contentFrame.setImageResizedOrCropped(true);
        return this.contentFrame.setContentModified(true, true);
    }

    /**
     * Set the value of objectPropertyModified and run change detection.
     * For use by IFrame scripts.
     */
    setObjectPropertyModified(modified: boolean, valid: boolean = true): void {
        this.markObjectPropertiesAsModifiedInState(modified, valid);
        this.contentFrame.runChangeDetection();
    }

    private markObjectPropertiesAsModifiedInState(modified: boolean, valid: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, valid));
    }

    /**
     * Used by IFrame scripts to open the file picker.
     */
    openFilePicker(type: 'image' | 'file'): Observable<File[]> {
        this.contentFrame.filePicker.multiple = false;
        this.contentFrame.filePicker.accept = type === 'image' ? 'image/*' : '*';
        // Ensure the options above get applied to the actual file input
        this.contentFrame.runChangeDetection();

        const pickedFiles$ = this.contentFrame.filePicker.fileSelect.pipe(
            take(1),
            publishReplay(1),
            refCount(),
        );

        // Hack to fix inexplicable error in IE11, where the subscribe function in the consumer of openFilePicker
        // will not be invoked. Adding a noop subscribe to the fileSelect stream fixes this, for some reason.
        pickedFiles$.subscribe(() => {});

        // Trigger the file picker dialog.
        const btn: HTMLElement = this.contentFrame.filePickerWrapper.nativeElement.querySelector('gtx-button');
        if (btn) {
            btn.click();
        }

        return pickedFiles$;
    }

    /** Uploads to the correct folder, e.g. the default folder of the node, the folder of the current page, ... */
    uploadForCurrentItem(type: 'image' | 'file', files: File[]): Observable<FileModel[]> {
        const currentNode = this.contentFrame.currentNode;
        const currentItem = this.contentFrame.currentItem;
        const defaultFolder = type === 'image' ? currentNode.defaultImageFolderId : currentNode.defaultFileFolderId;
        const uploadFolder = defaultFolder || (currentItem as Page).folderId || (currentItem as Folder).motherId;

        return this.folderActions.uploadFiles(type, files, uploadFolder).pipe(
            map(responses => {
                const fileModels: FileModel[] = responses.map(r => r.response.file);
                return fileModels;
            }),
        );
    }

    /**
     * Bridge method between the old and the new UI
     */
    openRepositoryBrowser(options: RepositoryBrowserOptions, callback: (selected: ItemInNode | ItemInNode[]) => any): void {
        this.ngZone.runGuarded(() => {
            this.repositoryBrowserClient.openRepositoryBrowser(options)
                .then((selected: ItemInNode | ItemInNode[]) => {
                    this.ngZone.runOutsideAngular(() => {
                        callback(selected);
                    });
                },
                error => this.errorHandler.catch(error));
        });
    }

    /**
     * Returns the current sid for the loggen-in user
     */
    getSid(): number {
        return this.ngZone.runGuarded(() => {
            return this.appState.now.auth.sid;
        });
    }

    /**
     * Translate a text in the UI language from inside an iframe
     * Needed e.g. for the tagfill dialog where we insert custom buttons.
     */
    getTranslation(translationKey: string, translationParams?: any): string {
        return this.i18n.translate(translationKey, translationParams);
    }

    /**
     * Show an error message toast from code inside an IFrame
     */
    showErrorMessage(message: string, duration: number = 10000): void {
        this.ngZone.runGuarded(() => {
            this.notification.show({
                message,
                type: 'alert',
                delay: duration,
            });
            this.errorHandler.catch(new Error(message), { notification: false });
        });
    }

    onPageSaved(handler: () => void): void {
        this.pageSavedHandler = handler;
    }

    pageWasSaved(): void {
        this.pageSavedHandler();
    }

    onPageStartSaving(handler: () => void): void {
        this.pageStartsSavingHandler = handler;
    }

    pageStartsSaving(): void {
        this.pageStartsSavingHandler();
    }

    /**
     * Register a handler function which will be invoked when saving a change to the
     * object properties.
     */
    onSaveObjectProperty(handler: () => Promise<any>): void {
        this.saveObjectPropertyHandler = handler;
    }

    /**
     * Invoke the handler registered by onSaveObjectProperty()
     */
    saveObjectProperty(): Promise<any> {
        if (typeof this.saveObjectPropertyHandler === 'function') {
            return this.saveObjectPropertyHandler();
        } else {
            return Promise.resolve(undefined);
        }
    }

    /**
     * Register a handler function which will be invoked when an image is saved without resizing/cropping.
     */
    onGetFocalPoint(handler: () => { fpX: number, fpY: number }): void {
        this.getFocalPointHandler = handler;
    }

    /**
     * Invoke the handler registered by onGetFocalPoint()
     */
    getFocalPoint(): { fpX: number, fpY: number } | undefined {
        if (typeof this.getFocalPointHandler === 'function') {
            return this.getFocalPointHandler();
        }
    }

    /**
     * Register a handler function which will be invoked when an image is saved with resizing/cropping.
     */
    onGetCropResizeParams(handler: () => CropResizeParameters): void {
        this.getCropResizeParamsHandler = handler;
    }

    /**
     * Invoke the handler registered by onGetCropResizeParams()
     */
    getCropResizeParams(): CropResizeParameters | undefined {
        if (typeof this.getCropResizeParamsHandler === 'function') {
            return this.getCropResizeParamsHandler();
        }
    }

    /**
     * Navigate to the given page
     */
    navigateToPagePreview(nodeId: number, pageId: number): void {
        this.navigationService
            .detailOrModal(nodeId, 'page', pageId, EditMode.PREVIEW)
            .navigate();
    }

    /**
     * Get a complete URL path to the UI app with the given page opened in preview mode
     */
    getInternalLinkUrlToPagePreview(nodeId: number, pageId: number): string {
        const urlTree = this.router.parseUrl(this.router.url);
        const detailOutlet = urlTree.root.children[PRIMARY_OUTLET].children.detail;
        detailOutlet.segments[1].path = nodeId.toString();
        detailOutlet.segments[2].path = 'page';
        detailOutlet.segments[3].path = pageId.toString();
        detailOutlet.segments[4].path = 'preview';

        return window.location.pathname + '#' + this.router.serializeUrl(urlTree);
    }
}
