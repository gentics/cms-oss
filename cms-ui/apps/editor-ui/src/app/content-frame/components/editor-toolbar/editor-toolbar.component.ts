import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { EditorState, SaveBehaviour } from '@editor-ui/app/common/models';
import { areItemsSaving } from '@editor-ui/app/common/utils/are-items-saving';
import { DecisionModalsService } from '@editor-ui/app/core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { PageVersionsModal } from '@editor-ui/app/shared/components';
import { BreadcrumbsService } from '@editor-ui/app/shared/providers';
import { PublishableStateUtil } from '@editor-ui/app/shared/util/entity-states';
import { ApplicationStateService, FocusListAction, FolderActionsService, SetFocusModeAction } from '@editor-ui/app/state';
import {
    EditMode,
    File,
    Folder,
    Form,
    FormPermissions,
    Image,
    InheritableItem,
    ItemNormalized,
    ItemPermissions,
    Language,
    Node,
    Normalized,
    Page,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { IBreadcrumbLink, IBreadcrumbRouterLink, ModalService } from '@gentics/ui-core';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { map, publishReplay, refCount } from 'rxjs/operators';

/** Used to define which buttons are visible at a certain moment. */
interface AvailableButtons {
    compareContents?: boolean;
    compareSources?: boolean;
    editItem?: boolean;
    edit?: boolean;
    editProperties?: boolean;
    lockedEdit?: boolean;
    previewPage?: boolean;
    publish?: boolean;
    saveAsCopy?: boolean;
    takeOffline?: boolean;
    timeManagement?: boolean;
    versionHistory?: boolean;
}

@Component({
    selector: 'gtx-editor-toolbar',
    templateUrl: './editor-toolbar.component.html',
    styleUrls: ['./editor-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditorToolbarComponent implements OnInit, OnChanges, OnDestroy {

    public readonly SaveBehaviour = SaveBehaviour;
    public readonly EditMode = EditMode;

    @Input()
    public nodeInherited: boolean;

    @Input()
    public currentNode: Node;

    @Input()
    public currentItem: ItemNormalized;

    @Input()
    public itemPermissions: ItemPermissions;

    @Input()
    public itemLanguage: Language;

    @Input()
    public locked: boolean;

    @Input()
    public showSave: boolean;

    @Input()
    public saveDisabled: boolean;

    @Input()
    public copyDisabled: boolean;

    @Input()
    public editorState: EditorState;

    @Output()
    public close = new EventEmitter<void>();

    @Output()
    public save = new EventEmitter<SaveBehaviour>();

    @Output()
    public timeManagement = new EventEmitter<ItemNormalized>();

    /*
    *  Regular GCMS UI Properties
    */
    public uploadInProgress$: Observable<boolean>;
    public breadcrumbs: (IBreadcrumbLink | IBreadcrumbRouterLink)[] = [];
    public multilineExpanded: boolean;
    public buttons: AvailableButtons = {};
    public activeNodeLanguages: Language[];
    public isSaving: boolean;
    public inQueue: boolean;
    public focusMode: boolean;

    /** Subscriptions to cleanup */
    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected appState: ApplicationStateService,
        protected userSettings: UserSettingsService,
        protected entityResolver: EntityResolver,
        protected navigationService: NavigationService,
        protected modalService: ModalService,
        protected decisionModals: DecisionModalsService,
        protected api: GcmsApi,
        protected breadcrumbsService: BreadcrumbsService,
        protected folderActions: FolderActionsService,
        protected permissions: PermissionService,
    ) {}

    ngOnInit(): void {
        this.uploadInProgress$ = this.appState.select(state => state.editor).pipe(
            map(editorState => editorState.uploadInProgress),
            publishReplay(1),
            refCount(),
        );

        this.subscriptions.push(combineLatest([
            this.appState.select(state => state.editor.focusMode),
            this.appState.select(state => state.editor.editMode),
            this.appState.select(state => state.editor.editorIsOpen),
            this.appState.select(state => state.editor.editorIsFocused),
        ]).subscribe(([active, editMode, open, focused]) => {
            this.focusMode = active && editMode === EditMode.EDIT && open && focused;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.ui.contentFrameBreadcrumbsExpanded).subscribe(expanded => {
            this.multilineExpanded = expanded;
            this.changeDetector.markForCheck();
        }));

        // TODO: this is wrong - the contentFrame may be open when another node is navigated to,
        // so the list of languages would be wrong. Need another way to get the list for the current editor node.
        this.subscriptions.push(this.appState.select(state => state.folder.activeNodeLanguages.list).pipe(
            map(languageIds => languageIds.map(id => this.entityResolver.getLanguage(id))),
        ).subscribe(languages => {
            this.activeNodeLanguages = languages;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(areItemsSaving).subscribe(isSaving => {
            this.isSaving = isSaving;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.editorState) {
            this.buttons = this.determineVisibleButtons();
        }
        if (changes.currentItem) {
            this.checkIfInQueue();
        }
        if (changes.currentItem || changes.currentNode) {
            this.setUpBreadcrumbs(this.currentItem, this.currentNode?.id);
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    logoClick(): void {
        this.folderActions.setSearchTerm('');
        this.appState.dispatch(new FocusListAction());
    }

    setUpBreadcrumbs(item: Page | File | Folder | Form | Image | Node | undefined, nodeId: number): void {
        let folderId: number = null;
        if (!item) {
            this.breadcrumbs = [];
            return;
        }
        if (item.type === 'image' || item.type === 'page' || item.type === 'form') {
            folderId = item.folderId;
        } else if (item.type === 'folder') {
            // If the folder does not have a motherId, it is the root folder of a node, so we use its ID.
            folderId = item.motherId || item.id;
        }
        if (folderId == null || !nodeId) {
            return;
        }
        this.subscriptions.push(this.api.folders.getBreadcrumbs(folderId, { nodeId }).pipe(
            map(response => response.folders.map((folder) => ({
                text: folder.name,
                route: ['/editor', { outlets: { list: ['node', nodeId, 'folder', folder.id] }} ],
            } as IBreadcrumbLink | IBreadcrumbRouterLink))),
            map((breadcrumbs) => !this.multilineExpanded
                ? this.breadcrumbsService.addTooltip(breadcrumbs)
                : breadcrumbs,
            ),
        ).subscribe(breadcrumbs => {
            this.breadcrumbs = breadcrumbs as any;
            this.changeDetector.markForCheck();
        }));
    }

    checkIfInQueue(): void {
        if (this.currentItem?.type !== 'page') {
            this.inQueue = false;
            return;
        }

        const page = this.currentItem as Page;
        this.subscriptions.push(this.permissions.forItem(page.id, 'page', this.currentNode.id)
            .map(permissions => permissions.publish)
            .pipe(
                map(permissions => permissions && PublishableStateUtil.stateInQueue(page)),
            ).subscribe(inQueue => {
                this.inQueue = inQueue;
                this.changeDetector.markForCheck();
            }),
        );
    }

    expandedChanged(multilineExpanded: boolean): void {
        this.userSettings.setContentFrameBreadcrumbsExpanded(multilineExpanded);
        this.changeDetector.detectChanges();
    }

    changeFocus(): void {
        this.appState.dispatch(new FocusListAction());
    }

    setFocusMode(enabled: boolean): void {
        this.appState.dispatch(new SetFocusModeAction(enabled));
        this.userSettings.setFocusMode(enabled);
    }

    approve(): void {
        const page = this.currentItem as Page;
        this.folderActions.pageQueuedApprove([page]);
    }

    /**
     * Switch from "compare sources" view to "compare contents".
     */
    switchToCompareSources(): void {
        const options = {
            version: this.editorState.version,
            oldVersion: this.editorState.oldVersion,
        };
        this.navigationService
            .detailOrModal(this.currentNode.id, 'page', this.currentItem.id, EditMode.COMPARE_VERSION_SOURCES, options)
            .navigate();
    }

    /**
     * Switch from "compare sources" view to "compare contents".
     */
    switchToCompareContents(): void {
        const options = {
            version: this.editorState.version,
            oldVersion: this.editorState.oldVersion,
        };
        this.navigationService
            .detailOrModal(this.currentNode.id, 'page', this.currentItem.id, EditMode.COMPARE_VERSION_CONTENTS, options)
            .navigate();
    }

    /**
     * Opens the "page version" modal for the current page.
     */
    showPageVersionsModal(): void {
        const options = { page: this.currentItem as Page, nodeId: this.currentNode.id };
        this.modalService.fromComponent(PageVersionsModal, null, options)
            .then(modal => modal.open());
    }

    showTimeManagement(): void {
        this.timeManagement.emit(this.currentItem);
    }

    /**
     * Switch the state to edit object properties mode.
     */
    editProperties(): void {
        this.decisionModals.showInheritedDialog(this.currentItem, this.currentNode.id)
            .then((data) => {
                if (!data) {
                    return;
                }

                this.navigationService.detailOrModal(data.nodeId, data.item.type, data.item.id, EditMode.EDIT_PROPERTIES).navigate();
            });
    }

    /**
     * Switch the state to edit mode.
     */
    editItem(): void {
        switch (this.currentItem.type) {
            case 'page':
                this.decisionModals.showInheritedDialog(this.currentItem, this.currentNode.id)
                    .then(({item, nodeId}) => this.navigationService.detailOrModal(nodeId, 'page', item.id, EditMode.EDIT).navigate());
                break;
            case 'form':
                this.editForm();
                break;
            case 'image':
                this.navigationService.detailOrModal(this.currentNode.id, 'image', this.currentItem.id, EditMode.EDIT).navigate();
                break;
            default:
                throw new Error('Incompatible item to edit.');
        }
    }

    /**
     * Switch the state to form edit mode.
     */
    editForm(): void {
        this.navigationService.detailOrModal(this.currentNode.id, 'form', this.currentItem.id, EditMode.EDIT).navigate();
    }

    /**
     * Tell the IFrameManager that the user initiated a close action.
     */
    closeEditor(): void {
        this.close.emit();
    }

    saveChanges(behaviour: SaveBehaviour): void {
        this.save.emit(behaviour);
    }

    takeItemOffline(): void {
        switch (this.currentItem.type) {
            case 'page':
                this.decisionModals.selectPagesToTakeOffline([this.currentItem as Page<Normalized>])
                    .then(result => this.folderActions.takePagesOffline(result));
                break;

            case 'form':
                this.folderActions.takeFormsOffline([this.currentItem.id]);
                break;
            default:
                throw new Error(`Undefined entity type "${this.currentItem.type}" with ID ${this.currentItem.id}.`);
        }
    }

    /**
     * Shifts the logic for determining which buttons to display out of the template.
     */
    private determineVisibleButtons(): AvailableButtons {
        const type = this.currentItem && this.currentItem.type;
        const isPage = type === 'page';
        const isForm = type === 'form';
        const editMode = this.editorState?.editMode;
        const previewing = editMode === 'preview';
        const editing = editMode === 'edit';
        const propertiesTab = editMode === 'editProperties' && this.editorState.openTab;
        const isInherited = this.currentItem && (this.currentItem as InheritableItem).inherited;
        const userCan = this.itemPermissions || { edit: false, view: false };
        const canPublish = !!(isPage || (isForm && (this.itemPermissions as FormPermissions).publish));

        return {
            compareContents: (isPage || isForm) && editMode === 'compareVersionSources',
            compareSources: (isPage || isForm) && editMode === 'compareVersionContents',
            editItem: editMode === 'editProperties' && (isPage || isForm) && userCan.edit && !this.locked,
            edit: (isPage || isForm) && previewing && userCan.edit && !this.locked,
            editProperties: editMode !== 'editProperties' && userCan.view && !this.locked,
            lockedEdit: (isPage || isForm) && this.locked && userCan.edit,
            previewPage: (isPage || isForm) && !previewing && userCan.view,
            publish: (editing || previewing || propertiesTab) && canPublish && userCan.edit && !isInherited,
            saveAsCopy: editing && type === 'image' && userCan.edit,
            takeOffline: canPublish && (this.currentItem as Page | Form).online && !isInherited,
            timeManagement: userCan.edit && canPublish,
            versionHistory: isPage,
        };
    }
}
