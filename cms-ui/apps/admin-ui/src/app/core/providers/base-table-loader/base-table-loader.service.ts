import { AppStateService } from '@admin-ui/state';
import { BaseListOptionsWithPaging, NormalizableEntityType, PagingSortOrder } from '@gentics/cms-models';
import { TableRow, TableSortOrder } from '@gentics/ui-core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BO_ID, BusinessObject, EntityPageResponse, TableEntityLoader, TableLoadOptions, TableLoadResponse } from '../../../common/models';
import { EntityManagerService } from '../entity-manager';

interface PagingationCreationOptions {
    /** If it should lowercase the `sortBy` field. Defaults to `true` */
    lowerCase?: boolean;
}

export abstract class BaseTableLoaderService<T, O = T & BusinessObject, A = never> implements TableEntityLoader<O> {

    private reloadSubject = new BehaviorSubject<void>(null);

    public reload$ = this.reloadSubject.asObservable();

    constructor(
        protected entityIdentifier: NormalizableEntityType | null,
        protected entityManager: EntityManagerService,
        protected appState: AppStateService,
    ) {}

    public abstract canDelete(entityId: string | number): Promise<boolean>;

    public abstract deleteEntity(entityId: string | number, additonalOptions?: A): Promise<void>;

    protected abstract loadEntities(options: TableLoadOptions, additionalOptions?: A): Observable<EntityPageResponse<O>>;

    public loadTablePage(options: TableLoadOptions, addtionalOptions?: A): Observable<TableLoadResponse<O>> {
        return this.loadEntities(options, addtionalOptions).pipe(
            tap(page => {
                if (this.entityIdentifier) {
                    this.entityManager.addEntities(this.entityIdentifier, page.entities as any);
                }
            }),
            map(page => {
                const rows = page.entities.map(bo => this.mapToTableRow(bo));

                return {
                    rows,
                    totalCount: page.totalCount,
                };
            }),
        );
    }

    public reload(): void {
        this.reloadSubject.next(null);
    }

    protected createDefaultOptions(options: TableLoadOptions, config?: PagingationCreationOptions): BaseListOptionsWithPaging<T> {
        const loadOptions: BaseListOptionsWithPaging<T> = {
            page: options.page,
            pageSize: options.perPage,
        };

        if (options.sortBy) {
            loadOptions.sort = {
                attribute: (config?.lowerCase ?? true) ? options.sortBy.toLowerCase() as any : options.sortBy,
                sortOrder: this.convertSortOrder(options.sortOrder),
            };
        }

        if (options.query) {
            loadOptions.q = options.query;
        }

        return loadOptions;
    }

    protected convertSortOrder(tableOrder: TableSortOrder): PagingSortOrder {
        switch (tableOrder) {
            case TableSortOrder.ASCENDING:
                return PagingSortOrder.Asc;
            case TableSortOrder.DESCENDING:
                return PagingSortOrder.Desc
            default:
                return PagingSortOrder.None;
        }
    }

    public mapToTableRow(bo: O): TableRow<O> {
        return {
            id: bo[BO_ID],
            item: bo,
        };
    }
}
