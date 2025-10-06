import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { EditMode } from '@gentics/cms-integration-api-models';
import {
    CmsFormType,
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
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ChangesOf, IBreadcrumbLink, IBreadcrumbRouterLink, ModalService } from '@gentics/ui-core';
import { Observable, Subscription, combineLatest } from 'rxjs';
import { map, publishReplay, refCount } from 'rxjs/operators';
import { EditorState, ITEM_PROPERTIES_TAB, SaveBehaviour } from '../../../common/models';
import { areItemsSaving } from '../../../common/utils/are-items-saving';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { PageVersionsModal } from '../../../shared/components';
import { BreadcrumbsService } from '../../../shared/providers';
import { PublishableStateUtil } from '../../../shared/util/entity-states';
import { ApplicationStateService, FocusListAction, FolderActionsService, SetFocusModeAction } from '../../../state';
import { AlohaIntegrationService } from '../../providers';

/** Used to define which buttons are visible at a certain moment. */
interface AvailableButtons {
    compareContents?: boolean;
    compareSources?: boolean;
    editItem?: boolean;
    edit?: boolean;
    editInheritance?: boolean;
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
    standalone: false,
})
export class EditorToolbarComponent implements OnInit, OnChanges, OnDestroy {

    public readonly SaveBehaviour = SaveBehaviour;
    public readonly EditMode = EditMode;
    public readonly ITEM_PROPERTIES_TAB = ITEM_PROPERTIES_TAB;
    public readonly CmsFormType = CmsFormType;

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

    public uploadInProgress$: Observable<boolean>;
    public alohaReady: boolean;

    public breadcrumbs: (IBreadcrumbLink | IBreadcrumbRouterLink)[] = [];
    public multilineExpanded: boolean;
    public buttons: AvailableButtons = {};
    public activeNodeLanguages: Language[];
    public isSaving: boolean;
    public inQueue: boolean;
    public focusMode: boolean;
    public brokenLinkCount = 0;

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
        protected client: GCMSRestClientService,
        protected breadcrumbsService: BreadcrumbsService,
        protected folderActions: FolderActionsService,
        protected permissions: PermissionService,
        protected aloha: AlohaIntegrationService,
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

        this.subscriptions.push(this.aloha.ready$.subscribe(ready => {
            this.alohaReady = ready;
            this.buttons = this.determineVisibleButtons();
            this.changeDetector.markForCheck();
        }));

        this.setUpBreadcrumbs(this.currentItem, this.currentNode?.id);
        this.checkIfInQueue();
        this.buttons = this.determineVisibleButtons();

        this.changeDetector.markForCheck();
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        if (changes.currentItem || changes.currentNode) {
            this.checkIfInQueue();
            this.setUpBreadcrumbs(this.currentItem, this.currentNode?.id);
        }
        if (changes.editorState || changes.currentItem || changes.currentNode || changes.itemPermissions || changes.locked) {
            this.buttons = this.determineVisibleButtons();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    logoClick(): void {
        this.folderActions.setSearchTerm('');
        this.appState.dispatch(new FocusListAction());
    }

    updateBrokenLinkCount(count: number): void {
        this.brokenLinkCount = count;
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
        this.subscriptions.push(this.client.folder.breadcrumbs(folderId, { nodeId }).pipe(
            map(response => response.folders.map((folder) => ({
                text: folder.name,
                route: ['/editor', { outlets: { list: ['node', nodeId, 'folder', folder.id] }} ],
            } as IBreadcrumbLink | IBreadcrumbRouterLink))),
            map((breadcrumbs) => !this.multilineExpanded
                ? this.breadcrumbsService.addTooltip(breadcrumbs)
                : breadcrumbs,
            ),
        ).subscribe(breadcrumbs => {
            this.breadcrumbs = breadcrumbs ;
            this.changeDetector.markForCheck();
        }));
    }

    checkIfInQueue(): void {
        if (this.currentItem?.type !== 'page') {
            this.inQueue = false;
            return;
        }

        const page = this.currentItem as Page;
        this.subscriptions.push(this.permissions.forItem(page.id, 'page', this.currentNode.id).pipe(
            map(permissions => permissions.publish && PublishableStateUtil.stateInQueue(page)),
        ).subscribe(inQueue => {
            this.inQueue = inQueue;
            this.changeDetector.markForCheck();
        }));
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

    previewPage(): void {
        this.navigationService
            .detailOrModal(this.currentNode?.id, this.currentItem.type, this.currentItem.id, EditMode.PREVIEW)
            .navigate();
    }

    editInheritance(): void {
        this.navigationService
            .detailOrModal(this.currentNode?.id, this.currentItem.type, this.currentItem.id, EditMode.EDIT_INHERITANCE)
            .navigate();
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
    public determineVisibleButtons(): AvailableButtons {
        const type = this.currentItem?.type;
        const isPage = type === 'page';
        const isForm = type === 'form';
        const editMode = this.editorState?.editMode;
        const previewing = editMode === EditMode.PREVIEW;
        const editing = editMode === EditMode.EDIT;
        const propertiesTab = editMode === EditMode.EDIT_PROPERTIES && this.editorState.openTab;
        const isInherited = (this.currentItem as InheritableItem)?.inherited;
        const userCan = this.itemPermissions || { edit: false, view: false };
        const canPublish = !!(isPage || (isForm && (this.itemPermissions as FormPermissions)?.publish));

        return {
            compareContents: (isPage || isForm) && editMode === EditMode.COMPARE_VERSION_SOURCES,
            compareSources: (isPage || isForm) && editMode === EditMode.COMPARE_VERSION_CONTENTS,
            editItem: (editMode === EditMode.EDIT_PROPERTIES || editMode === EditMode.EDIT_INHERITANCE)
                && (isPage || isForm)
                && userCan.edit
                && !this.locked,
            edit: (isPage || isForm) && previewing && userCan.edit && !this.locked,
            // TODO: Check for partial inheritance
            editInheritance: isPage && userCan.edit && editMode !== EditMode.EDIT_INHERITANCE && !isInherited,
            editProperties: editMode !== EditMode.EDIT_PROPERTIES && userCan.view && !this.locked,
            lockedEdit: (isPage || isForm) && this.locked && userCan.edit && !this.showSave,
            previewPage: (isPage || isForm) && !previewing && userCan.view,
            publish: (editing || previewing || propertiesTab) && canPublish && userCan.edit && !isInherited,
            saveAsCopy: editing && type === 'image' && userCan.edit,
            takeOffline: canPublish && (this.currentItem as Page | Form).online && !isInherited,
            timeManagement: userCan.edit && canPublish,
            versionHistory: isPage,
        };
    }
}
