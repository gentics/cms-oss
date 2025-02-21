import {
    BO_DISPLAY_NAME,
    BO_ID,
    BusinessObject,
    EditableEntity,
    TableLoadEndEvent,
    TableLoadOptions,
    TableLoadResponse,
    TableLoadStartEvent,
} from '@admin-ui/common';
import { BaseTableLoaderService, I18nService } from '@admin-ui/core/providers';
import { AppStateService, SetUIFocusEntity } from '@admin-ui/state';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import {
    BaseComponent,
    ModalService,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TableRow,
    TableSelectAllType,
    TableSortOrder,
    cancelEvent,
    coerceInstance,
} from '@gentics/ui-core';
import { Observable, Subject, combineLatest, of } from 'rxjs';
import { debounceTime, map, switchMap, tap } from 'rxjs/operators';
import { ConfirmDeleteModalComponent } from '../confirm-delete-modal/confirm-delete-modal.component';

export const DELETE_ACTION = 'delete';

@Component({ template: '' })
export abstract class  BaseEntityTableComponent<T, O = T & BusinessObject, A = never> extends BaseComponent implements  OnInit, OnChanges {

    public readonly TableSelectAllType = TableSelectAllType;
    // eslint-disable-next-line @typescript-eslint/naming-convention
    public readonly cancelEvent = cancelEvent;

    @Input()
    public showSearch = false;

    @Input()
    public hideActions = false;

    @Input()
    public selectable = true;

    @Input()
    public selected: string[] = [];

    @Input()
    public multiple = true;

    @Input()
    public activeEntity: string;

    @Input()
    public extraActions: TableAction<O>[] = [];

    @Input()
    public filters: Record<string, any> = {};

    @Output()
    public rowClick = new EventEmitter<TableRow<O>>();

    @Output()
    public actionClick = new EventEmitter<TableActionClickEvent<O>>();

    @Output()
    public selectedChange = new EventEmitter<string[]>();

    @Output()
    public select = new EventEmitter<TableRow<O>>();

    @Output()
    public deselect = new EventEmitter<TableRow<O>>();

    @Output()
    public createClick = new EventEmitter<void>();

    @Output()
    public loadStart = new EventEmitter<TableLoadStartEvent<A>>();

    @Output()
    public loadEnd = new EventEmitter<TableLoadEndEvent<O, A>>();

    protected abstract rawColumns: TableColumn<O>[];
    protected abstract entityIdentifier: NormalizableEntityType;
    protected focusEntityType: NormalizableEntityType | EditableEntity;

    // Table data
    public columns: TableColumn<O>[] = [];
    public rows: TableRow<O>[] = [];
    public actions: TableAction<O>[] = [];
    public totalCount = 0;

    // Data Settings
    public page = 0;
    public perPage = 10;
    public sortBy;
    public sortOrder = TableSortOrder.ASCENDING;
    public query: string;

    // Loading state
    public loading = false;
    public hasError = false;

    protected loadTrigger = new Subject<void>();
    protected actionRebuildTrigger = new Subject<void>();
    protected actionRebuildTrigger$ = this.actionRebuildTrigger.asObservable();

    constructor(
        changeDetector: ChangeDetectorRef,
        protected appState: AppStateService,
        protected i18n: I18nService,
        protected loader: BaseTableLoaderService<T, O, A>,
        protected modalService: ModalService,
    ) {
        super(changeDetector);
        this.booleanInputs.push('showSearch', 'hideActions', 'selectable');
    }

    public ngOnInit(): void {
        // Default the sort-by id to the first sortable one
        if (this.sortBy == null) {
            const canSort = this.rawColumns.find(col => col.sortable);
            if (canSort) {
                this.sortBy = canSort.id;
            }
        }

        this.rebuildColumns();

        this.setupRowLoading();
        this.setupActionLoading();

        // Load the first page
        this.loadTrigger.next();
        this.actionRebuildTrigger.next();
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        coerceInstance(this, this.booleanInputs, changes);

        if (changes.selected) {
            this.selected = (this.selected || []).map(value => {
                if (value != null && typeof value === 'object') {
                    return (value as object)[BO_ID];
                }
                return String(value);
            });
        }

        if (changes.extraActions) {
            this.actionRebuildTrigger.next();
        }
    }

