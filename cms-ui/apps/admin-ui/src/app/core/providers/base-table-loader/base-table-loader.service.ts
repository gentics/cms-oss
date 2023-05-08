import { AppStateService } from '@admin-ui/state';
import { BaseListOptionsWithPaging, NormalizableEntityType, PagingSortOrder, PermissionListResponse } from '@gentics/cms-models';
import { TableRow, TableSortOrder } from '@gentics/ui-core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BO_ID, BO_PERMISSIONS, BusinessObject, EntityPageResponse, TableEntityLoader, TableLoadOptions, TableLoadResponse } from '../../../common/models';
import { EntityManagerService } from '../entity-manager';

export abstract class BaseTableLoaderService<T, O = T & BusinessObject, A = never> implements TableEntityLoader<O> {

    private reloadSubject = new BehaviorSubject<void>(null);

    public reload$ = this.reloadSubject.asObservable();

    protected storedEntities: { [key: string]: O } = {};

    constructor(
        protected entityIdentifier: NormalizableEntityType | null,
        protected entityManager: EntityManagerService,
        protected appState: AppStateService,
    ) {}

    public abstract canDelete(entityId: string | number): Promise<boolean>;

    public abstract deleteEntity(entityId: string | number): Promise<void>;

    protected abstract loadEntities(options: TableLoadOptions, additionalOptions?: A): Observable<EntityPageResponse<O>>;

    public loadTablePage(options: TableLoadOptions, addtionalOptions?: A): Observable<TableLoadResponse<O>> {
        return this.loadEntities(options, addtionalOptions).pipe(
            tap(page => {
                page.entities.forEach(bo => this.storeEntity(bo));
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

    public storeEntity(entity: O, id?: string): void {
        if (!id) {
            id = entity[BO_ID];
        }
        this.storedEntities[id] = entity;
    }

    public removeEntity(id: string): O {
        const element = this.storedEntities[id];
        delete this.storedEntities[id];
        return element;
    }

    public resetStore(): void {
        this.storedEntities = {};
    }

    public getEntityById(entityId: string | number): O {
        return this.storedEntities[entityId];
    }

    public getEntitiesByIds(entityIds: (string | number)[]): O[] {
        return entityIds.map(id => this.storedEntities[id]);
    }

    public reload(): void {
        this.reloadSubject.next(null);
    }

    protected createDefaultOptions(options: TableLoadOptions): BaseListOptionsWithPaging<T> {
        const loadOptions: BaseListOptionsWithPaging<T> = {
            page: options.page,
            pageSize: options.perPage,
        };

        if (options.sortBy) {
            loadOptions.sort = {
                attribute: options.sortBy.toLowerCase() as any,
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

    protected applyPermissions(bos: O[], response: PermissionListResponse<any>): void {
        if (!response?.perms) {
            return;
        }

        for (const bo of bos) {
            const perms = response.perms[bo[BO_ID]];
            if (!perms) {
                continue;
            }
            bo[BO_PERMISSIONS] = perms;
        }
    }

    public mapToTableRow(bo: O): TableRow<O> {
        return {
            id: bo[BO_ID],
            item: bo,
        };
    }
}
