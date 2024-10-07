import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
    ChannelSyncRequest,
    Feature,
    FolderItemOrTemplateType,
    FolderItemTypePlural,
    InheritableItem,
    Node,
    Normalized,
    isFolderOrNode,
} from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { itemIsLocal } from '../../../common/utils/item-is-local';
import { itemIsLocalized } from '../../../common/utils/item-is-localized';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService, FolderActionsService } from '../../../state';

export interface AffectedObject {
    type: string;
    items: InheritableItem[];
    local: number;
    localized: number;
}

/**
 * Dialog for synchronizing a localized item in a channel with a master node.
 */
@Component({
    selector: 'gtx-synchronize-channel-modal',
    templateUrl: './synchonize-channel-modal.component.html',
    styleUrls: ['./synchronize-channel-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SynchronizeChannelModal extends BaseModal<ChannelSyncRequest> implements OnInit {

    item: InheritableItem;
    types: FolderItemOrTemplateType[] = ['folder', 'page', 'file', 'image', 'template'];
    selectedTypes = {
        folder: true,
        page: true,
        file: true,
        image: true,
        template: true,
    };
    expanded: any = {
        folder: false,
        page: false,
        file: false,
        image: false,
        template: false,
    };
    channel: Node;
    targetNode: Node;
    availableMasterNodes: Node[] = [];
    affectedObjects$: Observable<AffectedObject[]>;
    loading$: Observable<boolean>;
    recursive = false;

    private subscription: Subscription;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: ApplicationStateService,
        private api: Api,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.setupObservables();
        this.fetchAvailableSyncTargets();
        this.getSyncReport();
    }

    private setupObservables(): void {
        this.loading$ = this.appState.select(state => state.folder.channelSyncReport.fetching);
        this.affectedObjects$ = this.appState.select(state => state.folder.channelSyncReport).pipe(
            filter(report => !report.fetching),
            map(report => {
                const affectedObjects: AffectedObject[] = [];

                this.types.forEach(type => {
                    const typePlural = type + 's' as FolderItemTypePlural | 'templates';
                    const items: InheritableItem<Normalized>[] = report[typePlural]
                        .map((id: number) => this.entityResolver.getEntity(type, id) as InheritableItem<Normalized>);

                    affectedObjects.push({
                        type,
                        items,
                        local: items.filter(itemIsLocal).length,
                        localized: items.filter(itemIsLocalized).length,
                    });
                });

                return affectedObjects;
            }),
        )
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    confirmSettings(): void {
        const settings: ChannelSyncRequest = {
            masterId: this.targetNode.id,
            channelId: this.channel.id,
            recursive: this.recursive,
            types: [],
        };

        if (isFolderOrNode(this.item)) {
            settings.types = this.types.filter(type => this.selectedTypes[type] === true);
        }

        this.closeFn(settings);
    }

    toggleRecursive(): void {
        this.recursive = !this.recursive;
        this.getSyncReport();
    }

    toggleSelected(type: FolderItemOrTemplateType): void {
        this.selectedTypes[type] = !this.selectedTypes[type];
    }

    private fetchAvailableSyncTargets(): void {
        if (itemIsLocal(this.item) || !this.appState.now.features[Feature.MULTICHANNELLING]) {
            this.determineAvailableMasterNodes([]);
            this.changeDetector.markForCheck();
            return;
        }

        this.subscription = this.api.folders.getLocalizations(this.item.type, this.item.id)
            .subscribe(response => {
                const localizedNodeIDs = Object.keys(response.nodeIds).map(itemId => response.nodeIds[Number(itemId)]);
                this.determineAvailableMasterNodes(localizedNodeIDs);
                this.changeDetector.markForCheck();
            });
    }

    /**
     * Checks which nodes the item can be synchronized to.
     *
     * @example
     * - Master Node (#1)
     *   |- Channel 1 (#2)
     *   |  '- Channel 1.1 (#3)
     *   '- Channel 2 (#4)
     *
     * An item created locally in #3 can be synchronized to #1 or #2, but not #4.
     * An item localized from #1 in #2 and #3 can be synchronized from #3 to #2, but not #1.
     * An item localized from #1 in #2 (and not #3) can be synchronized from #3 to #2 or #1.
     */
    private determineAvailableMasterNodes(localizedNodeIds: number[]): void {
        this.availableMasterNodes = [];
        let node = this.channel;
        do {
            if ((node = this.entityResolver.getNode(node.inheritedFromId))) {
                this.availableMasterNodes.unshift(node);
            }
        } while (node && node.id !== node.inheritedFromId && localizedNodeIds.indexOf(node.id) < 0);

        // Pre-select direct parent (example above: #3 => #2)
        this.targetNode = this.availableMasterNodes[this.availableMasterNodes.length - 1];
    }

    private getSyncReport(): void {
        if (this.item.type === 'folder' && this.appState.now.features[Feature.MULTICHANNELLING]) {
            this.folderActions.getChannelSyncReport(this.item.id, this.channel.id, this.recursive);
        }
    }

}
