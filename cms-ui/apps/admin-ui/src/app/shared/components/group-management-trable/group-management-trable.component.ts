import { GroupBO, forEachTreeNode } from '@admin-ui/common';
import {
    ErrorHandler,
    GroupManagementTrableLoaderOptions,
    GroupManagementTrableLoaderService,
    GroupOperations,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, SimpleChanges } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { Group } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn, TrableRow } from '@gentics/ui-core';
import { Observable, Subject, combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { BaseEntityTrableComponent } from '../base-entity-trable/base-entity-trable.component';
import { CreateGroupModalComponent } from '../create-group-modal/create-group-modal.component';
import { MoveGroupsModalComponent } from '../move-groups-modal/move-groups-modal.component';

const CREATE_SUB_GROUP_ACTION = 'createSubGroup';
const MOVE_GROUP_ACTION = 'move';
const DELETE_ACTION = 'delete';

const GROUP_ENTITY_IDENTIFIER = 'group';

/**
 * How long typing pauses before the tree is filtered. Same value the flat list uses for its
 * search (`base-entity-table.component.ts:265`).
 */
const SEARCH_DEBOUNCE_MS = 50;

/**
 * Shortest query that actually filters. A single character matches almost every group, which
 * would put the whole tree into the DOM in one change-detection run.
 */
const MIN_SEARCH_QUERY_LENGTH = 2;

/**
 * Upper bound for the rows a search may reveal on its own.
 *
 * Auto-expanding *every* path to a match is what made a common two-character substring render
 * the complete tree: QA measured 3 410 rows at ~2 500 ms for a single keystroke, i.e. roughly
 * 0.73 ms per row, against a 200 ms budget. The limit therefore counts **rendered rows**, not
 * matches - a single match deep inside a wide branch drags its whole sibling set into the DOM,
 * so a match limit would not bound the cost.
 *
 * 200 rows is the performance review's "comfortable" figure and leaves headroom below the 200 ms
 * budget. Rows the user expands by hand are deliberately not counted against it.
 */
const MAX_VISIBLE_SEARCH_ROWS = 100;

/**
 * Hierarchical group tree for the group *management* master view.
 *
 * Deliberately a sibling of - and not a mode of - `GroupTrableComponent`: that component is
 * specific to permission editing (`folder-detail`), renders permission icons via the
 * `__fallback__` renderer and hijacks the row click. This component only navigates and offers
 * the three management actions.
 */
@Component({
    selector: 'gtx-group-management-trable',
    templateUrl: './group-management-trable.component.html',
    styleUrls: ['./group-management-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class GroupManagementTrableComponent
    extends BaseEntityTrableComponent<Group, GroupBO, GroupManagementTrableLoaderOptions> {

    public rawColumns: TableColumn<GroupBO>[] = [
        {
            id: 'name',
            label: 'shared.group_name',
            fieldPath: 'name',
        },
        {
            id: 'description',
            label: 'shared.description',
            fieldPath: 'description',
        },
    ];

    /** The current, untrimmed search query. Empty means "show the whole tree". */
    public query = '';

    /** Whether the tree is currently being (re-)loaded. */
    public loading = false;

    /** The complete, unfiltered tree. `rows` is either this or a filtered projection of it. */
    protected allRows: TrableRow<GroupBO>[] = [];

    /** The expansion state from before the running search, so it can be restored when it is cleared. */
    protected expansionBeforeSearch: Set<string> | null = null;

    /**
     * Filter-result rows, keyed by row id, reused across filter runs.
     *
     * `gtx-trable` renders child rows with `*ngFor="let child of row.children; trackRow"`
     * (`trable.component.html:177`) - that is a microsyntax key without an expression, **not** a
     * `trackBy`, so Angular falls back to identity tracking. Handing it a fresh `{ ...row }` per
     * keystroke would therefore destroy and re-create the DOM of every visible child row. Reusing
     * the same object and refreshing it in place keeps those views alive.
     *
     * Cleared whenever the tree is reloaded, because the row objects it mirrors are replaced then.
     */
    protected filteredRowCache = new Map<string, TrableRow<GroupBO>>();

    /** Feeds the debounced filter pipeline. */
    protected queryChange = new Subject<string>();

    /** How many groups the running query matches, regardless of whether they are revealed. */
    public searchMatchCount = 0;

    /** How many of those matches the row budget actually made visible. */
    public searchVisibleMatchCount = 0;

    /** Whether the row budget stopped the search from revealing every match. */
    public searchTruncated = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: GroupManagementTrableLoaderService,
        protected modalService: ModalService,
        protected permissions: PermissionsService,
        protected errorHandler: ErrorHandler,
        protected operations: GroupOperations,
        protected appState: AppStateService,
    ) {
        super(changeDetector, i18n, loader);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.subscriptions.push(this.queryChange.pipe(
            debounceTime(SEARCH_DEBOUNCE_MS),
            distinctUntilChanged(),
        ).subscribe((query) => this.applyQuery(query)));
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        // Makes a deeplinked group visible, no matter whether the tree or the active entity
        // arrives first.
        if (changes.activeEntity && this.expandToActiveEntity()) {
            this.applyFilter();
            this.changeDetector.markForCheck();
        }
    }

    /* -------------------------------------------------------------------------------------- */
    /* Loading                                                                                  */
    /* -------------------------------------------------------------------------------------- */

    protected override loadRootElements(): void {
        this.loadTree(null);
    }

    /**
     * Reloads the whole tree with a single `GET /group/load` and restores the expansion state.
     *
     * The inherited implementation reloads every root row via `loadEntityRow` and then walks the
     * descendants. Since `GET /group/{id}` answers without the `children` property, that path is
     * both fragile and more expensive than simply re-running the one request that returns
     * everything.
     */
    public override reload(): void {
        this.loadTree(this.collectExpandedIds(this.allRows));
    }

    private loadTree(expandedIds: Set<string> | null): void {
        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.loader.loadRowChildren(null, this.createAdditionalLoadOptions()).subscribe({
            next: (rootRows) => {
                this.loading = false;
                this.allRows = rootRows;

                if (expandedIds != null) {
                    this.applyExpansion(this.allRows, expandedIds);
                }

                this.loadedRows = {};
                this.filteredRowCache.clear();
                forEachTreeNode(this.allRows, (row) => {
                    this.loadedRows[row.id] = row;
                });
                this.expandToActiveEntity();
                this.applyFilter();

                this.onLoad();
                this.changeDetector.markForCheck();
            },
            error: (err) => {
                this.loading = false;
                this.changeDetector.markForCheck();
                this.errorHandler.catch(err);
            },
        }));
    }

    /* -------------------------------------------------------------------------------------- */
    /* Client-side search                                                                       */
    /* -------------------------------------------------------------------------------------- */

    /**
     * Records the typed query immediately (so the input stays in sync) and hands it to the
     * debounced filter pipeline. Filtering itself happens in {@link applyQuery}.
     */
    public updateSearchQuery(newQuery: string): void {
        const updated = newQuery || '';
        if (this.query === updated) {
            return;
        }

        this.query = updated;
        this.queryChange.next(updated);
    }

    /** Runs the expansion bookkeeping and the filter for a settled query. */
    protected applyQuery(query: string): void {
        if (this.isSearchQuery(query)) {
            if (this.expansionBeforeSearch == null) {
                // A search takes over the expansion state, so remember what the user had before.
                this.expansionBeforeSearch = this.collectExpandedIds(this.allRows);
            }
        } else if (this.expansionBeforeSearch != null) {
            this.applyExpansion(this.allRows, this.expansionBeforeSearch);
            this.expansionBeforeSearch = null;
        }

        this.applyFilter();
        this.changeDetector.markForCheck();
    }

    /** Whether a query is long enough to filter by. */
    protected isSearchQuery(query: string): boolean {
        return (query || '').trim().length >= MIN_SEARCH_QUERY_LENGTH;
    }

    protected applyFilter(): void {
        if (!this.isSearchQuery(this.query)) {
            // Always a fresh array, so that `gtx-trable` picks the change up.
            this.rows = [...this.allRows];
            this.resetSearchResultInfo();
            return;
        }

        const query = this.query.trim().toLowerCase();
        const filtered = this.filterRows(this.allRows, query, undefined);

        this.rows = filtered;
        this.revealMatches(filtered, query);
    }

    protected resetSearchResultInfo(): void {
        this.searchMatchCount = 0;
        this.searchVisibleMatchCount = 0;
        this.searchTruncated = false;
    }

    /**
     * Opens the paths to the first matches in document order until {@link MAX_VISIBLE_SEARCH_ROWS}
     * is used up, then stops expanding. Everything below stays collapsed but remains reachable by
     * hand, and {@link searchTruncated} tells the template to say so.
     *
     * {@link filterRows} leaves every row collapsed, so this is the only thing that expands rows
     * during a search.
     */
    protected revealMatches(rows: TrableRow<GroupBO>[], query: string): void {
        // Root rows are always rendered, whether they are expanded or not.
        let visibleRows = rows.length;
        let matchCount = 0;
        let budgetAvailable = true;

        const reveal = (current: TrableRow<GroupBO>[]): void => {
            for (const row of current) {
                if (this.matchesQuery(row.item, query)) {
                    matchCount++;

                    if (budgetAvailable) {
                        const cost = this.expansionCost(row);
                        if (visibleRows + cost <= MAX_VISIBLE_SEARCH_ROWS) {
                            visibleRows += cost;
                            this.expandAncestors(row);
                        } else {
                            // Document order from here on, so nothing after this fits either.
                            budgetAvailable = false;
                        }
                    }
                }

                // Keep walking even once the budget is gone - `matchCount` must stay complete.
                reveal(row.children || []);
            }
        };
        reveal(rows);

        this.searchMatchCount = matchCount;
        this.searchVisibleMatchCount = this.countVisibleMatches(rows, query);
        this.searchTruncated = this.searchVisibleMatchCount < matchCount;
    }

    /**
     * How many additional rows would be rendered if the path to `row` were opened.
     *
     * Expanding an ancestor reveals exactly its (already filtered) children. Ancestors are always
     * expanded as a whole path, so a collapsed ancestor can never sit above an expanded one and
     * the sum cannot double count.
     */
    protected expansionCost(row: TrableRow<GroupBO>): number {
        let cost = 0;

        for (let ancestor = row.parent; ancestor != null; ancestor = ancestor.parent) {
            if (!ancestor.expanded) {
                cost += (ancestor.children || []).length;
            }
        }

        return cost;
    }

    protected expandAncestors(row: TrableRow<GroupBO>): void {
        for (let ancestor = row.parent; ancestor != null; ancestor = ancestor.parent) {
            ancestor.expanded = true;
        }
    }

    /**
     * Counts the matches the user can actually see. Only descends into expanded rows, so the walk
     * is bounded by the row budget rather than by the size of the tree.
     */
    protected countVisibleMatches(rows: TrableRow<GroupBO>[], query: string): number {
        let count = 0;

        const walk = (current: TrableRow<GroupBO>[]): void => {
            for (const row of current) {
                if (this.matchesQuery(row.item, query)) {
                    count++;
                }
                if (row.expanded) {
                    walk(row.children || []);
                }
            }
        };
        walk(rows);

        return count;
    }

    /**
     * Keeps a row when it matches itself or when any of its descendants match, so the path to a
     * hit is *retained*. Every row comes back collapsed - {@link revealMatches} decides which
     * paths are opened, within the row budget.
     *
     * The returned rows are copies - the unfiltered tree in {@link allRows} keeps its own
     * expansion state and is restored verbatim once the query is cleared. The copies come from
     * {@link filteredRowCache} so that their object identity survives across filter runs.
     */
    protected filterRows(rows: TrableRow<GroupBO>[], query: string, parent?: TrableRow<GroupBO>): TrableRow<GroupBO>[] {
        const result: TrableRow<GroupBO>[] = [];

        for (const row of rows) {
            let copy = this.filteredRowCache.get(row.id);
            if (copy == null) {
                copy = {} as TrableRow<GroupBO>;
                this.filteredRowCache.set(row.id, copy);
            }

            // The children need `copy` as their parent, so recurse before `copy` is filled.
            const children = this.filterRows(row.children || [], query, copy);

            if (children.length === 0 && !this.matchesQuery(row.item, query)) {
                continue;
            }

            // Equivalent to the former `{ ...row, parent, children }`, but in place.
            Object.assign(copy, row, {
                parent,
                children,
                hasChildren: children.length > 0,
                expanded: false,
            });
            result.push(copy);
        }

        return result;
    }

    /**
     * The tree search matches `name` and `description` only - `id` and `globalId` are
     * deliberately excluded (design decision D5).
     *
     * This is a known and accepted difference to the flat list view, which searches server-side
     * through the `q` parameter of `GET /group` and therefore also matches `id` and `globalId`.
     * Please do not "fix" it here: mirroring it would mean either a second, server-side search
     * path for the tree (`GET /group/load` supports no filtering at all) or re-implementing the
     * backend's `ResolvableFilter` semantics on the client.
     * @param group The group to test.
     * @param query The search query, already trimmed and lower-cased.
     */
    protected matchesQuery(group: GroupBO, query: string): boolean {
        return (group.name || '').toLowerCase().includes(query)
          || (group.description || '').toLowerCase().includes(query);
    }

    /* -------------------------------------------------------------------------------------- */
    /* Expansion bookkeeping                                                                    */
    /* -------------------------------------------------------------------------------------- */

    protected collectExpandedIds(rows: TrableRow<GroupBO>[]): Set<string> {
        const expandedIds = new Set<string>();

        forEachTreeNode(rows, (row) => {
            if (row.expanded) {
                expandedIds.add(row.id);
            }
        });

        return expandedIds;
    }

    protected applyExpansion(rows: TrableRow<GroupBO>[], expandedIds: Set<string>): void {
        forEachTreeNode(rows, (row) => {
            row.expanded = row.hasChildren && expandedIds.has(row.id);
        });
    }

    /**
     * Expands all ancestors of the currently active entity, so a deeplinked group is visible
     * without the user having to expand the path manually.
     * @returns Whether anything had to be expanded.
     */
    protected expandToActiveEntity(): boolean {
        const activeRow = this.activeEntity ? this.loadedRows[this.activeEntity] : null;
        if (activeRow == null) {
            return false;
        }

        let expandedAny = false;
        for (let ancestor = activeRow.parent; ancestor != null; ancestor = ancestor.parent) {
            if (!ancestor.expanded) {
                ancestor.expanded = true;
                expandedAny = true;
            }
        }

        return expandedAny;
    }

    /* -------------------------------------------------------------------------------------- */
    /* Row actions                                                                              */
    /* -------------------------------------------------------------------------------------- */

    protected override createTableActionLoading(): Observable<TableAction<GroupBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.createGroup').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.moveGroup').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('group.deleteGroup').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canCreate, canMove, canDelete]) => {
                const actions: TableAction<GroupBO>[] = [
                    {
                        id: CREATE_SUB_GROUP_ACTION,
                        icon: 'add',
                        label: this.i18n.instant('shared.create_new_sub_group_button'),
                        enabled: canCreate,
                        type: 'success',
                        single: true,
                    },
                    {
                        id: MOVE_GROUP_ACTION,
                        icon: 'subdirectory_arrow_right',
                        label: this.i18n.instant('shared.move'),
                        enabled: canMove,
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        type: 'alert',
                        single: true,
                    },
                ];

                // Actions the user may not perform are dropped instead of being rendered as
                // disabled buttons: `gtx-trable` does not honour `TableAction.enabled`. Its
                // template binds the non-existent `action.disabled`
                // (trable.component.html:43 and :165), so the pipe always yields `false` and
                // every button stays clickable - unlike the flat `gtx-table`
                // (table.component.html:166), which binds `!(action | gtxTableActionEnabled)`.
                // Fixing that belongs into ui-core, which is out of scope for this component.
                return actions.filter((action) => action.enabled === true);
            }),
        );
    }

    public override handleActionClick(event: TableActionClickEvent<GroupBO>): void {
        switch (event.actionId) {
            case CREATE_SUB_GROUP_ACTION:
                void this.createSubgroup(event.item.id);
                return;

            case MOVE_GROUP_ACTION:
                void this.moveGroup(event.item.id);
                return;

            case DELETE_ACTION:
                void this.deleteGroup(event.item);
                return;
        }

        super.handleActionClick(event);
    }

    protected async createSubgroup(parentGroupId: number): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(
                CreateGroupModalComponent,
                { closeOnOverlayClick: false, width: '50%' },
                { parentGroupId },
            );
            const created = await dialog.open();

            if (!created) {
                return;
            }

            this.loader.reload();
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }

    protected async moveGroup(groupId: number): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(
                MoveGroupsModalComponent,
                { closeOnOverlayClick: false, width: '50%' },
                { sourceGroupIds: [groupId] },
            );
            await dialog.open();

            this.loader.reload();
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }

    /**
     * Deletes a group after an explicit confirmation which - unlike the generic delete dialog of
     * the flat list - warns that the subgroups disappear from the tree along with it.
     */
    protected async deleteGroup(group: GroupBO): Promise<void> {
        try {
            const dialog = await this.modalService.dialog({
                // The group name goes into the `title`, which the modal interpolates safely.
                // The `body` is rendered via `bypassSecurityTrustHtml`, so no user-controlled
                // value may be interpolated into it.
                title: this.i18n.instant('modal.confirm_delete_group_title', { groupName: group.name }),
                body: this.i18n.instant('modal.confirm_delete_group_message'),
                buttons: [
                    {
                        label: this.i18n.instant('common.cancel_button'),
                        returnValue: false,
                        flat: true,
                        type: 'secondary',
                    },
                    {
                        label: this.i18n.instant('modal.confirm_delete_group_button'),
                        returnValue: true,
                        type: 'alert',
                    },
                ],
            }, {
                closeOnOverlayClick: false,
            });

            const confirmed = await dialog.open();

            if (!confirmed) {
                return;
            }

            // Close the detail panel when the group which is currently open is the one being deleted.
            if (this.activeEntity === String(group.id)) {
                this.appState.dispatch(new SetUIFocusEntity(GROUP_ENTITY_IDENTIFIER, undefined));
            }

            await this.operations.delete(group.id).toPromise();
            this.loader.reload();
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }
}
