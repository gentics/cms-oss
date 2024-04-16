import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { iconForItemType } from '@editor-ui/app/common/utils/icon-for-item-type';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { ApplicationStateService } from '@editor-ui/app/state';
import { RepositoryBrowserOptions, TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    File,
    Folder,
    FolderItemType,
    Image,
    Item,
    ItemInNode,
    ListType,
    NodeIdObjectId,
    OrderBy,
    OrderDirection,
    Overview,
    OverviewSetting,
    OverviewTagPartProperty,
    Page,
    Raw,
    SelectType,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { Observable, Subscription, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

/** All item types that can be selected by the overview. */
export type OverviewItem = ItemInNode<Page<Raw> | Folder<Raw> | File<Raw> | Image<Raw>>;

/** Used for easily getting the label of an object in the template. */
export interface LabeledObject<T> {
    label: string;
    value: T;
}

/**
 * Used to group items that need to be loaded by their node IDs,
 * while remembering their original positions in the selectedNodeItemIds array.
 */
interface ItemsForNode {

    /** The array of items for a this node. */
    itemIds: number[];

    /** Maps each itemId to index this item had in the original selectedNodeItemIds array. */
    origIndices: {
        [itemId: number]: number;
    };

}

/**
 * Creates a LabeledObject for the specified OrderBy type.
 */
function createLabeledOrderByType(orderBy: OrderBy): LabeledObject<OrderBy> {
    return { label: `tag_editor.order_by_type_${orderBy.toLowerCase()}`, value: orderBy };
}

/** Maps each ListType to its corresponding allowed OrderBy types. */
const ALLOWED_ORDER_BY_TYPES = new Map<ListType, LabeledObject<OrderBy>[]>();
ALLOWED_ORDER_BY_TYPES.set(ListType.PAGE, [
    createLabeledOrderByType(OrderBy.ALPHABETICALLY), createLabeledOrderByType(OrderBy.PRIORITY), createLabeledOrderByType(OrderBy.PDATE),
    createLabeledOrderByType(OrderBy.CDATE), createLabeledOrderByType(OrderBy.EDATE), createLabeledOrderByType(OrderBy.SELF),
]);
ALLOWED_ORDER_BY_TYPES.set(ListType.FOLDER, [
    createLabeledOrderByType(OrderBy.ALPHABETICALLY), createLabeledOrderByType(OrderBy.CDATE), createLabeledOrderByType(OrderBy.EDATE),
    createLabeledOrderByType(OrderBy.SELF),
]);
ALLOWED_ORDER_BY_TYPES.set(ListType.FILE, [
    createLabeledOrderByType(OrderBy.ALPHABETICALLY), createLabeledOrderByType(OrderBy.CDATE), createLabeledOrderByType(OrderBy.EDATE),
    createLabeledOrderByType(OrderBy.FILESIZE), createLabeledOrderByType(OrderBy.SELF),
]);
ALLOWED_ORDER_BY_TYPES.set(ListType.IMAGE, ALLOWED_ORDER_BY_TYPES.get(ListType.FILE));


/**
 * Used to edit OverviewTagParts.
 */
@Component({
    selector: 'overview-tag-property-editor',
    templateUrl: './overview-tag-property-editor.component.html',
    styleUrls: ['./overview-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverviewTagPropertyEditor implements TagPropertyEditor, OnInit, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: OverviewTagPartProperty;

    /** Allowed types of listed objects. */
    allowedListTypes: LabeledObject<ListType>[];

    /** Allowed ways to select objects. */
    allowedSelectTypes: LabeledObject<SelectType>[];

    /** Allowed ways for determining the order of selected objects */
    allowedOrderByTypes: LabeledObject<OrderBy>[] = [];

    /** Allowed sort orders. */
    allowedOrderDirections: LabeledObject<OrderDirection>[] = [
        { label: 'tag_editor.order_direction_asc', value: OrderDirection.ASC },
        { label: 'tag_editor.order_direction_desc', value: OrderDirection.DESC },
    ];

    /** Used to access the ListType and SelectType enums from the template. */
    listTypeEnum = ListType;
    selectTypeEnum = SelectType;

    /** The currently selected items. */
    selectedItems: OverviewItem[] = [];

    /** Used to map ItemTypes their icons. */
    iconForItemType = iconForItemType;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    /** The current TagEditorContext. */
    context: TagEditorContext;

    /** Page this edited tag belongs to */
    private page?: Page<Raw>;

    private subscriptions = new Subscription();
    private previousListType: ListType;
    private previousSelectType: SelectType;
    private previousMaxItems: number;
    private nodeName: string;
    private isStickyChannelEnabled: boolean;

    constructor(
        private api: Api,
        private changeDetector: ChangeDetectorRef,
        private repositoryBrowserClient: RepositoryBrowserClient,
        private modalService: ModalService,
        private i18n: I18nService,
        private state: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        this.nodeName = this.state.now.entities.node[this.state.now.editor.nodeId].name;
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.isStickyChannelEnabled = this.tagPart.overviewSettings.stickyChannel;
        this.context = context;
        this.page = context.page;
        this.allowedListTypes = this.assembleAllowedListTypes(this.tagPart.overviewSettings);
        this.allowedSelectTypes = this.assembleAllowedSelectTypes(this.tagPart.overviewSettings);
        this.updateTagProperty(tagProperty);
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    /**
     * Opens the repository browser to allow the user to select items.
     */
    browseForItems(): void {
        let contentLanguage: string;
        if (this.page) {
            contentLanguage = this.page.language;
        }
        const options: RepositoryBrowserOptions = {
            allowedSelection: this.determineSelectedItemsType(this.tagProperty.overview.listType, this.tagProperty.overview.selectType),
            selectMultiple: true,
            contentLanguage,
        };

        this.repositoryBrowserClient.openRepositoryBrowser(options)
            .then((newItems: OverviewItem[]) => {
                if (!this.isStickyChannelEnabled) {
                    newItems.forEach(newItem => newItem.nodeId = undefined);
                }
                // Remove newItems, which already exist in the selectedItems.
                newItems = newItems.filter(newItem =>
                    !this.selectedItems.find(existingItem =>
                        existingItem.id === newItem.id && existingItem.nodeId === newItem.nodeId || this.isAlreadyAddedItem(existingItem, newItem),
                    ),
                );
                this.updatePath(newItems);
                this.selectedItems = this.selectedItems.concat(newItems);
                this.onUserChange();
                this.changeDetector.markForCheck();
            });
    }

    /**
     * When stickyChannel is disabled, the localized items of channel node can't be saved,
     * thus in case of localized item is added to the list, we save the master item.
     * The item list cannot contain the master item and the localized item which belong to it in same time.
     */
    private isAlreadyAddedItem(existingItem: OverviewItem, newItem: OverviewItem): boolean {
        if (!this.isStickyChannelEnabled) {
            return (existingItem.masterNodeId === newItem.masterNodeId && existingItem.id === newItem.masterId) ||
                (existingItem.masterNodeId === newItem.masterNodeId && newItem.id === existingItem.masterId);
        }
    }

    /**
     * Updates the tagProperty with changes made by the user and calls onChangeFn().
     */
    onUserChange(): void {
        // This is necessary, because many GUIC components update their ngModels after firing their change event.
        setTimeout(() => {
            const overview = this.tagProperty.overview;

            if (this.isStickyChannelEnabled) {
                overview.selectedNodeItemIds = this.selectedItems.map(item => ({ objectId: item.id, nodeId: item.nodeId }));
                if (overview.selectedItemIds) {
                    delete overview.selectedItemIds;
                }
            } else {
                overview.selectedItemIds = this.selectedItems.map(item => item.id);
                if (overview.selectedNodeItemIds) {
                    delete overview.selectedNodeItemIds;
                }
            }

            if (this.onChangeFn) {
                const changes: Partial<TagPropertyMap> = {};
                changes[this.tagPart.keyword] = this.tagProperty;
                this.onChangeFn(changes);
            }
        });
    }

    /**
     * Event handler for changes to the selected ListType or the selected SelectType.
     */
    onSelectedListOrSelectTypeChange(): void {
        // This is necessary, because gtx-select updates its ngModel after firing the change event.
        setTimeout(() => {
            const overview = this.tagProperty.overview;

            this.allowedOrderByTypes = this.getAllowedOrderByTypes(overview.listType);
            if (!this.allowedOrderByTypes.find(orderByType => orderByType.value === overview.orderBy)) {
                overview.orderBy = OrderBy.UNDEFINED;
            }

            if (overview.listType !== ListType.FOLDER && overview.selectType !== SelectType.FOLDER) {
                overview.recursive = false;
            }

            if (overview.selectType !== SelectType.FOLDER && overview.selectType !== SelectType.AUTO) {
                overview.maxItems = 0;
            }

            if (overview.listType !== this.previousListType || overview.selectType !== this.previousSelectType) {
                this.selectedItems = [];
            }

            this.previousListType = overview.listType;
            this.previousSelectType = overview.selectType;
            this.changeDetector.markForCheck();
            this.onUserChange();
        });
    }

    onMaxItemsChange(value: number): void {
        // This is necessary, because gtx-input updates its ngModel after firing the change event.
        setTimeout(() => {
            const valueSanitized = Math.floor(value);
            if (valueSanitized !== this.previousMaxItems) {
                this.previousMaxItems = valueSanitized;
                this.tagProperty.overview.maxItems = valueSanitized;
                this.changeDetector.markForCheck();
                this.onUserChange();
            }
        });
    }

    /**
     * Determines if the selected items list and the "Add items" button are visible.
     */
    areSelectedItemsVisible(overview: Overview): boolean {
        return overview.listType && overview.listType !== ListType.UNDEFINED &&
            (overview.selectType === SelectType.FOLDER || overview.selectType === SelectType.MANUAL);
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.OVERVIEW) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by OverviewTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        const overview = this.tagProperty.overview;

        if (overview.orderBy === 'UNDEFINED') {
            overview.orderBy = OrderBy.CDATE;
        }

        if (overview.orderDirection === 'UNDEFINED') {
            overview.orderDirection = OrderDirection.DESC;
        }

        this.previousListType = overview.listType;
        this.previousSelectType = overview.selectType;
        this.previousMaxItems = overview.maxItems;
        this.allowedOrderByTypes = this.getAllowedOrderByTypes(overview.listType);
        this.loadSelectedItems(overview);
        this.changeDetector.markForCheck();
    }

    /**
     * Loads the items currently selected in the specified overview.
     */
    private loadSelectedItems(overview: Overview): void {
        if (!this.areSelectedItemsVisible(overview)) {
            this.selectedItems = [];
            this.changeDetector.markForCheck();
            return;
        }

        let groupedItems: Map<number, ItemsForNode>;
        if (this.isStickyChannelEnabled) {
            groupedItems = this.groupItemsByNode(overview.selectedNodeItemIds || []);
        } else {
            groupedItems = this.groupItemsWithoutNodes(overview.selectedItemIds || []);
        }
        const loadRequest$ = this.loadItems(groupedItems, this.determineSelectedItemsType(overview.listType, overview.selectType));
        const sub = loadRequest$.subscribe(loadedItems => {
            this.updatePath(loadedItems);
            this.selectedItems = loadedItems;
            this.changeDetector.markForCheck();
        });
        this.subscriptions.add(sub);
    }

    private updatePath(selectedItems: OverviewItem[]): void {
        if (!this.isStickyChannelEnabled) {
            selectedItems.forEach(item => {
                if (this.state.now.entities.node[this.state.now.editor.nodeId].masterNodeId === item.masterNodeId) {
                    item.path = item.path.replace(item.masterNode, this.nodeName);
                }
            })
        }
    }

    /**
     * Creates the list of allowed ListTypes based on the overview's settings.
     */
    private assembleAllowedListTypes(settings: OverviewSetting): LabeledObject<ListType>[] {
        if (settings.listTypes.length > 0) {
            return settings.listTypes.map(listType => {
                if (listType === ListType.UNDEFINED) {
                    throw new TagEditorError('Incorrect OverviewSetting: allowed listTypes includes ListType.UNDEFINED.');
                }
                return this.createLabeledListType(listType);
            });
        } else {
            return [
                this.createLabeledListType(ListType.PAGE),
                this.createLabeledListType(ListType.FOLDER),
                this.createLabeledListType(ListType.FILE),
                this.createLabeledListType(ListType.IMAGE),
            ];
        }
    }

    /**
     * Creates a LabeledObject for the specified ListType.
     */
    private createLabeledListType(listType: ListType): LabeledObject<ListType> {
        return { label: `tag_editor.list_type_${listType.toLowerCase()}`, value: listType };
    }

    /**
     * Creates the list of allowed SelectTypes based on the overview's settings.
     */
    private assembleAllowedSelectTypes(settings: OverviewSetting): LabeledObject<SelectType>[] {
        if (settings.selectTypes.length > 0) {
            return settings.selectTypes.map(selectType => {
                if (selectType === SelectType.UNDEFINED) {
                    throw new TagEditorError('Incorrect OverviewSetting: allowed selectTypes includes SelectType.UNDEFINED.');
                }
                return this.createLabeledSelectType(selectType);
            });
        } else {
            return [
                this.createLabeledSelectType(SelectType.FOLDER),
                this.createLabeledSelectType(SelectType.MANUAL),
                this.createLabeledSelectType(SelectType.AUTO),
            ];
        }
    }

    /**
     * Gets the OrderBy types, which are allowed for the specified listType.
     */
    private getAllowedOrderByTypes(listType: ListType): LabeledObject<OrderBy>[] {
        return ALLOWED_ORDER_BY_TYPES.get(listType) || [];
    }

    /**
     * Creates a LabeledObject for the specified SelectType.
     */
    private createLabeledSelectType(selectType: SelectType): LabeledObject<SelectType> {
        return { label: `tag_editor.select_type_${selectType.toLowerCase()}`, value: selectType };
    }

    /**
     * Loads the specified Items.
     * In case the "Sticky Channel" option is disabled, we only know the ids of the selected items, not the node ids they were selected from.
     * Items without node context are grouped into the group with "node id" "-1" in this component. We set nodeId to "undefined", such that
     * it is not part of the request body in the subsequent "getExistingItems" request. Thus, we load the items only by item id, as expected in
     * case the "Sticky Channel" option is disabled.
     */
    private loadItems(itemsToLoad: Map<number, ItemsForNode>, itemType: FolderItemType): Observable<OverviewItem[]> {
        // Create API requests to load the items from each node.
        const requests$: Observable<{ item: Item<Raw>, nodeId: number, origIndex: number }[]>[] = [];
        itemsToLoad.forEach((itemsFromNode, nodeId) => {
            if (nodeId === -1) {
                nodeId = undefined;
            }
            const request$ = this.api.folders.getExistingItems(itemsFromNode.itemIds, nodeId, itemType).pipe(
                map(items => items.map(item => ({
                    item: item,
                    nodeId: this.isStickyChannelEnabled ? item.inheritedFromId : item.masterNodeId,
                    origIndex: itemsFromNode.origIndices[item.id],
                }))),
            );
            requests$.push(request$);
        });

        // Combine the results to an array of items, which have the same order as in the selection in the overview.
        return forkJoin(requests$).pipe(
            map(results => {
                if (this.isStickyChannelEnabled) {
                    itemsToLoad = this.updateItems(results);
                }

                const loadedItemsInOrder: OverviewItem[] = [];

                Array.from(itemsToLoad).forEach(([nodeId, itemsForNode]: [number, ItemsForNode]) => {
                    itemsForNode.itemIds.forEach(itemId => {
                        const itemsForNodeItems: { item: Item, nodeId: number, origIndex: number }[] = results.find(resultNodes => {
                            return resultNodes.some(resultNode => resultNode.item.id === itemId);
                        });
                        // if item is not existing, insert error object to be displayed instead
                        const itemLoaded: OverviewItem = Array.isArray(itemsForNodeItems) && itemsForNodeItems.find(item => {
                            return item.item.id === itemId || item.item.masterId === itemId;
                        }).item as OverviewItem
                        || {
                            id: itemId,
                            name: this.i18n.translate('editor.item_not_found', { id: itemId }),
                            nodeId,
                        } as OverviewItem;

                        if (nodeId === -1) {
                            /**
                             * Here we set the nodeId to undefined, as we do with the items selected from the repository browser
                             * in case the "Sticky Channel" option is disabled. This guarantees that the deduplication process
                             * after selecting items from the repository browser works as expected. (See browseForItems method).
                             */
                            itemLoaded.nodeId = undefined;
                        }

                        loadedItemsInOrder[itemsForNode.origIndices[itemId]] = itemLoaded;
                    });
                });
                return loadedItemsInOrder;
            }),
        );
    }

    /**
     * Updates the item id list, as it may contain localized channel item id.
     */
    private updateItems(results: { item: Item<Raw>, nodeId: number, origIndex: number }[][]): Map<number, ItemsForNode> {
        const items: NodeIdObjectId[] = [];
        results.forEach(result => {
            result.forEach(itemData => {
                items.push({nodeId: itemData.nodeId, objectId: itemData.item.id})
            });
        });
        return this.groupItemsByNode(items);
    }

    /**
     * Creates a Map<number, ItemsForNode> from items not linked with a nodeId.
     * The items will be grouped into the nodeId -1.
     */
    private groupItemsWithoutNodes(items: number[]): Map<number, ItemsForNode> {
        const ret = new Map<number, ItemsForNode>();
        const itemsForNode: ItemsForNode = {
            itemIds: items,
            origIndices: {},
        };
        items.forEach((id, index) => {
            // remove all duplicate entries from items ... only use first occurrence of a value
            if (items.indexOf(id) === index) {
                itemsForNode.origIndices[id] = index;
            }
        });
        ret.set(-1, itemsForNode);
        return ret;
    }

    /**
     * Groups the specified items by their node IDs.
     */
    private groupItemsByNode(items: NodeIdObjectId[]): Map<number, ItemsForNode> {
        const groupedItems = new Map<number, ItemsForNode>();
        items.forEach((item, index) => {
            let itemsForNode = groupedItems.get(item.nodeId);
            if (!itemsForNode) {
                itemsForNode = { itemIds: [], origIndices: {} };
                groupedItems.set(item.nodeId, itemsForNode);
            }
            // itemsForNode should be unique - only add if it not already exists
            if (itemsForNode.itemIds.findIndex(itemId => itemId === item.objectId) === -1) {
                itemsForNode.itemIds.push(item.objectId);
                itemsForNode.origIndices[item.objectId] = index;
            }
        });
        return groupedItems;
    }

    /**
     * Determines the FolderItemType based on the current overview settings.
     */
    private determineSelectedItemsType(listType: ListType, selectType: SelectType): FolderItemType {
        if (selectType === SelectType.FOLDER) {
            return 'folder';
        }
        switch (listType) {
            case ListType.PAGE:
                return 'page';
            case ListType.FOLDER:
                return 'folder';
            case ListType.FILE:
                return 'file';
            case ListType.IMAGE:
                return 'image';
            default:
                throw new TagEditorError(`Cannot have selected items in an Overview with the listType being ListType.${listType}.`);
        }
    }

}
