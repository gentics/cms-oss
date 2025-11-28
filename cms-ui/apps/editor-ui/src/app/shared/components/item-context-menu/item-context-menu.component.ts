import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import {
    Feature,
    File as FileModel,
    Folder,
    Form,
    Image,
    InheritableItem,
    Item,
    ItemPermissions,
    LocalizationType,
    Node as NodeModel,
    Page,
    StagedItemsMap,
} from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { EditorPermissions, StageableItem, StagingMode, UIMode, getNoPermissions, plural } from '../../../common/models';
import { isEditableImage } from '../../../common/utils/is-editable-image';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService } from '../../../state';

export interface ContextMenuButtonsMap {
    edit: boolean;
    properties: boolean;
    copy: boolean;
    createVariation: boolean;
    pageVersions: boolean;
    publishProtocol: boolean;
    setAsStartpage: boolean;
    localize: boolean;
    editInheritance: boolean;
    editInParent: boolean;
    move: boolean;
    inheritanceSettings: boolean;
    synchronizeChannel: boolean;
    requestTranslation: boolean;
    linkTemplates: boolean;
    delete: boolean;
    restore: boolean;
    unlocalize: boolean;
    takeOffline: boolean;
    publish: boolean;
    publishLanguageVariants: boolean;
    timeManagement: boolean;
    stage: boolean;
    stageRecursive: boolean;
    stageAllLanguages: boolean;
}

