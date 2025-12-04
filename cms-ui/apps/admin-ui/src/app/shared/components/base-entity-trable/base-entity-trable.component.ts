import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import {
    CoerceOption,
    FALLBACK_TABLE_COLUMN_RENDERER,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TrableRow,
    TrableRowExpandEvent,
    coerceInstance,
} from '@gentics/ui-core';
import { Observable, Subject, Subscription, combineLatest, forkJoin, of } from 'rxjs';
import { debounceTime, map, switchMap } from 'rxjs/operators';
import { BO_ID, BusinessObject, TrableRowReloadOptions } from '../../../common';
import { BaseTrableLoaderService } from '../../../core/providers/base-trable-loader/base-trable-loader.service';

@Component({
    template: '',
    standalone: false,
})
export abstract class BaseEntityTrableComponent<T, O = T & BusinessObject, A = never> implements OnInit, OnChanges, OnDestroy {

    public readonly FALLBACK_TABLE_COLUMN_RENDERER = FALLBACK_TABLE_COLUMN_RENDERER;

    @Input()
    public selectable = false;

    @Input()
    public multiple = true;

    @Input()
    public hideActions = false;

    @Input()
    public activeEntity: string;

    @Input()
    public selected: string[] = [];

    @Input()
    public extraActions: TableAction<O>[] = [];

    @Input()
    public inlineExpansion = false;

    @Input()
    public inlineSelection = false;

    @Output()
    public rowClick = new EventEmitter<TrableRow<O>>();

    @Output()
    public actionClick = new EventEmitter<TableActionClickEvent<O>>();

    @Output()
    public selectedChange = new EventEmitter<string[]>();

    @Output()
    public rowSelect = new EventEmitter<TrableRow<O>>();

    @Output()
    public rowDeselect = new EventEmitter<TrableRow<O>>();

    protected abstract rawColumns: TableColumn<O>[];

    public columns: TableColumn<O>[] = [];
    public rows: TrableRow<O>[] = [];
    public actions: TableAction<O>[] = [];

    protected loadedRows: Record<string, TrableRow<O>> = {};

    protected subscriptions: Subscription[] = [];
    protected booleanInputs: CoerceOption<this>[] = ['selectable', ['multiple', true], 'hideActions', 'inlineExpansion', 'inlineSelection'];

    protected actionRebuildTrigger = new Subject<void>();
    protected actionRebuildTrigger$ = this.actionRebuildTrigger.asObservable();

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected i18n: I18nService,
        protected loader: BaseTrableLoaderService<T, O, A>,
    ) { }

    public ngOnInit(): void {
        // Setup columns with the translated labels
        this.columns = this.translateColumns(this.rawColumns);

        this.subscriptions.push(this.loader.reload$.subscribe(() => {
            this.reload();
        }));

        this.setupActionLoading();
        this.loadRootElements();

        this.actionRebuildTrigger.next();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        coerceInstance(this, this.booleanInputs, changes);

        if (changes.selected) {
            this.selected = (this.selected || []).map((value) => {
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

    public ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    protected setupActionLoading(): void {
        this.subscriptions.push(combineLatest([
            this.actionRebuildTrigger$.pipe(
                debounceTime(50),
            ),
            this.createTableActionLoading(),
        ]).pipe(
            map(([_, actions]) => actions),
        ).subscribe((actions) => {
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

    protected applyActions(actions: TableAction<O>[]): void {
        this.actions = [...this.extraActions, ...actions];
    }

    /**
     * Function to reload the current trable.
     * Reloads all root-rows and recursively all already loaded descendants.
     */
    public reload(): void {
        const options = this.createAdditionalLoadOptions();

        // Mark all rows as loading
        Object.values(this.loadedRows).forEach((row) => {
            row.loading = true;
        });
        // Force view refresh
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();

        this.subscriptions.push(forkJoin(this.rows.map((rootRow) => this.loader.reloadRow(rootRow, options, {
            reloadDescendants: true,
        }))).subscribe((newRootRows) => {
            this.rows = newRootRows;
            this.loadedRows = newRootRows.reduce((acc, row) => {
                acc[row.id] = row;
                return acc;
            }, {});

            this.onLoad();
            this.changeDetector.markForCheck();
        }));
    }

    protected loadRootElements(): void {
        this.subscriptions.push(this.loader.loadRowChildren(null, this.createAdditionalLoadOptions()).subscribe((newRootRows) => {
            this.rows = newRootRows;
            this.loadedRows = newRootRows.reduce((acc, row) => {
                acc[row.id] = row;
                return acc;
            }, {});

            this.onLoad();
            this.changeDetector.markForCheck();
        }));
    }

    protected translateColumns(columns: TableColumn<O>[]): TableColumn<O>[] {
        return columns.map((column) => ({
            ...column,
            label: this.i18n.instant(column.label),
        }));
    }

    public updateRowExpansion(event: TrableRowExpandEvent<O>): void {
        if (event.row) {
            event.row.expanded = event.expanded;
        }

        // Force view refresh
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();
    }

    public reloadRow(row: TrableRow<O>, reloadOptions?: TrableRowReloadOptions): void {
        // Mark the row (and descendants if applicable) as loading
        if (reloadOptions?.reloadDescendants) {
            const updateLoading = (currentRow: TrableRow<O>) => {
                currentRow.loading = true;
                const children = currentRow.children || [];
                for (const child of children) {
                    updateLoading(child);
                }
            };
            updateLoading(row);
        } else {
            row.loading = true;
        }

        // Force view refresh
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.loader.reloadRow(row, this.createAdditionalLoadOptions(), reloadOptions).subscribe((reloadedRow) => {
            // Replace the `row` with the `reloadedRow` in the parent element
            if (row.parent && row.parent.children) {
                row.parent.children = row.parent.children.map((child) => child.id === row.id ? reloadedRow : child);
                // Force view refresh
                this.rows = [...this.rows];
            } else {
                this.rows = this.rows.map((rootRow) => rootRow.id === reloadedRow.id ? reloadedRow : rootRow);
            }
            this.loadedRows[row.id] = reloadedRow;

            this.changeDetector.markForCheck();
        }));

    }

    public loadRow(row: TrableRow<O>): void {
        row.loading = true;
        // Force view refresh
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.loader.loadRowChildren(row, this.createAdditionalLoadOptions()).subscribe((loadedChildren) => {
            // Update the row state
            row.loading = false;
            row.loaded = true;
            row.children = loadedChildren;
            row.hasChildren = loadedChildren.length > 0;
            row.expanded = true;

            // Add the loaded children to the state for later lookup
            for (const child of loadedChildren) {
                this.loadedRows[child.id] = child;
            }

            // Force view refresh
            this.rows = [...this.rows];
            this.changeDetector.markForCheck();

            this.onLoad();
        }));
    }

    public handleRowClick(row: TrableRow<O>): void {
        this.rowClick.emit(row);
    }

    public handleActionClick(action: TableActionClickEvent<O>): void {
        this.actionClick.emit(action);
    }

    public updateSelection(newSelection: string[]): void {
        this.selected = newSelection;
        this.selectedChange.emit(newSelection);
    }

    public forwardSelect(row: TrableRow<O>): void {
        this.rowSelect.emit(row);
    }

    public forwardDeselect(row: TrableRow<O>): void {
        this.rowDeselect.emit(row);
    }

    protected onLoad(): void {}

    protected createAdditionalLoadOptions(): A {
        return null;
    }
}
