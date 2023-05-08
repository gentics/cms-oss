import { Injectable } from '@angular/core';
import { RepositoryBrowserOptions } from '@gentics/cms-models';
import { RepositoryBrowserClient } from '../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../../shared/util/selected-item-helper/selected-item-helper';
import { ApplicationStateService } from '../../../state';
import { Api } from '../api/api.service';

@Injectable()
export class GcmsUiServices {

    editorNodeId: number | undefined;

    constructor (
        private api: Api,
        private appState: ApplicationStateService,
        private repositoryBrowserClient: RepositoryBrowserClient,
    ) {
        this.editorNodeId = this.appState.now.editor.nodeId;
    }

    openRepositoryBrowser(options: RepositoryBrowserOptions) {
        return this.repositoryBrowserClient.openRepositoryBrowser({ startNode: this.editorNodeId, ...options });
    }

    createSelectedItemsHelper(itemType: 'page' | 'folder' | 'file' | 'image' | 'form', defaultNodeId?: number) {
        return new SelectedItemHelper(itemType, defaultNodeId ? defaultNodeId : (this.editorNodeId ? this.editorNodeId : -1), this.api.folders);
    }
}