    public reload(): void {
        this.loadTrigger.next();
    }

    public changePageTo(newPage: number): void {
        this.page = newPage;
        this.reload();
    }

    public updateSortBy(newSortBy: string): void {
        this.sortBy = newSortBy;
        this.reload();
    }

    public updateSortOrder(newSortOrder: TableSortOrder): void {
        this.sortOrder = newSortOrder;
        this.reload();
    }

    public updateSearchQuery(newQuery: string): void {
        newQuery = (newQuery || '').trim();
        if (this.query === newQuery) {
            return;
        }
        this.query = newQuery;
        this.page = 0;
        this.reload();
    }

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    public applyFilterValue(field: string, value: any): void {
        this.filters[field] = value;
        this.query = value;

        // Reload the table with the new filter value
        this.loadTrigger.next();
    }

    public updateSelection(newSelection: string[]): void {
        this.selected = newSelection;
        this.selectedChange.emit(newSelection);
    }

    public forwardSelect(row: TableRow<O>): void {
        this.select.emit(row);
    }

    public forwardDeselect(row: TableRow<O>): void {
        this.deselect.emit(row);
    }

    public handleRowClick(row: TableRow<O>): void {
        this.rowClick.emit(row);
    }

    protected translateColumns(columns: TableColumn<O>[]): TableColumn<O>[] {
        return columns.map(column => ({
            ...column,
            label: this.i18n.instant(column.label),
        }));
    }

    protected rebuildColumns(): void {
        // Setup columns with the translated labels
        this.columns = this.translateColumns(this.rawColumns);
    }

    protected applyActions(actions: TableAction<O>[]): void {
        this.actions = [...this.extraActions, ...actions];
    }

