import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { ContentRepositoryType, EditableFolderProps } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { FolderPropertiesForm } from '../../../shared/components/folder-properties-form/folder-properties-form.component';
import { ApplicationStateService, FolderActionsService } from '../../../state';

@Component({
    selector: 'create-folder-modal',
    templateUrl: './create-folder-modal.tpl.html'
})
export class CreateFolderModalComponent implements AfterViewInit, OnDestroy, OnInit, IModalDialog {

    creating$: Observable<boolean>;

    form: UntypedFormGroup;

    folderProperties: EditableFolderProps = {};

    @ViewChild(FolderPropertiesForm, { static: true })
    private folderPropertiesForm: FolderPropertiesForm;

    private subscriptions: Subscription[] = [];

    constructor(
        private folderActions: FolderActionsService,
        private entityResolver: EntityResolver,
        private appState: ApplicationStateService,
    ) {
        this.creating$ = appState.select(state => state.folder.folders.creating);
    }

    ngOnInit(): void {
        const activeNodeId = this.appState.now.folder.activeNode;
        const activeFolderId = this.appState.now.folder.activeFolder;

        /** if cr is of type Mesh prefilling folder directory has another logic in contrast to MySQL cr.
         * @see https://jira.gentics.com/browse/GTXPE-590
         */
        const crIsMesh = this.contentRepositoryTypeIsMesh(activeFolderId);

        this.folderProperties.directory = '';

        if (!!activeNodeId && !!activeFolderId && !crIsMesh) {
            const activeNode = this.appState.now.entities.node[activeNodeId];
            if (!!activeNode && !activeNode.pubDirSegment) {
                this.folderProperties.directory = this.entityResolver.getFolder(activeFolderId).publishDir;
            }
        }
    }

    ngAfterViewInit(): void {
        this.form = this.folderPropertiesForm.form;
    }

    ngOnDestroy(): void {
        this.subscriptions.map(s => s.unsubscribe());
    }

    closeFn = (val: any) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    saveChanges(): void {
        const activeFolderId = this.appState.now.folder.activeFolder;
        const activeNodeId = this.appState.now.folder.activeNode;
        const newFolder = Object.assign({}, this.form.value, { parentFolderId: activeFolderId, nodeId: activeNodeId, failOnDuplicate: true });
        this.folderActions.createNewFolder(newFolder)
            .then(folder => {
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
        return this.getContentRepositoryTypeFromFolderId(folderId) === 'mesh';
    }

}
