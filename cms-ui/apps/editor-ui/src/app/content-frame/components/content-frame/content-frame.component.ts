import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostListener,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { EditorState, EditorTab, ITEM_PROPERTIES_TAB, ITEM_TAG_LIST_TAB, PropertiesTab, SaveBehaviour } from '@editor-ui/app/common/models';
import {
    CmsFormType,
    EditMode,
    File as FileModel,
    Folder,
    FolderItemOrNodeType,
    FolderItemType,
    Form,
    Image,
    InheritableItem,
    ItemNormalized,
    ItemPermissions,
    ItemWithObjectTags,
    Language,
    Node,
    Normalized,
    Page,
    Raw,
    noItemPermissions,
} from '@gentics/cms-models';
import { FilePickerComponent, ModalService } from '@gentics/ui-core';
import { debounce as _debounce, isEqual } from 'lodash';
import {
    BehaviorSubject,
    Observable,
    SubscribableOrPromise,
    Subscription,
    combineLatest,
    forkJoin,
    of,
} from 'rxjs';
import {
    catchError,
    delay,
    distinctUntilChanged,
    filter,
    map,
    mergeMap,
    publishReplay,
    refCount,
    switchMap,
    switchMapTo,
    take,
    tap,
    withLatestFrom,
} from 'rxjs/operators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { parentFolderOfItem } from '../../../common/utils/parent-folder-of-item';
import { Api } from '../../../core/providers/api/api.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { PublishTimeManagedPagesModal } from '../../../shared/components/publish-time-managed-pages-modal/publish-time-managed-pages-modal.component';
import { TimeManagementModal } from '../../../shared/components/time-management-modal/time-management-modal.component';
import { PublishableStateUtil } from '../../../shared/util/entity-states';
import {
    AddEditedEntityToRecentItemsAction,
    ApplicationStateService,
    CancelEditingAction,
    CloseEditorAction,
    ComparePageVersionSourcesAction,
    ComparePageVersionsAction,
    EditorActionsService,
    EditorStateUrlOptions,
    EditorStateUrlParams,
    FolderActionsService,
    ListSavingErrorAction,
    MarkContentAsModifiedAction,
    MarkObjectPropertiesAsModifiedAction,
    PreviewPageVersionAction,
    ResetPageLockAction,
    SaveErrorAction,
    SaveSuccessAction,
    StartSavingAction,
} from '../../../state';
import { TagEditorService } from '../../../tag-editor';
import { CustomScriptHostService } from '../../providers/custom-script-host/custom-script-host.service';
import { CustomerScriptService } from '../../providers/customer-script/customer-script.service';
import { IFrameManager } from '../../providers/iframe/iframe-manager.service';
import { CombinedPropertiesEditorComponent } from '../combined-properties-editor/combined-properties-editor.component';
import { ConfirmApplyToSubitemsModalComponent } from '../confirm-apply-to-subitems-modal/confirm-apply-to-subitems-modal.component';
import { ConfirmNavigationModal } from '../confirm-navigation-modal/confirm-navigation-modal.component';
import { CNParentWindow, CNWindow } from './common';

/**
 * To make the iframed contentnode pages better fit the look and feel of this app, we apply quite a lot of custom
 * CSS (see the /custom-styles folder). However, this may have the unwanted effect of overwriting user implementations
 * (e.g. custom-styled tag fill dialogs). Therefore we will use the value of this constant to control whether or not
 * our custom styles are being applied or not.
 *
 * TODO: actual implementation of styles toggle still needs to be done. This flag is just there in case it is needed.
 */
const APPLY_CUSTOM_STYLES = true;

/**
 * This component wraps the GCMS content in an iframe, and provides the means for interacting with
 * the content of the frame.
 */
@Component({
    selector: 'content-frame',
    templateUrl: './content-frame.component.html',
    styleUrls: ['./content-frame.component.scss'],
    providers: [IFrameManager, CustomScriptHostService],
})
export class ContentFrame implements OnInit, AfterViewInit, OnDestroy {

    /**
     * Lodash debounce function used by the ContentFrame.
     * It is stored as a static property to allow overriding in unit tests.
     */
    static _debounce = _debounce;

    public readonly ITEM_PROPERTIES_TAB = ITEM_PROPERTIES_TAB;
    public readonly APPLY_CUSTOM_STYLES = APPLY_CUSTOM_STYLES;
    public readonly CMS_FORM_TYPE = CmsFormType;

    @ViewChild('iframe', { static: true })
    private iframe: ElementRef<HTMLIFrameElement>;

    @ViewChild(CombinedPropertiesEditorComponent)
    private combinedPropertiesEditor: CombinedPropertiesEditorComponent;

    @ViewChild(FilePickerComponent, { static: true })
    filePicker: FilePickerComponent;

    @ViewChild('filePickerWrapper', { static: true })
    filePickerWrapper: ElementRef<HTMLDivElement>;

    contentModified = false;
    imageResizedOrCropped = false;
    objectPropertyModified = false;
    modifiedObjectPropertyValid: boolean;
    currentItem: ItemNormalized;
    editorNodeId: number;
    currentNode: Node;
    editorIsOpen = false;
    isImageLoading = false;
    editMode: EditMode;
    // currently only used by form editor (not properties editing)
    itemValid = false;

