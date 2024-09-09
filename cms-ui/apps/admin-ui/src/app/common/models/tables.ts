import { TableRow, TableSortOrder } from '@gentics/ui-core';
import { Observable } from 'rxjs';

export interface TableLoadOptions {
    /** The page to load */
    page: number;
    /** How many items are to be loaded in a page */
    perPage: number;
    /** By which property it should sort */
    sortBy?: string;
    /** Which sort order should be used */
    sortOrder?: TableSortOrder;
    /** The search query to filter results */
    query?: string;
    /** The applied filters. */
    filters?: Record<string, any>;
}

export interface TableLoadResponse<T> {
    rows: TableRow<T>[];
    totalCount: number;
    hasError?: boolean;
}

export interface TableLoadStartEvent<A = never> {
    options: TableLoadOptions;
    additionalOptions?: A;
}

export interface TableLoadEndEvent<T, A = never> extends TableLoadResponse<T>, TableLoadStartEvent<A> { }

export interface TableSortEvent<T> {
    row: TableRow<T>;
    from: number;
    to: number;
}

export interface EntityPageResponse<T> {
    entities: T[];
    totalCount: number;
}

export interface TableEntityLoader<T, A = never> {

    readonly reload$: Observable<void>;

    loadTablePage(options: TableLoadOptions, additionalOptions?: A): Observable<TableLoadResponse<T>>;
    reload(): void;

    getEntityById(entityId: string | number): T;
    canDelete(entityId: string | number): Promise<boolean>;
    deleteEntity(entityId: string | number): Promise<void>
}

export interface PackageTableEntityLoader<T, A = never> extends TableEntityLoader<T, A>  {

    addToDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void>;
    removeFromDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void>;
}