@Component({
    selector: 'item-context-menu',
    templateUrl: './item-context-menu.component.html',
    styleUrls: ['./item-context-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ItemContextMenuComponent implements OnInit, OnChanges, OnDestroy {

    readonly UIMode = UIMode;
    readonly StagingMode = StagingMode;
    readonly plural = plural;

    @Input()
    public item: InheritableItem;

    @Input()
    public isFolderStartPage = false;

    @Input()
    public isLocked = false;

    @Input()
    public isDeleted = false;

    @Input()
    public permissions: EditorPermissions = getNoPermissions();

    @Input()
    public activeNode: NodeModel = undefined;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    buttons: ContextMenuButtonsMap;
    wastebinEnabled = false;
    multiChannelingEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private contextMenuOperations: ContextMenuOperationsService,
        private folderActions: FolderActionsService,
        private state: ApplicationStateService,
        private entityResolver: EntityResolver,
    ) { }

    ngOnInit(): void {
        this.subscriptions.push(this.state.select((state) => state.features[Feature.WASTEBIN]).subscribe((enabled) => {
            this.wastebinEnabled = enabled;
            this.buttons = this.determineVisibleButtons();
            this.changeDetector.markForCheck();
        }));
        this.subscriptions.push(this.state.select((state) => state.features[Feature.MULTICHANNELLING]).subscribe((enabled) => {
            this.multiChannelingEnabled = enabled;
            this.buttons = this.determineVisibleButtons();
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (Object.keys(changes).length > 0) {
            this.buttons = this.determineVisibleButtons();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    /**
     * Returns true if the item is a Page with more than a single language variant.
     */
    pageHasLanguageVariants(item: Item): boolean {
        switch (item.type) {
            case 'page': {
                const page = item as Page;
                return page.languageVariants && Object.keys(page.languageVariants).length > 1;
            }

            case 'form': {
                const form = item as Form;
                return Array.isArray(form.languages) && form.languages.length > 1;
            }

            default:
                return false;
        }
    }

    editClicked(item: InheritableItem): void {
        if (this.isLocked) {
            return;
        }
        this.contextMenuOperations.editItem(item, this.activeNode.id);
    }

    propertiesClicked(item: InheritableItem): void {
        if (this.isLocked) {
            return;
        }
        this.contextMenuOperations.editProperties(item, this.activeNode.id);
    }

    versionsClicked(page: Page): void {
        this.contextMenuOperations.listPageVersions(page, this.activeNode.id);
    }

    publishProtocolClicked(item: Page | Form): void {
        this.contextMenuOperations.openPublishProtocol(item);
    }

    setAsStartpageClicked(page: Page): void {
        const currentFolderId = this.state.now.folder.activeFolder;
        this.contextMenuOperations.setAsStartpage(page, currentFolderId);
    }

    localizeClicked(item: InheritableItem): void {
        this.contextMenuOperations.localize(item, this.activeNode.id);
    }

    editInheritance(item: Page): void {
        this.contextMenuOperations.editInheritance(item, this.activeNode.id);
    }

    editInParentNode(item: InheritableItem): void {
        this.contextMenuOperations.editInParentNode(item);
    }

    inheritanceClicked(item: InheritableItem): void {
        this.contextMenuOperations.setInheritance(item, this.activeNode.id);
    }

    synchronizeClicked(item: Folder | Page | FileModel | Image): void {
        this.contextMenuOperations.synchronizeChannel(item);
    }

    takeOfflineClicked(item: Page | Form): void {
        switch (item.type) {
            case 'page': {
                const page: Page = item;
                this.contextMenuOperations.takePagesOffline([page]);
                break;
            }

            case 'form': {
                const form: Form = item;
                this.contextMenuOperations.takeFormsOffline([form]);
                break;
            }

            default:
                throw new Error('Behavior not defined for item type.');
        }
    }

    publishClicked(item: Page | Form): void {
        if (this.isLocked) {
            return;
        }

        switch (item.type) {
            case 'page': {
                const page: Page = item;
                this.contextMenuOperations.publishPages([page]);
                break;
            }

            case 'form': {
                const form: Form = item;
                this.contextMenuOperations.publishForms([form]);
                break;
            }

            default:
                throw new Error('Behavior not defined for item type.');
        }
    }

    publishLanguageVariantsClicked(page: Page): void {
        this.contextMenuOperations.publishPages([page], true);
    }

    timeManagementClicked(item: Page | Form): void {
        this.contextMenuOperations.showTimeManagement(item, this.activeNode.id);
    }

    copyClicked(item: InheritableItem): void {
        this.contextMenuOperations.copyItems(item.type, [item], this.activeNode.id);
    }

    moveClicked(item: InheritableItem): void {
        const currentFolderId = this.state.now.folder.activeFolder;
        this.contextMenuOperations.moveItems(item.type, [item], this.activeNode.id, currentFolderId);
    }

    createVariationClicked(page: Page): void {
        this.contextMenuOperations.createVariationClicked(page, this.activeNode.id);
    }

    deleteClicked(item: InheritableItem): void {
        if (this.isLocked) {
            return;
        }

        this.contextMenuOperations.deleteItems(item.type, [item], this.activeNode.id)
            .then((removedItemIds) => this.folderActions.refreshList(item.type));
    }

    restoreClicked(item: InheritableItem): void {
        // `refreshList`is executed from within WastebinActions.restoreItemsFromWastebin
        this.contextMenuOperations.restoreItems([item]);
    }

    requestTranslation(page: Page): void {
        this.contextMenuOperations.requestTranslation(page.id, this.activeNode.id);
    }

    linkToTemplatesClicked(): void {
        this.contextMenuOperations.linkTemplatesToFolder(this.activeNode.id, this.item.id);
    }

    /**
     * If has permission to publish and state is planned return true
     */
    isInQueue(item: InheritableItem): boolean {
        if (item && item.type === 'page' && this.permissions.page) {
            return this.permissions.page.publish && this.findQueuedPages(item as Page).length > 0;
        } else {
            return false;
        }
    }

    approveClicked(page: Page): void {
        this.folderActions.pageQueuedApprove(this.findQueuedPages(page));
    }

    /**
     * @param page object
     * @returns The language variant pages are in queued of the page
     */
    findQueuedPages(page: Page): Page[] {
        const pages = [];

        if (page?.languageVariants) {
            Object.values(page.languageVariants).forEach((pageId) => {
                const page = this.entityResolver.getPage(pageId);
                if (page.queued === true) {
                    pages.push(page);
                }
            });
        }

        return pages;
    }

    stageClicked(item: StageableItem, mode: StagingMode): void {
        if (this.stagingMap?.[item.globalId]?.included) {
            if (item.type === 'page') {
                this.contextMenuOperations.unstagePageFromCurrentPackage(item as Page, mode === StagingMode.ALL_LANGUAGES);
            } else {
                this.contextMenuOperations.unstageItemFromCurrentPackage(item);
            }
            return;
        }

        if (item.type === 'page') {
            this.contextMenuOperations.stagePageToCurrentPackage(item as Page, mode === StagingMode.ALL_LANGUAGES);
        } else {
            this.contextMenuOperations.stageItemToCurrentPackage(item, mode === StagingMode.RECURSIVE);
        }
    }

    private determineVisibleButtons(): ContextMenuButtonsMap {
        const type = this.item && this.item.type;
        const isPage = type === 'page';
        const isFolder = type === 'folder';
        const isForm = type === 'form';
        const isFile = type === 'file';
        const isImage = type === 'image';
        const inherited = this.item ? this.item.inherited : false;
        let isMaster = this.item ? ((<Folder> this.item).isMaster || (<Page> this.item).master) : false;
        if (type === 'file' || type === 'image') {
            // Files don't provide isMaster/master properties
            isMaster = isMaster || (!inherited && this.item.inheritedFrom === this.item.masterNode);
        }

        const isLocalized = !isMaster && !inherited;
        const userCan: ItemPermissions = (<any> this.permissions)[type];
        const canPublish = (isPage || isForm) && userCan.edit && !inherited;
        const templatePermissions = this.permissions['template'];

        // Items can be synchronized to master when they are inside a folder
        // of an inherited channel and are not inherited (localized & local is both OK!).
        const canBeSynchronizedToParentNode = !inherited
          && this.permissions.synchronizeChannel
          && this.activeNode
          && (this.activeNode.id !== this.activeNode.inheritedFromId);

        const showEditButton = (isPage || isImage || isForm)
          && userCan.edit
          && isEditableImage(this.item as Image);

        const showRequestTranslationButton = isPage
          && this.activeNode.languagesId
          && this.activeNode.languagesId.length > 1
          && this.state.now.tools.available.some((tool) => tool.key.startsWith('task-management'));

        return {
            edit: !this.isDeleted && showEditButton,
            properties: !this.isDeleted,
            copy: !this.isDeleted && (isPage || isFile || isImage || isForm),
            createVariation: isPage && !this.isDeleted && userCan.create,
            pageVersions: isPage && !this.isDeleted && userCan.view,
            publishProtocol: (isPage || isForm) && !this.isDeleted && userCan.view,
            setAsStartpage: isPage && !this.isDeleted && isPage && !this.isFolderStartPage && this.permissions.folder.edit,
            localize: this.multiChannelingEnabled && !isForm && !this.isDeleted && inherited && userCan.localize,
            editInheritance: this.multiChannelingEnabled
              && isPage
              && !this.isDeleted
              && isLocalized
              && userCan.edit
              && (this.item as Page).localizationType === LocalizationType.PARTIAL,
            editInParent: this.multiChannelingEnabled && !isForm && !this.isDeleted && showEditButton && (inherited || isLocalized),
            move: !this.isDeleted && (isForm || (isMaster && !inherited)) && userCan.delete,
            inheritanceSettings: this.multiChannelingEnabled && !isForm && !this.isDeleted && isMaster && !inherited && userCan.inherit,
            synchronizeChannel: this.multiChannelingEnabled && !isForm && !this.isDeleted && canBeSynchronizedToParentNode,
            requestTranslation: !this.isDeleted && showRequestTranslationButton,
            linkTemplates: !this.isDeleted && isFolder && userCan.edit && templatePermissions ? templatePermissions.link : false,
            delete: !this.isDeleted && (isMaster || isForm) && !inherited && userCan.delete,
            restore: this.wastebinEnabled && this.isDeleted && !inherited && userCan.delete,
            unlocalize: this.multiChannelingEnabled && !isForm && !this.isDeleted && isLocalized && userCan.unlocalize,
            takeOffline: this.hasOnlineItem(this.item, isPage, isForm, inherited) && canPublish,
            publish: !this.isDeleted && ((isPage && canPublish) || (isForm && this.permissions.form.publish)),
            publishLanguageVariants: !isForm && isPage && !this.isDeleted && this.hasLanguageVariants(this.item as Page) && canPublish,
            timeManagement: ((isForm && this.permissions.form.publish) || isPage) && !this.isDeleted && userCan.edit,
            stage: !this.isDeleted && userCan.view,
            stageRecursive: !this.isDeleted && userCan.view && isFolder,
            stageAllLanguages: !this.isDeleted && userCan.view && (isPage || isForm),
        };
    }

    private hasOnlineItem(item: InheritableItem, isPage: boolean, isForm: boolean, inherited: boolean): boolean {
        if (isPage) {
            const page = item as Page;
            if (page?.online && !page.queued) {
                return !this.isDeleted && !inherited;
            }
        }
        let hasOnlineItem = false;
        if (isPage && (item as Page)?.languageVariants) {
            Object.values((item as Page).languageVariants).every((pageId) => {
                if (hasOnlineItem) {
                    return false;
                }
                const page = this.entityResolver.getPage(pageId);
                if (page?.online && !page.queued) {
                    hasOnlineItem = true;
                }
                return true;
            });
        } else if (isForm && this.permissions.form.publish && (this.item as Form).online) {
            hasOnlineItem = true;
        }
        return !this.isDeleted && hasOnlineItem && !inherited;
    }

    private hasLanguageVariants(translatableItem: Page): boolean {
        return translatableItem.languageVariants instanceof Object && Object.keys(translatableItem.languageVariants).length > 0;
    }

}