    activeNode$: Observable<Node>;
    isInherited$: Observable<boolean>;
    editorState$: Observable<EditorState>;
    propertiesTab$: Observable<EditorTab>;
    openPropertiesTab: PropertiesTab;
    requesting: boolean;
    currentItemPath = '';
    currentItem$: Observable<ItemNormalized | undefined>;

    activeUiLanguageCode$: Observable<string>;
    activeFormLanguageCode$: Observable<string>;

    itemLanguage: Language | undefined = undefined;
    pageComparisonLanguage: Language | undefined = undefined;
    previewLink: any[] = [];

    saveAsCopyButtonIsDisabled = false;
    saveButtonVisible = true;
    saveButtonIsDisabled = false;
    isLocked = false;

    /** If has permission to publish and state is planned return true */
    isInQueue$: Observable<boolean> = undefined;

    private forceItemRefresh$ = new BehaviorSubject<void>(undefined);
    private onLoadListener: EventListener;
    public itemPermissions: ItemPermissions = noItemPermissions;
    private subscriptions: Subscription[] = [];
    private masterFrameLoaded = false;
    private alohaReady = false;
    private contentModifiedByExternalScript = false;

    private cancelEditingDebounced: (item: Page | FileModel | Folder | Form | Image | Node) => void = ContentFrame._debounce(
        (item: Page | FileModel | Folder | Image | Node) => {
            if (item && item.type === 'page') {
                this.appState.dispatch(new CancelEditingAction());
                this.appState.dispatch(new ResetPageLockAction(item.id));
            }
        }, 250, { leading: true, trailing: false },
    );

    constructor(
        private appState: ApplicationStateService,
        private route: ActivatedRoute,
        private navigationService: NavigationService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
        private decisionModals: DecisionModalsService,
        private changeDetector: ChangeDetectorRef,
        private ngZone: NgZone,
        private editorActions: EditorActionsService,
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private permissions: PermissionService,
        private iframeManager: IFrameManager,
        private notification: I18nNotification,
        private customScriptHostService: CustomScriptHostService,
        private customerScriptService: CustomerScriptService,
        private api: Api,
        private tagEditorService: TagEditorService,
    ) { }

