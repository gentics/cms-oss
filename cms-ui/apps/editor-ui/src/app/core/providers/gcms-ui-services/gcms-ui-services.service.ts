import { Injectable } from '@angular/core';
import { GcmsUiServices, ImageEditorOptions, RepositoryBrowserOptions, TagEditorOptions, TagEditorResult } from '@gentics/cms-integration-api-models';
import { FileOrImage, Folder, Image, ItemInNode, Page, Raw, Tag, TagType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService } from '@gentics/ui-core';
import { EditorOverlayService } from '../../../editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../../shared/util/selected-item-helper/selected-item-helper';
import { ApplicationStateService } from '../../../state';
import { UploadWithPropertiesModalComponent } from '../../../tag-editor/components/shared/upload-with-properties-modal/upload-with-properties-modal.component';
import { TagEditorService } from '../../../tag-editor';

@Injectable()
export class GcmsUiServicesProvider implements GcmsUiServices {

    editorNodeId: number | undefined;

    constructor(
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private editorOverlayService: EditorOverlayService,
        private tagEditorService: TagEditorService,
        private modals: ModalService,
    ) {
        this.editorNodeId = this.appState.now.editor.nodeId;
    }

    public readonly restClient = this.client.getClient();

    openImageEditor(options: ImageEditorOptions): Promise<Image<Raw> | void> {
        return this.editorOverlayService.editImage(options);
    }

    openTagEditor(tag: Tag, tagType: TagType, page: Page<Raw>, options?: TagEditorOptions): Promise<TagEditorResult> {
        return this.tagEditorService.openTagEditor(tag, tagType, page, options);
    }

    openUploadModal(uploadType: 'image' | 'file', destinationFolder?: Folder, allowFolderSelection?: boolean): Promise<FileOrImage> {
        return this.modals.fromComponent(
            UploadWithPropertiesModalComponent,
            { padding: true, width: '1000px' },
            {
                itemType: uploadType,
                allowFolderSelection: allowFolderSelection ?? true,
                destinationFolder,
            },
        ).then((dialog) => dialog.open());
    }

    openRepositoryBrowser<T = ItemInNode>(options: RepositoryBrowserOptions): Promise<T | T[] | null> {
        return this.repositoryBrowserClient.openRepositoryBrowser({ startNode: this.editorNodeId, ...options });
    }

    createSelectedItemsHelper(itemType: 'page' | 'folder' | 'file' | 'image' | 'form', defaultNodeId?: number): any {
        return new SelectedItemHelper(itemType, defaultNodeId ? defaultNodeId : (this.editorNodeId ? this.editorNodeId : -1), this.client);
    }
}
