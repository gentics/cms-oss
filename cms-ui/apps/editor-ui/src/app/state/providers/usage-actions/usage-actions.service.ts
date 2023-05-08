import { Injectable } from '@angular/core';
import { fileSchema, folderSchema, pageSchema, templateSchema } from '@editor-ui/app/common/models';
import {
    FolderItemTypePlural,
    ItemType,
    Usage,
    UsageType,
} from '@gentics/cms-models';
import { Schema } from 'normalizr';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { ItemsInfo } from '../../../common/models';
import { Api } from '../../../core/providers/api/api.service';
import { Version } from '../../../core/providers/user-settings/version.class';
import { ApplicationStateService } from '../../providers';
import {
    ItemUsageFetchingErrorAction,
    ItemUsageFetchingSuccessAction,
    PartialUsageData,
    StartItemUsageFetchingAction,
    UpdateEntitiesAction,
} from '../../modules';

@Injectable()
export class UsageActionsService {

    private readonly CURRENT_VERSION: Version;
    private readonly TARGET_VERSION: Version;
    private readonly EXTENDED_USAGE_AVILABLE: boolean;

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {
        this.CURRENT_VERSION = Version.parse(this.appState.now.ui.cmpVersion?.version);
        this.TARGET_VERSION = Version.parse('5.29.0');
        this.EXTENDED_USAGE_AVILABLE = this.CURRENT_VERSION.satisfiesMinimum(this.TARGET_VERSION);
    }

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
            for (let id of Object.keys(usage)) {
                updateHash[id as any] = { usage: usage[id as any] };
            }

            this.appState.dispatch(new UpdateEntitiesAction(updateHash));
        });
    }

    /**
     * Get all usage of the given item.
     */
    public getUsage(itemType: ItemType, itemId: number, nodeId: number): void {
        this.appState.dispatch(new StartItemUsageFetchingAction(itemType, itemId));

        let usageTypes: UsageType[];
        switch (itemType) {
            case 'page':
                usageTypes = ['tag', 'template', 'variant', 'page'];
                if (this.EXTENDED_USAGE_AVILABLE) {
                    usageTypes = [...usageTypes, 'linkedPage', 'linkedImage', 'linkedFile'];
                }
                break;
            case 'file':
            case 'image':
                usageTypes = ['file', 'folder', 'image', 'page', 'template'];
                break;
            default:
                usageTypes = [];
        }

        const requests = usageTypes.map(usageType =>
            this.api.folders.getUsageBy(itemType, itemId, usageType, nodeId).pipe(
                map(res => ({ res, usageType })),
            ),
        );

        forkJoin(requests).subscribe(responses => {
            const usageData: PartialUsageData = {};
            for (let response of responses) {
                const typeKey = `${response.usageType}s` as keyof PartialUsageData;
                const listKey = this.getUsageResponseListKey(response.usageType);
                usageData[typeKey] = response.res[listKey];
            }

            this.appState.dispatch(new ItemUsageFetchingSuccessAction(usageData));
        }, () => {
            this.appState.dispatch(new ItemUsageFetchingErrorAction());
        });

    }

    /**
     * Given the usageType, returns the name of the key in the response object which contains the
     * list of items.
     */
    private getUsageResponseListKey(usageType: UsageType): string {
        switch (usageType) {
            case 'page':
            case 'variant':
            case 'tag':
            case 'linkedPage':
                return 'pages';
            case 'file':
            case 'image':
            case 'linkedFile':
            case 'linkedImage':
                return 'files';
            case 'folder':
                return 'folders';
            case 'template':
                return 'templates';
            default:
                throw new Error(`Invalid usageType "${usageType}"`);
        }
    }

    /**
     * Given the usageType, returns the name of the key in the response object which contains the
     * list of items.
     */
    private getSchema(usageType: UsageType): Schema {
        switch (usageType) {
            case 'page':
            case 'variant':
            case 'tag':
                return pageSchema;
            case 'file':
            case 'image':
                return fileSchema;
            case 'folder':
                return folderSchema;
            case 'template':
                return templateSchema;
            default:
                throw new Error(`Invalid usageType "${usageType}"`);
        }
    }
}