    ngOnInit(): void {
        this.customScriptHostService.initialize(this);
        (window as unknown as CNParentWindow).GCMSUI_childIFrameInit =
            (iFrameWindow, iFrameDocument) => this.customerScriptService.createGCMSUIObject(this.customScriptHostService, iFrameWindow, iFrameDocument);

        this.subscriptions.push(
            this.appState.select(state => state.editor.contentModified)
                .subscribe(modified => this.contentModified = modified),
            this.appState.select(state => state.editor.objectPropertiesModified)
                .subscribe(modified => this.objectPropertyModified = modified),
            this.appState.select(state => state.editor.modifiedObjectPropertiesValid)
                .subscribe(valid => this.modifiedObjectPropertyValid = valid),
            this.appState.select(state => state.editor.openPropertiesTab)
                .subscribe(openPropertiesTab => this.openPropertiesTab = openPropertiesTab),
        );

        const onLogin$ = this.appState.select(state => state.auth).pipe(
            distinctUntilChanged(isEqual, state => state.currentUserId),
            filter(state => state.isLoggedIn === true),
        );

        let prevEditMode: EditMode = null;
        let prevItemID: number = null;
        let prevItemType: FolderItemType = null;
        this.subscriptions.push(onLogin$.pipe(
            switchMapTo(this.route.params),
            switchMap((params: EditorStateUrlParams) => {
                // We always fetch the item to make sure that we have the current state,
                // except if the item is already open in the content-frame and we have not switched to edit mode.
                const reqItemId = Number(params.itemId);
                const reqNodeId = Number(params.nodeId);
                const requireLoadForUpdate = (params.type as FolderItemOrNodeType) !== 'node' &&
                    (prevEditMode !== params.editMode || prevItemID !== reqItemId || prevItemType !== params.type) &&
                    (params.editMode === 'edit' || params.editMode === 'editProperties');
                prevEditMode = params.editMode;
                prevItemID = reqItemId;
                prevItemType = params.type;

                if (!requireLoadForUpdate
                    && this.currentItem
                    && this.allTagsHaveConstructs(this.currentItem)
                    && reqItemId === this.currentItem.id
                    && ((params.type as FolderItemOrNodeType) !== 'node' || (this.currentNode && reqNodeId === this.currentNode.id))
                ) {
                    return [params];
                }

                const options = { nodeId: params.nodeId };

                if (requireLoadForUpdate) {
                    options['update'] = true;
                }
                if (params.type === 'page') {
                    options['langvars'] = true;
                }
                if (<FolderItemOrNodeType> params.type !== 'node') {
                    options['construct'] = true;
                }

                const itemLoaded = (item: InheritableItem) => {
                    if (item) {
                        return params;
                    } else {
                        return false;
                    }
                };

                return this.folderActions.getItem(params.itemId, params.type, options, true)
                    .then(itemLoaded)
                    .catch(error => {
                        // If there is an error, the user might not have edit permissions, so we try loading again without update=true.
                        // This MUST be changed after the REST API can deliver all object property definitions without using update=true.
                        if (!options['update']) {
                            this.errorHandler.catch(error);
                            return;
                        }

                        delete options['update'];
                        return this.folderActions.getItem(params.itemId, params.type, options)
                            .then(itemLoaded);
                    });
            }),
        ).subscribe(params => params && this.updateEditorState(params)));

        this.activeNode$ = combineLatest([
            this.appState.select(state => state.editor.nodeId),
            this.appState.select(state => state.entities.node),
        ]).pipe(
            map(([nodeId, loadedNodes]) => loadedNodes[nodeId]),
            filter(node => node != null),
        );

        this.isInherited$ = this.activeNode$
            .pipe(map(activeNode => activeNode && activeNode.inheritedFromId !== activeNode.id));

        const requestingSubscription = this.iframeManager.requesting$.subscribe(val => {
            this.requesting = val;
            this.changeDetector.markForCheck();
        });
        this.subscriptions.push(requestingSubscription);

        this.iframeManager.onMasterFrameClosed(() => {
            this.cancelEditingDebounced(this.currentItem);
        });

        const editorState$ = this.editorState$ = this.appState.select(state => state.editor).pipe(
            // If the editor is not open yet, the editorState may still contain the IDs from the last time it was open.
            filter(editorState => editorState.editorIsOpen),
            publishReplay(1),
            refCount(),
        );

        this.propertiesTab$ = editorState$.map(state => state.openTab).pipe(
            distinctUntilChanged(isEqual),
        );

        this.currentItem$ = editorState$.pipe(
            distinctUntilChanged((a, b) =>
                a.itemId === b.itemId &&
                a.itemType === b.itemType &&
                a.nodeId === b.nodeId,
            ),
            switchMap(editorState => this.forceItemRefresh$.pipe(
                map(() => editorState),
            )),
            switchMap(({ itemId, itemType }) => {
                // forceItemRefresh$ and copying of the item in the subsequent map() is necessary
                // to force the UI to reset if a saveRequest resulted in an unchanged item.
                let forceRefresh = true;
                const item$ = (itemId && itemType) ? this.appState.select(state => state.entities[itemType][itemId]) : of(undefined);
                return item$.pipe(
                    distinctUntilChanged(isEqual),
                    map(item => {
                        if (forceRefresh) {
                            item = { ...item };
                            forceRefresh = false;
                        }
                        return item;
                    }),
                );
            }),
            publishReplay(1),
            refCount(),
        );

        const itemSub = this.currentItem$.pipe(
            delay(0),
        ).subscribe((item) => {
            this.tagEditorService.forceCloseTagEditor();
            this.currentItemPath = this.getItemPath(item);
            this.changeDetector.detectChanges();
        });
        this.subscriptions.push(itemSub);

        const localStateSubscription = editorState$.pipe(
            switchMap(state => {
                if (!state.editorIsOpen || !state.itemId) {
                    return of(state);
                }

                // If the entity is not yet in the app store, we need to fetch it first.
                const entity = this.entityResolver.getEntity(state.itemType, state.itemId);
                const node = this.entityResolver.getEntity('node', state.nodeId);
                const fetchEntities: SubscribableOrPromise<any>[] = [];

                if (!entity) {
                    if (state.itemType === 'node') {
                        fetchEntities.push(this.folderActions.getNode(state.itemId));
                    } else {
                        fetchEntities.push(this.folderActions.getItem(state.itemId, state.itemType));
                    }
                }
                if (!node) {
                    fetchEntities.push(
                        this.appState.select(state => state.entities.node).pipe(
                            filter(node => !!node[state.nodeId]),
                            take(1),
                        ),
                    );
                }
                if (0 < fetchEntities.length) {
                    return forkJoin(fetchEntities).mapTo(state);
                }

                return of(state);
            }),
            tap(state => {
                if (this.currentItem && state.itemId !== this.currentItem.id) {
                    this.cancelEditingDebounced(this.currentItem);
                }
                this.setLocalStateVars(state);
            }),
            // Permissions need to be fetched for a specific language, node, folder and item type
            distinctUntilChanged((a, b) =>
                a.itemType === b.itemType &&
                a.itemId === b.itemId &&
                a.nodeId === b.nodeId,
            ),
            filter(state => state.itemId != null && state.itemType != null),
            withLatestFrom(this.appState.select(state => state.folder.activeNodeLanguages.list)),
            filter(([, langs]) => langs?.length > 0),
            switchMap(([state]) => {
                this.itemLanguage = this.getItemLanguage();
                this.changeDetector.markForCheck();

                const langId: number = this.itemLanguage ? this.itemLanguage.id : null;

                if (state.itemType === 'node') {
                    const node = this.entityResolver.getNode(state.nodeId);
                    return this.permissions.forItem(node.folderId, 'folder', node.id);
                } else if (state.itemType === 'folder') {
                    const node = this.entityResolver.getNode(state.nodeId);
                    return this.permissions.forFolder(state.itemId, node.id).pipe(
                        map(permissions => permissions.folder),
                    );
                } else {
                    return this.permissions.forItemInLanguage(state.itemType, state.itemId, state.nodeId, langId);
                }
            }),
        ).subscribe(fetchedPermissions => {
            this.itemPermissions = fetchedPermissions;
            this.changeDetector.markForCheck();
        });
        this.subscriptions.push(localStateSubscription);

        this.activeUiLanguageCode$ = this.appState.select(state => state.ui.language);

        this.activeFormLanguageCode$ = this.appState.select(state => state.folder.activeFormLanguage).pipe(
            mergeMap(activeLanguageId => this.appState.select(state => state.entities.language[activeLanguageId])),
            map(activeLanguage => activeLanguage && activeLanguage.code),
        );
    }

