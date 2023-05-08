import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
    ChannelSyncRequest,
    DependencyItemType,
    DependencyItemTypePlural,
    File as FileModel,
    Folder,
    Image,
    ItemsGroupedByChannelId,
    Page,
    SyncObjectsRequest,
    SyncObjectsResponse,
} from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { iconForItemType } from '../../../common/utils/icon-for-item-type';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService } from '../../../state';

export interface ItemsSelectedIds {
    pages: number[];
    files: number[];
    images: number[];
}

/**
 * Dialog for synchronizing a localized item in a channel with a master node.
 */
@Component({
    selector: 'gtx-channel-dependencies-modal',
    templateUrl: './channel-dependencies-modal.component.html',
    styleUrls: ['./channel-dependencies-modal.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChannelDependenciesModal implements OnInit, IModalDialog {
    item: Folder | Page | FileModel | Image;
    response: ChannelSyncRequest;

    typesToDisplay: DependencyItemTypePlural[] = ['pages', 'files', 'images'];
    allDependencyTypes: DependencyItemType[] = ['page', 'file', 'image'];

    activeTab = '';
    iconForItemType = iconForItemType;
    loading$: Observable<boolean>;

    syncableItems: SyncObjectsResponse;
    itemsGroupedByChannelId: ItemsGroupedByChannelId = {
        pages: [],
        files: [],
        images: [],
    };
    selectedItems: ItemsSelectedIds = {
        pages: [],
        files: [],
        images: [],
    };

    constructor(private api: Api, private appState: ApplicationStateService,
        private entityResolver: EntityResolver, private changeDetector: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.getDependencies();
    }

    closeFn(val: ItemsGroupedByChannelId): void { }
    cancelFn(val?: any): void { }

    registerCloseFn(close: (val: ItemsGroupedByChannelId) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    getDependencies(): void {
        const options: SyncObjectsRequest = {
            dstNodeId: this.response.masterId,
            ids: this.item.id,
            srcNodeId: this.response.channelId,
        };
        if (this.item.type === 'folder') {
            options.recursive = this.response.recursive;
            options.types = this.response.types;
        }
        this.api.folders.getSyncableObjects(this.item.type, options).toPromise().then((response: SyncObjectsResponse) => {
            this.removeTypeIfNoItems('pages', response.pagesTotal);
            this.removeTypeIfNoItems('files', response.filesTotal);
            this.removeTypeIfNoItems('images', response.imagesTotal);
            if (this.typesToDisplay.length === 0) {
                this.addSynchronizedItemIfNotFolder();
                this.closeFn(this.itemsGroupedByChannelId);
            } else {
                this.syncableItems = response;
                this.selectAllItems('pages');
                this.selectAllItems('files');
                this.selectAllItems('images');
                this.selectTab(this.typesToDisplay[0]);
                this.changeDetector.markForCheck();
            }
        });
    }

    removeTypeIfNoItems(type: DependencyItemTypePlural, count: number): void {
        if (count === 0) {
            let index = this.typesToDisplay.indexOf(type);
            if (index > -1) {
                this.typesToDisplay.splice(index, 1);
            }
        }
    }

    addSynchronizedItemIfNotFolder(): void {
        if (this.item.type !== 'folder') {
            this.addItemsGroupedByChannelAndType(
                this.item.type + 's' as DependencyItemTypePlural,
                this.item.channelId,
                this.item.id);
        }
    }

    getChannelName(channelId: number): string {
        const node = this.entityResolver.getEntity('node', channelId);
        if (node && node.name) {
            return node.name;
        }
        return '';
    }

    addItemsGroupedByChannelAndType(type: DependencyItemTypePlural, channelId: number, itemId: number): void {
        const itemsToSync = this.itemsGroupedByChannelId[type];
        const itemGroup = itemsToSync.find(obj => obj.channelId === channelId);
        if (!itemGroup) {
            itemsToSync.push({
                ids: [itemId],
                masterId: this.response.masterId,
                channelId: channelId,
            });
        } else if (itemGroup.ids.indexOf(itemId) === -1) {
            itemGroup.ids.push(itemId);
        }
    }

    confirmSettings(): void {
        for (const type of this.typesToDisplay) {
            this.addSelectedItemsAndDependencies(type);
        }

        this.addSynchronizedItemIfNotFolder();
        this.closeFn(this.itemsGroupedByChannelId);
    }

    private addSelectedItemsAndDependencies(type: DependencyItemTypePlural): void {
        for (const item of this.syncableItems[type]) {
            if (this.selectedItems[type].indexOf(item.id) >= 0) {
                this.addItemsGroupedByChannelAndType(type, item.channelId, item.id);
                this.addTransientDependencies(item);
            }
        }
    }

    private addTransientDependencies(item: Page | Image | FileModel): void {
        const itemDependencies = this.syncableItems.dependencies[item.type];

        if (itemDependencies && itemDependencies[item.id]) {
            for (const dependencyType of this.allDependencyTypes) {
                const transientDependencies = itemDependencies[item.id][dependencyType];
                if (transientDependencies) {
                    for (const itemId of Object.keys(transientDependencies).map(Number)) {
                        const typePlural = dependencyType + 's' as DependencyItemTypePlural;
                        this.addItemsGroupedByChannelAndType(typePlural, transientDependencies[itemId], itemId);
                    }
                }
            }
        }
    }

    selectTab(tab: string): void {
        this.activeTab = tab;
    }

    isSelected(itemType: DependencyItemTypePlural, itemId: number): boolean {
        return this.selectedItems[itemType].indexOf(itemId) > -1;
    }

    selectItem(itemType: DependencyItemTypePlural, itemId: number): void {
        this.selectedItems[itemType].push(itemId);
    }

    deselectItem(itemType: DependencyItemTypePlural, itemId: number): void {
        this.selectedItems[itemType] = this.selectedItems[itemType].filter((id: number) => id !== itemId);
    }

    areAllSelected(itemType: 'pages' | 'files' | 'images'): boolean {
        if (!this.selectedItems[itemType] || this.selectedItems[itemType].length === 0) {
            return false;
        }
        return this.selectedItems[itemType].length === this.syncableItems[itemType].length;
    }

    selectAllItems(itemType: 'pages' | 'files' | 'images'): void {
        const itemsOfType = this.syncableItems[itemType] as Array<Page | Image | FileModel>;
        this.selectedItems[itemType] = itemsOfType.map(item => item.id);
    }

    deselectAllItems(itemType: DependencyItemTypePlural): void {
        this.selectedItems[itemType] = [];
    }
}
