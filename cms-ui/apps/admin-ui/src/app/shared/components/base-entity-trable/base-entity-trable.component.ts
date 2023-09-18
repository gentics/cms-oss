import { BO_ID, BusinessObject } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
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
import { Observable, Subscription, forkJoin, Subject, combineLatest, of } from 'rxjs';
import { debounceTime, map, switchMap } from 'rxjs/operators';

@Component({ template: '' })
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
    public select = new EventEmitter<TrableRow<O>>();

    @Output()
    public deselect = new EventEmitter<TrableRow<O>>();

    protected abstract rawColumns: TableColumn<O>[];

    public columns: TableColumn<O>[] = [];
    public rows: TrableRow<O>[] = [];
    public actions: TableAction<O>[] = [];

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

        this.subscriptions.push(this.loader.refreshView$.subscribe(() => {
            this.rows = [...this.rows];
            this.changeDetector.markForCheck();
        }));

        this.setupActionLoading();
        this.loadRootElements();

        this.actionRebuildTrigger.next();
    }

    public ngOnChanges(changes: SimpleChanges): void {
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

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
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

    protected applyActions(actions: TableAction<O>[]): void {
        this.actions = [...this.extraActions, ...actions];
    }

    /**
     * Function to reload the current trable.
     * Simply reloads all currently loaded/visible rows, via `reloadRow` and waits for all to complete.
     * Once it's done, re-assigns the rows to trigger a change detection for angular.
     */
    public reload(): void {
        const loaders: Observable<TrableRow<O>>[] = [];
        const options = this.createAdditionalLoadOptions();

        Object.values(this.loader.flatStore).forEach(row => {
            loaders.push(this.loader.reloadRow(row, options));
        });

        this.subscriptions.push(forkJoin(loaders).subscribe(() => {
            this.rows = [...this.rows];
            this.onLoad();
            this.changeDetector.markForCheck();
        }));
    }

    protected loadRootElements(): void {
        this.subscriptions.push(this.loader.loadRowChildren(null, this.createAdditionalLoadOptions()).subscribe(rows => {
            this.rows = rows;
            this.onLoad();
            this.changeDetector.markForCheck();
        }));
    }

    protected translateColumns(columns: TableColumn<O>[]): TableColumn<O>[] {
        return columns.map(column => ({
            ...column,
            label: this.i18n.instant(column.label),
        }));
    }

    public updateRowExpansion(event: TrableRowExpandEvent<O>): void {
        if (event.row) {
            event.row.expanded = event.expanded;
        }
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();
    }

    public reloadRow(row: TrableRow<O>): void {
        this.subscriptions.push(this.loader.reloadRow(row, this.createAdditionalLoadOptions()).subscribe(() => {
            this.rows = [...this.rows];
            this.changeDetector.markForCheck();
        }));

        row.loading = true;
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();
    }

    public loadRow(row: TrableRow<O>): void {
        this.subscriptions.push(this.loader.loadRowChildren(row, this.createAdditionalLoadOptions(), true).subscribe(() => {
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
        this.select.emit(row);
    }

    public forwardDeselect(row: TrableRow<O>): void {
        this.deselect.emit(row);
    }

    protected onLoad(): void {}

    protected createAdditionalLoadOptions(): A {
        return null;
    }
}