    ngAfterViewInit(): void {
        const masterFrame = this.iframe.nativeElement;
        this.iframeManager.initialize(masterFrame, this);
    }

    /**
     * Remove the iframe's load event listener, unsubscribe from streams.
     */
    ngOnDestroy(): void {
        const iframe = this.iframe.nativeElement;
        iframe.removeEventListener('load', this.onLoadListener);
        this.subscriptions.forEach(s => s.unsubscribe());
        this.iframeManager.destroy();
        this.cancelEditingDebounced(this.currentItem);
        this.appState.dispatch(new CloseEditorAction());
        (window as unknown as CNParentWindow).GCMSUI_childIFrameInit = null;
    }

    /**
     * In-app navigation is handled by the CanDeactiveContentFrame guard, but in the case that the user
     * e.g. presses the "refresh" button in the browser, then this confirmation dialog will still prevent
     * unwanted loss of unsaved data.
     */
    @HostListener('window:beforeunload', ['$event'])
    windowBeforeUnload(e: BeforeUnloadEvent): string | undefined {
        if (this.contentModified || this.objectPropertyModified) {
            const message = 'Leave page without saving?';
            e.returnValue = message;
            return message;
        }
        this.cancelEditingDebounced(this.currentItem);
        return;
    }

    getItemPath(item: Page | FileModel | Folder | Form | Image | Node): string {
        if (item && item.type !== 'node' && item.type !== 'channel' && item.type !== 'form') {
            const currentPath = (item as Page | Image | FileModel | Folder).path.slice(1);
            if (item.type === 'folder') {
                return currentPath.slice(0, -1);
            } else {
                return currentPath + this.currentItem.name;
            }
        }
        return '';
    }

    /**
     * Returns the language object for the given page.
     */
    getItemLanguage(): Language | undefined {
        if (!this.currentItem) {
            return null;
        }
        if (this.currentItem.type === 'page') {
            return this.entityResolver.getLanguage((this.currentItem as Page).contentGroupId);
        }

        if (this.currentItem.type !== 'form') {
            return;
        }

        const form = this.currentItem as Form;
        const { activeFormLanguage: currentLanguageId, activeNodeLanguages } = this.appState.now.folder;
        const nodeLanguages = activeNodeLanguages.list.map(nlid => {
            return this.entityResolver.getLanguage(nlid);
        });
        const currentLanguage = this.entityResolver.getLanguage(currentLanguageId);
        if (currentLanguage && form.languages.includes(currentLanguage.code)) {
            return currentLanguage;
        }

        const fallbackLanguageCodes = Array.isArray(form.languages) && form.languages.length > 0 && form.languages;
        if (!fallbackLanguageCodes) {
            throw new Error(`Form with ID ${form.id} has no translation defined in form.languages.`);
        }

        const fallbackLanguages = form.languages
            .filter(l => nodeLanguages.map(nl => nl.code).includes(l))
            .map(formlanguageInNodeCode => {
                return Object.values(this.appState.now.entities.language)
                    .find(stateLanguage => stateLanguage.code === formlanguageInNodeCode);
            });

        if (!Array.isArray(fallbackLanguages) || fallbackLanguages.length === 0) {
            throw new Error(
                `Form with ID ${form.id} has no translation in a language which is configured for Node with ID ${this.currentNode.id}.`,
            );
        }

        return fallbackLanguages[0];
    }

    /**
     * Gets the current item async, which guarantees that the value will always be available
     * by the time the promise resolves.
     */
    getCurrentItem(): Promise<Page | FileModel | Folder | Form | Image | Node> {
        if (this.currentItem) {
            return Promise.resolve(this.currentItem);
        }

        const editorState = this.appState.now.editor;
        return this.currentItem$.pipe(
            filter(item => item &&
                item.id === editorState.itemId &&
                item.type === editorState.itemType),
            take(1),
        ).toPromise();
    }

    /**
     * Returns the language of the language variant which a page is being compared to.
     */
    getPageComparisonLanguage(): Language | undefined {
        const editorState = this.appState.now.editor;
        if (editorState.compareWithId) {
            const variant = this.entityResolver.getPage(editorState.compareWithId);
            if (variant) {
                return this.entityResolver.getLanguage(variant.contentGroupId);
            }
        }
    }

    /**
     * Used by the IFrameManager to signal that the "load" event has fired in the master frame.
     */
    setMasterFrameLoaded(val: boolean): void {
        this.masterFrameLoaded = val;
    }

    /**
     * Used by the IFrameManager to signal that the "aloha-ready" event has fired in the master frame.
     */
    setAlohaReady(val: boolean): void {
        this.alohaReady = val;
        this.changeDetector.markForCheck();
    }