    protected setupRowLoading(): void {
        // Setup the loading of the table to be controlable via this component or the loader service.
        this.subscriptions.push(combineLatest([
            this.loader.reload$,
            this.loadTrigger.asObservable(),
        ]).pipe(
            debounceTime(50),
            switchMap(() => {
                const options = this.createTableLoadOptions();
                const additionalOptions = this.createAdditionalLoadOptions();

                this.loading = true;
                this.changeDetector.markForCheck();
                this.loadStart.emit({ options, additionalOptions });

                return this.loadTablePage(options, additionalOptions).pipe(
                    tap(res => {
                        this.loading = false;
                        this.changeDetector.markForCheck();

                        const copy = structuredClone(res) as TableLoadEndEvent<O, A>;

                        // the cloning above does not carry over symbols, therefore we have to copy them manually
                        copy.rows = copy.rows.map((row, idx) => {
                            const original = res.rows[idx].item;
                            const symbols = Object.getOwnPropertySymbols(original);

                            for (const s of symbols) {
                                row.item[s] = original[s];
                            }

                            return row;
                        });

                        copy.options = options;
                        copy.additionalOptions = additionalOptions;
                        this.loadEnd.emit(copy);
                    }),
                );
            }),
        ).subscribe(page => {
            this.rows = page.rows || [];
            this.totalCount = page.totalCount || page.rows?.length || 0;
            this.changeDetector.markForCheck();
        }, error => {
            console.error(error);

            this.rows = [];
            this.totalCount = 0;
            this.hasError = true;
            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    protected setupActionLoading(): void {
        this.subscriptions.push(combineLatest([
            this.actionRebuildTrigger$.pipe(
                debounceTime(50),
            ),
            this.createTableActionLoading(),
        ]).pipe(
            map(([_, actions]) => actions),
        ).subscribe(actions => {
            this.applyActions(actions);
            this.changeDetector.markForCheck();
        }));
    }

    protected createTableActionLoading(): Observable<TableAction<O>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            switchMap(() => of([])),
        );
    }

    protected createTableLoadOptions(): TableLoadOptions {
        // Usually the sort-value is the id of the column. In some cases, it has to be different.
        // Therefore use the sortValue in case it exists and use that one instead.
        let sortValue = this.sortBy;
        if (sortValue) {
            const column = this.columns.find(col => col.id === sortValue);
            if (column && column.sortValue) {
                sortValue = column.sortValue;
            }
        }

        return {
            page: this.page,
            perPage: this.perPage,
            sortBy: sortValue,
            sortOrder: this.sortOrder,
            query: this.query,
            filters: this.filters,
        };
    }

    protected createAdditionalLoadOptions(): A {
        return;
    }

    protected loadTablePage(
        options: TableLoadOptions,
        additionalOptions?: A,
    ): Observable<TableLoadResponse<O>> {
		// Edge case: we have no rows at the moment, but the total shows there is still something - reset.
        if (this.totalCount > 0 && options.page > this.page && this.rows.length < 1) {
            options.page = 0;
        }
        return this.loader.loadTablePage(options, additionalOptions);
    }

    public handleCreateButton(): void {
        this.createClick.emit();
    }

    public handleAction(event: TableActionClickEvent<O>): void {
        switch (event.actionId) {
            case DELETE_ACTION:
                this.deleteEntities(this.getAffectedEntityIds(event)).then(didDelete => {
                    if (didDelete && event.selection) {
                        this.selected = [];
                    }
                    this.loader.reload();
                });
                return;
        }

        this.actionClick.emit(event);
    }

    protected getAffectedEntityIds(event: TableActionClickEvent<O>): string[] {
        if (event.selection) {
            return this.selected;
        }
        return [event.item[BO_ID]];
    }

    protected removeFromSelection(ids: string | string[]): void {
        if (typeof ids === 'string') {
            ids = [ids];
        }

        const copy = this.selected.slice();

        for (const id of ids) {
            const idx = copy.indexOf(id);
            if (idx > -1) {
                copy.splice(idx, 1);
            }
        }

        this.selectedChange.emit(copy);
    }

    protected async deleteEntities(entityIds: string[]): Promise<boolean> {
        // if no row is selected, display modal
        if (!entityIds || entityIds.length < 1) {
            // this.notificationNoneSelected();
            return false;
        }

        const entities: O[] = [];
        for (const id of entityIds) {
            if (await this.loader.canDelete(id)) {
                entities.push(this.loader.getEntityById(id));
            }
        }

        // Can't delete any of the items
        if (entities.length < 1) {
            return false;
        }

        const entityNames = entities.map(entity => entity[BO_DISPLAY_NAME]);

        // open modal to confirm deletion
        const dialog = await this.modalService.fromComponent(
            ConfirmDeleteModalComponent,
            { closeOnOverlayClick: false },
            {
                entityIdentifier: this.entityIdentifier,
                entityNames,
            },
        );
        const confirmed = await dialog.open();

        if (!confirmed) {
            return false;
        }

        for (const singleEntity of entities) {
            // If the current entity is being edited right now, then we need to close the editor.
            if (this.activeEntity === singleEntity[BO_ID]) {
                this.appState.dispatch(new SetUIFocusEntity(this.focusEntityType || this.entityIdentifier, undefined));
            }
            await this.callToDeleteEntity(singleEntity[BO_ID]);
        }

        // Remove the items from the selection, as they have been deleted
        this.removeFromSelection(entities.map(singleEntity => singleEntity[BO_ID]));

        return true;
    }

    protected callToDeleteEntity(id: string): Promise<void> {
        return this.loader.deleteEntity(id, this.createAdditionalLoadOptions());
    }
}
