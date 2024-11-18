import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ContentRepositoryType, EditableFolderProps, Folder, FolderCreateRequest } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-folder-modal',
    templateUrl: './create-folder-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateFolderModalComponent extends BaseModal<Folder> implements OnInit, OnDestroy {

    public control: FormControl<EditableFolderProps>;
    public loading = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select(state => state.folder.folders.creating).subscribe(creating => {
            this.loading = creating;

            if (this.control) {
                if (this.loading && this.control.enabled) {
                    this.control.disable();
                } else if (!this.loading && !this.control.enabled) {
                    this.control.enable();
                }
            }

            this.changeDetector.markForCheck();
        }));

        const properties: EditableFolderProps = {};

        const activeNodeId = this.appState.now.folder.activeNode;
        const activeFolderId = this.appState.now.folder.activeFolder;

        /** if cr is of type Mesh prefilling folder directory has another logic in contrast to MySQL cr.
         * @see https://jira.gentics.com/browse/GTXPE-590
         */
        const crIsMesh = this.contentRepositoryTypeIsMesh(activeFolderId);

        properties.publishDir = '';

        if (!!activeNodeId && !!activeFolderId && !crIsMesh) {
            const activeNode = this.appState.now.entities.node[activeNodeId];
            if (!!activeNode && !activeNode.pubDirSegment) {
                properties.publishDir = this.entityResolver.getFolder(activeFolderId).publishDir;
            }
        }

        this.control = new FormControl(properties);
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    saveChanges(): void {
        if (!this.control.valid || this.loading) {
            return;
        }

        const activeFolderId = this.appState.now.folder.activeFolder;
        const activeNodeId = this.appState.now.folder.activeNode;
        const newFolder: FolderCreateRequest = {
            ...this.control.value,
            motherId: activeFolderId,
            nodeId: activeNodeId,
            failOnDuplicate: true,
        };
        this.folderActions.createNewFolder(newFolder).then(folder => {
            if (folder) {
                this.closeFn(folder);
            }
        })
    }

    private getContentRepositoryTypeFromFolderId(folderId: number): ContentRepositoryType | null {
        const folder = this.appState.now.entities.folder[folderId];
        const nodeId = folder.nodeId;
        const node = this.entityResolver.getNode(nodeId);
        const contentRepository = this.entityResolver.getEntity('contentRepository', node.contentRepositoryId);
        return contentRepository && contentRepository.crType || null;
    }

    private contentRepositoryTypeIsMesh(folderId: number): boolean {
        return this.getContentRepositoryTypeFromFolderId(folderId) === ContentRepositoryType.MESH;
    }

}
