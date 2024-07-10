import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { EditorPermissions, UIMode } from '@editor-ui/app/common/models';
import { Folder, FolderPermissions, StagedItemsMap, TemplatePermissions } from '@gentics/cms-models';
import { ContextMenuOperationsService } from '../../providers/context-menu-operations/context-menu-operations.service';
import { NavigationService } from '../../providers/navigation/navigation.service';

/**
 * Context menu for folders. Intended to operate on the currently-open folder in the list view.
 */
@Component({
    selector: 'folder-context-menu',
    templateUrl: './folder-context-menu.component.html',
    styleUrls: ['./folder-context-menu.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FolderContextMenuComponent implements OnChanges {

    readonly UIMode = UIMode;

    @Input()
    public folder: Folder;

    @Input()
    public activeNodeId: number;

    @Input()
    public permissions: EditorPermissions;

    @Input()
    public uiMode: UIMode = UIMode.EDIT;

    @Input()
    public stagingMap: StagedItemsMap;

    isBaseFolder = false;
    buttons: { [key: string]: boolean };

    constructor(
        private contextMenuOperations: ContextMenuOperationsService,
        private navigationService: NavigationService,
    ) { }

    ngOnChanges(changes: SimpleChanges): void {
        if (Object.keys(changes).length > 0) {
            this.isBaseFolder = this.folder ? this.folder.motherId === undefined : false;
            this.buttons = this.determineVisibleButtons();
        }
    }

    nodePropertiesClicked(): void {
        this.contextMenuOperations.editNodeProperties(this.activeNodeId);
    }

    propertiesClicked(): void {
        this.contextMenuOperations.editProperties(this.folder, this.activeNodeId);
    }

    localizeClicked(): void {
        this.contextMenuOperations.localize(this.folder, this.activeNodeId);
    }

    inheritanceClicked(): void {
        this.contextMenuOperations.setInheritance(this.folder, this.activeNodeId);
    }

    synchronizeClicked(): void {
        this.contextMenuOperations.synchronizeChannel(this.folder);
    }

    copyClicked(): void {
        this.contextMenuOperations.copyItems('folder', [this.folder], this.activeNodeId);
    }

    moveClicked(): void {
        this.contextMenuOperations.moveItems('folder', [this.folder], this.activeNodeId, this.folder.motherId);
    }

    linkToTemplatesClicked(): void {
        this.contextMenuOperations.linkTemplatesToFolder(this.folder.nodeId, this.folder.id);
    }

    deleteClicked(): void {
        this.contextMenuOperations.deleteItems('folder', [this.folder], this.activeNodeId)
            .then(deletedIds => {
                if (deletedIds) {
                    this.navigationService.list(this.activeNodeId, this.folder.motherId).navigate();
                }
            });
    }

    stagingClicked(recursive: boolean): void {
        if (this.stagingMap?.[this.folder.globalId]?.included) {
            this.contextMenuOperations.unstageItemFromCurrentPackage(this.folder);
        } else {
            this.contextMenuOperations.stageItemToCurrentPackage(this.folder, recursive);
        }
    }

    private determineVisibleButtons(): { [key: string]: boolean } {
        if (!this.permissions) {
            return {};
        }
        const inherited = this.folder ? this.folder.inherited : false;
        const isMaster = this.folder ? this.folder.isMaster : false;
        const isLocalized = !isMaster && !inherited && !this.isBaseFolder;
        const userCan: FolderPermissions & TemplatePermissions = { ...this.permissions.folder, ...this.permissions.template };

        // Items can be synchronized to master when they are inside a folder
        // of an inherited channel and are not inherited (localized & local is both OK!).
        const canBeSynchronizedToParentNode = !inherited
            && this.permissions.synchronizeChannel
            && (this.folder.nodeId !== this.folder.inheritedFromId);

        return {
            nodeProperties: this.isBaseFolder,
            properties: true,
            localize: inherited && userCan.localize,
            move: isMaster && !inherited && userCan.delete,
            inheritanceSettings: isMaster && !inherited && userCan.inherit,
            synchronizeChannel: canBeSynchronizedToParentNode,
            linkTemplates: userCan.link,
            delete: isMaster && !inherited && userCan.delete,
            unlocalize: isLocalized && userCan.unlocalize,
            staging: userCan.view,
        };
    }
}
