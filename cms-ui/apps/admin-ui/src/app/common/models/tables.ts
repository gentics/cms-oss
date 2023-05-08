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
}

export interface TableLoadResponse<T> {
    rows: TableRow<T>[];
    totalCount: number;
}

export interface TableLoadStartEvent<A = never> {
    options: TableLoadOptions;
    additionalOptions?: A;
}

export interface TableLoadEndEvent<T, A = never> extends TableLoadResponse<T>, TableLoadStartEvent<A> { }

export interface EntityPageResponse<T> {
    entities: T[];
    totalCount: number;
}

export interface TableEntityLoader<T> {

    loadTablePage(options: TableLoadOptions): Observable<TableLoadResponse<T>>;
}
