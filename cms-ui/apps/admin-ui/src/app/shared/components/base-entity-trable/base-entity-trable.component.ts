import { BusinessObject } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import { ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import {
    coerceInstance,
    CoerceOption,
    FALLBACK_TABLE_COLUMN_RENDERER,
    TableAction,
    TableActionClickEvent,
    TableColumn,
    TrableRow,
} from '@gentics/ui-core';
import { forkJoin, Observable, Subscription } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';

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
    public selection: string[] = [];

    @Input()
    public inlineExpansion = false;

    @Input()
    public inlineSelection = false;

    @Output()
    public rowClick = new EventEmitter<TrableRow<O>>();

    @Output()
    public actionClick = new EventEmitter<TableActionClickEvent<O>>();

    @Output()
    public selectionChange = new EventEmitter<string[]>();

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

        this.loadRootElements();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        coerceInstance(this, this.booleanInputs, changes);
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    public reload(): void {
        const storeClone = { ...this.loader.flatStore };
        this.loader.resetStore();

        this.loader.loadRowChildren(null, this.createAdditionalLoadOptions()).pipe(
            switchMap(rootRows => {
                if (!Array.isArray(rootRows)) {
                    rootRows = [rootRows];
                }

                const loadQueue: Observable<any>[] = [];

                // Delete all root rows
                Object.entries(storeClone).forEach(([id, row]) => {
                    if (row.level === 0) {
                        delete storeClone[id];
                    } else if (row.loaded) {
                        loadQueue.push(this.loader.loadRowChildren(row, this.createAdditionalLoadOptions()));
                    }
                });

                return forkJoin(loadQueue).pipe(
                    tap(() => {
                        this.rows = rootRows as TrableRow<O>[];
                        this.onLoad();
                        this.changeDetector.markForCheck();
                    }),
                );
            }),
        )
    }

    protected loadRootElements(): void {
        this.subscriptions.push(this.loader.loadRowChildren(null, this.createAdditionalLoadOptions()).subscribe(rows => {
            this.rows = rows as TrableRow<O>[];
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

    public updateRowExpansion(event: { row: TrableRow<O>, expanded: boolean }): void {
        if (event.row) {
            event.row.expanded = event.expanded;
        }
        this.rows = [...this.rows];
        this.changeDetector.markForCheck();
    }

    public loadRow(row: TrableRow<O>): void {
        this.subscriptions.push(this.loader.loadRowChildren(row, this.createAdditionalLoadOptions()).subscribe(() => {
            this.rows = [...this.rows];
            this.onLoad();
            this.changeDetector.markForCheck();
        }));
    }

    public handleRowClick(row: TrableRow<O>): void {
        this.rowClick.emit(row);
    }

    public handleActionClick(action: TableActionClickEvent<O>): void {
        this.actionClick.emit(action);
    }

    public updateSelection(newSelection: string[]): void {
        this.selection = newSelection;
        this.selectionChange.emit(newSelection);
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
