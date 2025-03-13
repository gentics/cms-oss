import { Injectable } from '@angular/core';
import {
    FolderItemTypePlural,
    Usage,
} from '@gentics/cms-models';
import { ItemsInfo } from '../../../common/models';
import { Api } from '../../../core/providers/api/api.service';
import {
    UpdateEntitiesAction,
} from '../../modules';
import { ApplicationStateService } from '../../providers';

// TODO: Move the only remaining function to a different service, or rename this service
@Injectable()
export class UsageActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    /**
     * Fetch the total usage for a list of items.
     */
    public getTotalUsage(type: 'file' | 'folder' | 'form' | 'image' | 'page', itemIds: number[], nodeId: number): void {
        const pluralizedType = `${type}s` as FolderItemTypePlural;
        const itemsInfo: ItemsInfo = this.appState.now.folder[pluralizedType];
        const shouldGetUsage = itemsInfo && itemsInfo.displayFields && itemsInfo.displayFields.indexOf('usage') >= 0;

        if (!shouldGetUsage) {
            return;
        }

        this.api.folders.getTotalUsage(type, itemIds, nodeId).subscribe(result => {
            const usage = result.infos;
            const updateHash: { [id: number]: { usage: Usage } } = {};
            for (const id of Object.keys(usage)) {
                updateHash[id as any] = { usage: usage[id as any] };
            }

            this.appState.dispatch(new UpdateEntitiesAction({ [type]: updateHash }));
        });
    }
}
