import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges } from '@angular/core';
import { EditorPermissions, UIMode } from '@editor-ui/app/common/models';
import { ApplicationStateService } from '@editor-ui/app/state';
import { Feature, Folder, FolderPermissions, StagedItemsMap, TemplatePermissions } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
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
    standalone: false
})
export class FolderContextMenuComponent implements OnInit, OnChanges, OnDestroy {

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
    buttons: { [key: string]: boolean } = {};
    multiChannelingEnabled = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private contextMenuOperations: ContextMenuOperationsService,
        private navigationService: NavigationService,
        private appState: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select(state => state.features[Feature.MULTICHANNELLING]).subscribe(enabled => {
            this.multiChannelingEnabled = enabled;
            this.buttons = this.determineVisibleButtons();
            this.changeDetector.markForCheck();
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (Object.keys(changes).length > 0) {
            this.isBaseFolder = this.folder ? this.folder.motherId === undefined : false;
            this.buttons = this.determineVisibleButtons();
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
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
            localize: this.multiChannelingEnabled && inherited && userCan.localize,
            move: isMaster && !inherited && userCan.delete,
            inheritanceSettings: this.multiChannelingEnabled && isMaster && !inherited && userCan.inherit,
            synchronizeChannel: this.multiChannelingEnabled && canBeSynchronizedToParentNode,
            linkTemplates: userCan.link,
            delete: isMaster && !inherited && userCan.delete,
            unlocalize: this.multiChannelingEnabled && isLocalized && userCan.unlocalize,
            staging: userCan.view,
        };
    }
}