    /**
     * Set the value of contentModified and run change detection.
     *
     * @param modified - sets the value of contentModified
     * @param modifiedByExternalScript - Indicates that this change was called from an external
     * script from within the iframe via the GCMSUI.setContentModified() method.
     */
    setContentModified(modified: boolean, modifiedByExternalScript: boolean = false): void {
        if (this.editMode === 'edit' || this.editMode === 'editProperties') {
            if (modified && modifiedByExternalScript) {
                this.contentModifiedByExternalScript = true;
            }
            if (!modified) {
                this.imageResizedOrCropped = false;
            }
            this.markContentAsModifiedInState(modified);
            this.runChangeDetection();
        }
    }

    /**
     * Sets the validity status of the current item
     */
    setItemValidity(valid: boolean): void {
        this.itemValid = valid;
        this.setLocalStateVars(this.appState.now.editor);
    }

    setImageResizedOrCropped(resizedOrCropped: boolean): void {
        this.imageResizedOrCropped = resizedOrCropped;
    }

    markContentAsModifiedInState(modified: boolean): void {
        this.appState.dispatch(new MarkContentAsModifiedAction(modified));
    }

    markObjectPropertiesAsModifiedInState(modified: boolean): void {
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(modified, true));
    }

    /**
     * Used by IFrame scripts to trigger a change detection cycle when one of this component's properties
     * get updated.
     */
    runChangeDetection(): void {
        if (!this.iframeManager.destroyed) {
            this.ngZone.run(() => {
                this.changeDetector.markForCheck();
                this.changeDetector.detectChanges();
            });
        }
    }

    /**
     * Switch the state to preview mode.
     */
    createPreviewLink(): any[] | undefined {
        return this.currentItem
            ? this.navigationService
                .detailOrModal(this.currentNode && this.currentNode.id, this.currentItem.type, this.currentItem.id, EditMode.PREVIEW)
                .commands()
            : undefined;
    }

    /**
     * User wants to switch tabs of item properties.
     * If the item is inherited, we need to ask the user which item they want to edit. The inheritance check only
     * makes sense for images or files, where they can be previewed in an inherited state and then transitioned to
     * a localized/original edit view.
     */
    changePropertiesTab(newTab: EditorTab): void {
        const itemType = this.currentItem.type;
        if ((itemType === 'file' || itemType === 'image') && newTab !== 'preview') {
            this.decisionModals.showInheritedDialog(this.currentItem, this.currentNode.id)
                .then(({ item, nodeId }) =>
                    this.navigationService
                        .detailOrModal(nodeId, itemType, item.id, EditMode.EDIT_PROPERTIES, { openTab: newTab })
                        .navigate(),
                );
        } else {
            this.navigationService
                .detailOrModal(this.currentNode.id, itemType, this.currentItem.id, EditMode.EDIT_PROPERTIES, { openTab: newTab })
                .navigate();
        }
    }

    determineSaveAsCopyButtonIsDisabled(): boolean {
        return this.determineSaveButtonIsDisabled()
            || (this.currentItem && this.currentItem.type === 'image' && !this.imageResizedOrCropped);
    }

    determineSaveButtonIsDisabled(): boolean {

        /** If properties of any entity is being edited: */
        if (
            this.editMode === 'editProperties'
            && (!this.modifiedObjectPropertyValid || !this.combinedPropertiesEditor)
        ) {
            return true;
        }

        /** If entity is being edited: */
        if (this.editMode !== 'editProperties' && this.currentItem) {

            /**
             * Implementation explanation:
             * Since it is presumed that backend will verify item validity PUT/POST requests,
             * there is is no client-side validity checking for `page`, `image`, `file` data.
             * But for `form` there is, since `form` contains data maintained by application
             * `libs/form-generator/src/lib/form-generator.module.ts` which is not validated by backend.
             *
             * Also:
             * Property `locked` is only provided and checked for `page` and `form`.
             */

            /** If is Folder, Page, Image or File and not yet loaded or locked: */
            if (
                this.currentItem.type !== 'form'
                && ((!this.alohaReady && !this.masterFrameLoaded) || this.isLockedByAnother())
            ) {
                return true;
            }
            /** If is Form and is not valid: */
            if (
                this.currentItem.type === 'form'
                && (!this.itemValid || this.isLockedByAnother())
            ) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the item is locked by a different user than the current logged in user.
     * If this is the case, the item should not be editable.
     */
    isLockedByAnother(): boolean {
        const currentUserId = this.appState.now.auth.currentUserId;
        if (this.currentItem && (this.currentItem.type === 'page' || this.currentItem.type === 'form')) {
            const item = this.currentItem as Page | Form;
            if (item.locked && item.lockedBy !== currentUserId) {
                return true;
            }
        }
        return false;
    }

    public handleItemSave(behaviour: SaveBehaviour): Promise<void> | undefined {
        switch (behaviour) {
            case SaveBehaviour.REGULAR:
                return this.saveChanges();
            case SaveBehaviour.APPLY_TO_LANGUAGE_VARIANTS:
                return this.saveChangesAndApplyToLanguageVariants();
            case SaveBehaviour.APPLY_TO_SUBFOLDERS:
                return this.saveChangesAndApplyToSubfolders();
            case SaveBehaviour.SAVE_AS_COPY:
                return this.cropAndResizeImage(true);
            case SaveBehaviour.SAVE_AND_PUBLISH:
                this.saveAndPublishItem();
                break;
        }
    }

    /**
     * Use the GCN JavaScript API to save the current page, or the CombinedPropertiesEditor for simple properties and object properties.
     */
    saveChanges(): Promise<void> | undefined {
        const itemId = this.currentItem.id;
        if (this.editMode === 'editProperties') {
            if (this.appState.now.editor.modifiedObjectPropertiesValid) {
                return this.combinedPropertiesEditor.saveChanges()
                    .then(() => this.forceItemRefresh$.next());
            }

            this.notification.show({
                type: 'alert',
                message: 'message.invalid_image_property',
            });

            return;
        } else if (this.editMode !== 'edit') {
            return;
        }

        switch (this.currentItem.type) {
            case 'page':
                return this.savePage().then(() => {
                    this.contentModifiedByExternalScript = false;
                    this.setContentModified(false);

                    this.folderActions.getPage(this.currentItem.id, {langvars: true});
                });

            case 'form':
                return this.saveForm().then(() => {
                    this.contentModifiedByExternalScript = false;

                    this.folderActions.getForm(this.currentItem.id)
                        .then(() => this.setContentModified(false));
                });

            case 'image': {
                if (this.imageResizedOrCropped) {
                    return this.cropAndResizeImage(false);
                }
                this.appState.dispatch(new StartSavingAction());
                const focalPoint = this.customScriptHostService.getFocalPoint();

                return this.folderActions.updateImageProperties(itemId, focalPoint, { showNotification: true, fetchForUpdate: false })
                    .then(() => {
                        this.appState.dispatch(new SaveSuccessAction());
                        this.markContentAsModifiedInState(false);
                    });
            }

            default:
                break;
        }
    }

    /**
     * Saves the current object property and applies it to all subfolders.
     * This is only available if the current item is a folder.
     */
    saveChangesAndApplyToSubfolders(): Promise<void> {
        const options = {
            closeOnOverlayClick: false,
        };
        return this.modalService.fromComponent(ConfirmApplyToSubitemsModalComponent, options, {
            item: this.currentItem as ItemWithObjectTags,
            objPropId: this.openPropertiesTab,
        }).then(dialog => dialog.open())
            .then(() => this.combinedPropertiesEditor.saveChanges({ applyToSubfolders: true }))
            .catch(() => {});
    }

    /**
     * Saves the current object property and applies it to all language variants.
     * This is only available if the current item is a page.
     */
    saveChangesAndApplyToLanguageVariants(): Promise<void> {
        const options = {
            closeOnOverlayClick: false,
        };
        const languageVariants = Object.values((this.currentItem as Page).languageVariants);
        return this.modalService.fromComponent(ConfirmApplyToSubitemsModalComponent, options, {
            item: this.currentItem as ItemWithObjectTags,
            objPropId: this.openPropertiesTab,
        }).then(dialog => dialog.open())
            .then(() => this.combinedPropertiesEditor.saveChanges({ applyToLanguageVariants: languageVariants }))
            .catch(() => {});
    }

    /** This is used to reset the modified flags in the EditorState. */
    onChangesDiscarded(): void {
        this.appState.dispatch(new MarkContentAsModifiedAction(false));
        this.appState.dispatch(new MarkObjectPropertiesAsModifiedAction(false, false));
    }

    /**
     * Saves the current page if necessary and then publishes it.
     */
    saveAndPublishItem(): void {
        switch (this.currentItem.type) {
            case 'page':
                if (this.editMode === 'edit' || (this.editMode === 'editProperties' && this.contentModified === true)) {
                    this.saveChanges()
                        .then<Page>(() => {
                        if (this.contentModifiedByExternalScript) {
                            this.contentModifiedByExternalScript = false;
                            // if an external script modified the content, then the page entity we have in
                            // the state may contain stale data. Therefore we should fetch the page newly
                            // before publishing, since the publishPageSuccess state action reads the entity
                            // data to decide how to e.g. update the time management status.
                            return this.folderActions.getPage(this.currentItem.id);
                        } else {
                            return this.currentItem as Page;
                        }
                    })
                        .then(() => this.publishPage(true));
                } else {
                    this.publishPage(true);
                }
                break;

            case 'form':
                if (this.editMode === 'edit' || (this.editMode === 'editProperties' && this.contentModified === true)) {
                    this.saveChanges()
                        .then<Form>(() => this.folderActions.getForm(this.currentItem.id))
                        .then(() => this.publishForm(true));
                } else {
                    this.publishForm(true);
                }
                break;
            default:
                throw new Error(`Undefined entity type "${this.currentItem.type}" with ID ${this.currentItem.id}.`);
        }
    }

    /**
     * Show an error message toast from GCMS UI code running form inside the iframe
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

    navigateToParentFolder(): Promise<boolean> {
        const currentItem = this.currentItem;
        if (currentItem.type === 'node' || currentItem.type === 'channel') {
            // Navigate to the node's root folder
            return this.navigationService.list(currentItem.id, currentItem.folderId).navigate();
        }

        const nodeId = this.appState.now.editor.nodeId;

        // Reload the item from the server (in case it was moved) and navigate to it
        return this.folderActions
            .getItem(currentItem.id, currentItem.type, { nodeId })
            .then(item => {
                let folderToNavigateTo = parentFolderOfItem(item);
                if (!folderToNavigateTo && currentItem.type === 'folder') {
                    folderToNavigateTo = currentItem.id;
                }
                return this.navigationService.list(nodeId, folderToNavigateTo).navigate();
            });
    }

    private publishPage(closeEditor: boolean): Promise<void> {
        const page = this.currentItem as Page;
        let publishReq: Promise<any>;
        // if page has timemanagement set or timemanagement setting request in queue so that publishing it would override those settings
        if ((PublishableStateUtil.statePlanned(page) && PublishableStateUtil.statePlannedOnline(page)) || PublishableStateUtil.stateInQueue(page)) {
            // The modal will handle refreshing the item list and closing the editor.
            publishReq = this.modalService.fromComponent(PublishTimeManagedPagesModal, {}, { pages: [ page ], allPages: 1, closeEditor })
                .then(modal => modal.open())
        } else {
            publishReq = this.folderActions.publishPages([ page ])
                .then(() => {
                    // refresh content list to update state changes
                    this.folderActions.refreshList('page');
                    if (closeEditor) {
                        this.closeEditor();
                    }
                });
        }

        // Set content modified state right before closing editor to avoid content changed modal.
        return publishReq.then(() => this.setContentModified(false));
    }

    private publishForm(closeEditor: boolean): Promise<void> {
        const form = this.currentItem as Form;
        return this.folderActions.publishForms([ form ])
            .then(() => {
                // refresh content list to update state changes
                this.folderActions.refreshList('form');
                if (closeEditor) {
                    this.closeEditor();
                }
            })
            .then(() => this.setContentModified(false));
    }

    /**
     * Takes the "detail" aux route params and uses these to call the correct editorActions method.
     */
    private updateEditorState(urlParams: EditorStateUrlParams): void {
        let { itemId, nodeId, type, editMode } = urlParams;
        itemId = Number(itemId);
        nodeId = Number(nodeId);

        const options = this.navigationService.deserializeOptions<EditorStateUrlOptions>(urlParams.options);
        switch (editMode) {
            case 'preview':
                switch (type) {
                    case 'page':
                        this.editorActions.previewPage(itemId, nodeId);
                        break;
                    case 'form':
                        this.editorActions.previewForm(itemId, nodeId);
                        break;
                    default:
                        throw new Error(`Type ${type} does not support content frame action ${editMode}.`);
                }
                break;

            case 'editProperties':
                this.editorActions.editProperties(itemId, type, nodeId, options.openTab, options.propertiesTab);
                break;

            case 'previewVersion':
                this.appState.dispatch(new PreviewPageVersionAction(itemId, nodeId, options.version));
                this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
                break;

            case 'compareVersionContents':
                this.appState.dispatch(new ComparePageVersionsAction(itemId, nodeId, options.oldVersion, options.version));
                this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
                break;

            case 'compareVersionSources':
                this.appState.dispatch(new ComparePageVersionSourcesAction(itemId, nodeId, options.oldVersion, options.version));
                this.appState.dispatch(new AddEditedEntityToRecentItemsAction());
                break;

            case 'edit':
                switch (type) {
                    case 'page':
                        this.editorActions.editPage(itemId, nodeId, options.compareWithId);
                        break;

                    case 'form':
                        this.editorActions.editForm(itemId, nodeId);
                        break;

                    case 'image':
                        this.editorActions.editImage(itemId, nodeId);
                        break;

                    default:
                        throw new Error(`Type "${type}" does not support content frame action "${editMode}".`);
                }
                break;

            default:
                break;
        }
    }

    /**
     * Tell the IFrameManager that the user initiated a close action.
     */
    closeEditor(): void {
        this.iframeManager.initiateUserClose();
        this.navigationService.instruction({ detail: null }).navigate();
    }

    /**
     * Save the page via the Aloha GCN plugin.
     * TODO: It would be more consistent to do this via the REST API, but this works for now.
     * Would need to discover how to get the edited page contents as a JS object which could be
     * posted to the `pages/save/id` endpoint.
     */
    private savePage(): Promise<any> {
        const win = this.iframe.nativeElement.contentWindow as CNWindow;
        this.customScriptHostService.pageStartsSaving();
        this.appState.dispatch(new StartSavingAction());

        return new Promise((resolve, reject) => {
            win.Aloha.GCN.savePage({
                createVersion: true,
                unlock: false,
                onsuccess: (returnValue: any) => {
                    this.appState.dispatch(new SaveSuccessAction());
                    this.customScriptHostService.pageWasSaved();
                    // refresh content list to update state changes
                    this.folderActions.refreshList('page');

                    this.notification.show({
                        message: 'message.page_saved',
                        type: 'success',
                        action: {
                            label: 'common.publish_button',
                            onClick: () => this.publishPage(true),
                        },
                    });

                    resolve(returnValue);
                },
                onfailure: (data: any, error: any) => {
                    this.appState.dispatch(new ListSavingErrorAction('page', error.errorMessage));
                    this.appState.dispatch(new SaveErrorAction(error.errorMessage));
                    this.errorHandler.catch(error, { notification: true });
                },
            });
        });
    }

    /**
     * Save current form object.
     */
    private saveForm(): Promise<any> {
        const id = this.currentItem.id;
        const payload = this.entityResolver.denormalizeEntity('form', this.currentItem as Form<Normalized>);
        this.appState.dispatch(new StartSavingAction());

        return this.api.forms.updateForm(id, payload).pipe(
            tap(() => {
                this.appState.dispatch(new SaveSuccessAction());
                this.folderActions.refreshList('form');

                this.notification.show({
                    message: 'message.form_saved',
                    type: 'success',
                    action: {
                        label: 'common.publish_button',
                        onClick: () => this.publishForm(true),
                    },
                });
            }),
            catchError((error) => {
                this.appState.dispatch(new ListSavingErrorAction('form', error.errorMessage));
                this.appState.dispatch(new SaveErrorAction(error.errorMessage));
                this.errorHandler.catch(error, { notification: true });
                return of(error);
            }),
        ).toPromise();
    }

    /**
     * Crop and resize the image, either modifying the original or creating a modified copy.
     */
    private cropAndResizeImage(createCopy: boolean): Promise<void> | undefined {
        const imageParams = this.customScriptHostService.getCropResizeParams();
        if (imageParams) {
            imageParams.copyFile = createCopy;
            this.appState.dispatch(new StartSavingAction());

            return this.folderActions.cropAndResizeImage(this.currentItem as Image<Normalized>, imageParams)
                .then((image: Image<Raw>) => {
                    this.appState.dispatch(new SaveSuccessAction());
                    this.markContentAsModifiedInState(false);
                    if (image) {
                        if (createCopy) {
                            // a new image was created, so reload this image now into the editor
                            this.editorActions.editImage(image.id, this.currentNode.id);
                        } else {
                            // we need to force a reload, else the gcnImagePlugin will start to
                            // error on subsequent saves.
                            this.iframeManager.reloadMasterFrame();
                        }
                    }
                })
                .catch(error => {
                    this.appState.dispatch(new SaveErrorAction(error.message));
                    this.markContentAsModifiedInState(false);
                });
        }
    }

    /**
     * Open Timemanagement modal
     *
     * @param item to set timemanagement configuration for
     */
    showTimeManagement(item: Page | Form): void {
        let promise: Promise<boolean | null> = Promise.resolve(true);

        if (this.contentModified) {
            // Before time management can be configured, a page just created needs to be saved at least once
            promise = this.modalService.fromComponent(
                ConfirmNavigationModal,
                { closeOnOverlayClick: false },
                { contentFrame: this, allowSaving: true },
            )
                .then(modal => modal.open());
        }

        promise.then(result => {
            if (typeof result === 'boolean') {

                setTimeout(() => {
                    this.modalService.fromComponent(TimeManagementModal, {}, { item, currentNodeId: this.currentNode.id })
                        .then(modal => modal.open())
                        .then(() => {
                            // prevent ConfirmNavigationModal pop up a second time triggered by route guard when TimeManagementModal closes editor
                            this.contentModified = false;
                            this.markContentAsModifiedInState(false);
                            // refresh folder content list to display new TimeManagement settings
                            this.folderActions.refreshList('page');
                            // then close content frame
                            this.navigationService.instruction({ detail: null }).navigate();
                        });
                }, 1);
            }
        });
    }

    /**
     * We need to keep track of a few things in the local state of this component.
     */
    private setLocalStateVars(state: EditorState): void {
        this.editorIsOpen = state.editorIsOpen;
        this.editorNodeId = state.nodeId;
        this.currentNode = this.entityResolver.getNode(state.nodeId);
        this.editMode = state.editMode;

        this.itemLanguage = this.getItemLanguage();
        this.pageComparisonLanguage = this.getPageComparisonLanguage();
        this.saveAsCopyButtonIsDisabled = this.determineSaveAsCopyButtonIsDisabled();
        this.saveButtonIsDisabled = this.determineSaveButtonIsDisabled();
        this.saveButtonVisible = this.editMode === 'edit'
            || (this.editMode === 'editProperties'
                && state.openTab === 'properties'
                && state.openPropertiesTab !== ITEM_TAG_LIST_TAB
            );
        this.isLocked = this.isLockedByAnother();

        if (state.editorIsOpen && state.itemId) {
            const item = this.entityResolver.getEntity(state.itemType, state.itemId);
            // Without the following check, saving a change that results in no update (e.g., deleting a file's extension)
            // would cause the first subsequent change to be restored immediately because of the following assignment.

            // Specific logic for Form entity
            // In 'edit' mode we do not update the form properties, just in 'editProperties' mode.
            if (
                this.currentItem && this.currentItem.id === state.itemId &&
                // ( this.contentModified || this.editorIsOpen ) &&
                this.currentItem.type === 'form' && state.editMode === 'edit'
            ) {
                return;
            }

            if (this.currentItem !== item && !deepEqual(this.currentItem, item)) {
                this.currentItem = item;
            }
        }

        this.previewLink = this.createPreviewLink();
        this.changeDetector.markForCheck();
    }

    private allTagsHaveConstructs(item: ItemWithObjectTags<Normalized> | Form<Normalized> | Node<Normalized>): boolean {
        const constructs = [];
        if (item.type === 'node' || item.type === 'form') {
            return true;
        } else {
            if ((item as ItemWithObjectTags).tags) {
                const itemWithTags = item as ItemWithObjectTags;
                for (const key of Object.keys(itemWithTags.tags)) {
                    const tag = itemWithTags.tags[key];
                    if (tag.construct) {
                        constructs.push(true);
                    } else {
                        constructs.push(false);
                    }
                }
            }
        }

        return constructs.every(hasConstruct => hasConstruct);
    }
}
