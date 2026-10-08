import { BO_NODE_ID, ContentItem, PickableEntity } from '../../../../common';
import { ErrorHandler } from '../../../../core';
import { EntityPickerModalComponent } from '../../../../shared/components';
import { ContentItemTrableLoaderService } from '../../../../shared/providers';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import {
    ListType,
    NodeIdObjectId,
    Overview,
    OverviewSetting,
    OverviewTagPartProperty,
    SelectType,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BaseFormElementComponent, ModalService, generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash-es';
import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';

type OverviewItemType = 'page' | 'file' | 'image' | 'folder';

interface OverviewItem {
    /** Cache-Key of the item */
    key: string;
    /** The type of the item */
    type: OverviewItemType;
    /** The ID references which are stored in the value */
    ref: NodeIdObjectId;
    /** The loaded entity, or `null` if it is still loading or failed to load */
    entity: PickableEntity | null;
    /** The folder path of the entity, split into the individual segments */
    breadcrumbs: string[];
    /** If the entity could not be loaded */
    failed: boolean;
}

function toItemType(listType: ListType, selectType: SelectType): OverviewItemType | null {
    // When selecting by folder, the selected items are always the folders
    if (selectType === SelectType.FOLDER) {
        return 'folder';
    }
    if (selectType !== SelectType.MANUAL) {
        return null;
    }

    switch (listType) {
        case ListType.PAGE:
            return 'page';
        case ListType.FILE:
            return 'file';
        case ListType.IMAGE:
            return 'image';
        case ListType.FOLDER:
            return 'folder';
        default:
            return null;
    }
}

function toCacheKey(type: OverviewItemType, ref: NodeIdObjectId): string {
    return `${type}.${ref.objectId}:${ref.nodeId}`;
}

function toBreadcrumbs(path: string | null | undefined): string[] {
    return (path || '').split('/').filter((segment) => segment.length > 0);
}

@Component({
    selector: 'gtx-overview-part-fill',
    templateUrl: './overview-part-fill.component.html',
    styleUrls: ['./overview-part-fill.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(OverviewPartFillComponent)],
    standalone: false,
})
export class OverviewPartFillComponent extends BaseFormElementComponent<OverviewTagPartProperty> implements OnChanges {

    @Input()
    public settings: OverviewSetting | null = null;

    /**
     * If the settings allow a default selection to be made.
     * Requires exactly one `listTypes` entry, exactly one `selectTypes` entry which is
     * either `FOLDER` or `MANUAL`, and `stickyChannel` to be enabled.
     */
    public selectionAvailable = false;

    /** The list-type which is used for the selection */
    public listType: ListType | null = null;

    /** The select-type which is used for the selection */
    public selectType: SelectType | null = null;

    /** The type of items which are selected, determined by the list- and select-type */
    public itemType: OverviewItemType | null = null;

    /** The selected items which are displayed */
    public items: OverviewItem[] = [];

    /** Cache of loaded entities, so they aren't loaded again on every change */
    protected entityCache: Record<string, PickableEntity> = {};

    /** Keys of entities which could not be loaded */
    protected failedLoads: Record<string, boolean> = {};

    /** Currently running loads, to not load the same entity multiple times */
    protected pendingLoads: Record<string, Subscription> = {};

    /** The item-type determined from the previous settings, to detect when it changes */
    private oldItemType: OverviewItemType | null | undefined;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected client: GCMSRestClientService,
        protected loader: ContentItemTrableLoaderService,
        protected modals: ModalService,
        protected errorHandler: ErrorHandler,
    ) {
        super(changeDetector);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.settings) {
            this.updateSelectionTypes();
            this.updateItems();
        }
    }

    /**
     * Determines the list-, select- and item-type from the settings.
     * If the item-type changes or becomes unavailable, the current selection is cleared,
     * as the selected items would not match the type anymore.
     */
    protected updateSelectionTypes(): void {
        const listTypes = this.settings?.listTypes || [];
        const selectTypes = this.settings?.selectTypes || [];

        this.listType = listTypes.length === 1 ? listTypes[0] : null;
        this.selectType = selectTypes.length === 1 && (selectTypes[0] === SelectType.FOLDER || selectTypes[0] === SelectType.MANUAL)
            ? selectTypes[0]
            : null;
        this.itemType = this.settings?.stickyChannel === true
            ? toItemType(this.listType, this.selectType)
            : null;
        this.selectionAvailable = this.itemType != null;

        // Settings aren't loaded yet, nothing to compare against
        if (this.settings == null) {
            return;
        }

        if (this.oldItemType === undefined) {
            this.oldItemType = this.itemType;
            return;
        }

        if (this.itemType === this.oldItemType) {
            return;
        }

        this.oldItemType = this.itemType;
        if ((this.value?.overview?.selectedNodeItemIds?.length ?? 0) > 0) {
            this.selectionChanged([]);
        }
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        Object.values(this.pendingLoads).forEach((sub) => sub.unsubscribe());
        this.pendingLoads = {};
    }

    protected onValueChange(): void {
        this.updateItems();
    }

    /**
     * Rebuilds the displayed items from the current value, and loads all entities which aren't cached yet.
     */
    protected updateItems(): void {
        const overview = this.value?.overview;

        if (!this.itemType) {
            this.items = [];
            return;
        }

        const type = this.itemType;
        this.items = (overview?.selectedNodeItemIds || []).map((ref) => {
            const key = toCacheKey(type, ref);
            const entity = this.entityCache[key] ?? null;

            if (entity == null && !this.failedLoads[key]) {
                this.loadEntity(type, ref);
            }

            return {
                key,
                type,
                ref,
                entity,
                breadcrumbs: toBreadcrumbs((entity?.entity as any)?.path),
                failed: !!this.failedLoads[key],
            };
        });
    }

    protected loadEntity(type: OverviewItemType, ref: NodeIdObjectId): void {
        const key = toCacheKey(type, ref);
        if (this.pendingLoads[key]) {
            return;
        }

        this.pendingLoads[key] = this.fetchEntity(type, ref).subscribe({
            next: (item) => {
                this.entityCache[key] = this.toPickableEntity(item, ref.nodeId);
                delete this.pendingLoads[key];
                this.updateItems();
                this.changeDetector.markForCheck();
            },
            error: (err) => {
                console.error('could not load overview entity!', err);
                this.failedLoads[key] = true;
                delete this.pendingLoads[key];
                this.updateItems();
                this.changeDetector.markForCheck();
            },
        });
    }

    protected fetchEntity(type: OverviewItemType, ref: NodeIdObjectId): Observable<ContentItem> {
        const options = { nodeId: ref.nodeId };

        switch (type) {
            case 'page':
                return this.client.page.get(ref.objectId, options).pipe(map((res) => res.page));
            case 'file':
                return this.client.file.get(ref.objectId, options).pipe(map((res) => res.file));
            case 'image':
                return this.client.image.get(ref.objectId, options).pipe(map((res) => res.image));
            case 'folder':
                return this.client.folder.get(ref.objectId, options).pipe(map((res) => res.folder));
        }
    }

    protected toPickableEntity(item: ContentItem, nodeId: number): PickableEntity {
        return {
            entity: {
                ...this.loader.mapToBusinessObject(item, null),
                [BO_NODE_ID]: nodeId,
            },
            type: item.type,
            nodeId,
        };
    }

    protected toNodeItemIds(entities: PickableEntity[]): NodeIdObjectId[] {
        return entities.map((entity) => ({
            nodeId: entity.nodeId,
            objectId: entity.entity.id,
        }));
    }

    public async addItems(): Promise<void> {
        if (!this.itemType) {
            return;
        }

        this.triggerTouch();

        const type = this.itemType;
        // Nodes/Channels may be picked for folders as well, which are converted to their root-folder by the picker
        const allowNodes = type === 'folder';

        const dialog = await this.modals.fromComponent(EntityPickerModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        }, {
            types: allowNodes ? ['folder', 'node', 'channel'] : [type],
            nodesAsFolder: allowNodes,
            multiple: true,
        });

        try {
            const picked = await dialog.open();
            if (picked === false || picked == null) {
                return;
            }

            const entities = Array.isArray(picked) ? picked : [picked];
            const currentRefs = this.items.map((item) => item.ref);
            const existingKeys = new Set(currentRefs.map((ref) => toCacheKey(type, ref)));
            const newRefs: NodeIdObjectId[] = [];

            this.toNodeItemIds(entities).forEach((ref, index) => {
                const key = toCacheKey(type, ref);
                // Skip items which are already selected
                if (existingKeys.has(key)) {
                    return;
                }
                existingKeys.add(key);
                this.entityCache[key] = entities[index];
                newRefs.push(ref);
            });

            if (newRefs.length === 0) {
                return;
            }

            this.selectionChanged([...currentRefs, ...newRefs]);
        } catch (err) {
            if (wasClosedByUser(err)) {
                return;
            }
            this.errorHandler.catch(err);
        }
    }

    public removeItem(index: number): void {
        this.triggerTouch();
        const refs = this.items.map((item) => item.ref);
        refs.splice(index, 1);
        this.selectionChanged(refs);
    }

    protected selectionChanged(selectedNodeItemIds: NodeIdObjectId[]): void {
        this.overviewChanged({
            listType: this.listType,
            selectType: this.selectType,
            selectedNodeItemIds,
        });
    }

    public overviewChanged(overview: Partial<Overview>): void {
        const newValue: OverviewTagPartProperty = {
            ...pick(this.value || {}, ['id', 'globalId', 'partId']),
            type: TagPropertyType.OVERVIEW,
            overview: {
                ...this.value?.overview,
                ...overview,
            } as Overview,
        };

        this.triggerChange(newValue);
    }
}
