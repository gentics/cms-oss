import { TableActionClickEvent, TableRow, TableSortOrder } from '@gentics/ui-core';
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

    canDelete(entityId: string | number): Promise<boolean>;
    deleteEntity(entityId: string | number): Promise<void>
}

export interface PackageTableEntityLoader<T, A = never> extends TableEntityLoader<T, A>  {

    addToDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void>;
    removeFromDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void>;
}

export interface TrableRowReloadOptions {
    /**
     * If it should reload loaded/expanded descendants of the row as well.
     */
    reloadDescendants?: boolean;
}

export interface EntityTableActionClickEvent<T> extends TableActionClickEvent<T> {
    /** The selected items when `selection` is `true` */
    selectedItems?: T[];
}

export const DELETE_ACTION = 'delete';
export const EDIT_ACTION = 'edit';

export const MOVE_TO_TOP_ACTION = 'moveToTop';
export const MOVE_UP_ACTION = 'moveUp';
export const MOVE_DOWN_ACTION = 'moveDown';
export const MOVE_TO_BOTTOM_ACTION = 'moveToBottom';

export const MANAGE_PROJECT_ASSIGNMENT_ACTION = 'manageProjects';
export const ASSIGN_TO_PROJECTS_ACTION = 'assignToProjects';
export const UNASSIGN_FROM_PROJECTS_ACTION = 'unassignFromProjects';

export const MANAGE_TAGS_ACTIONS = 'manageTags';

export const MANAGE_SCHEMA_ASSIGNMENT_ACTION = 'manageSchemaAssignment';
export const MANAGE_MICROSCHEMA_ASSIGNMENT_ACTION = 'manageMicroschemaAssignment';

export const ASSIGN_CONSTRUCT_TO_NODES_ACTION = 'assignConstructToNodes';
export const ASSIGN_CONSTRUCT_TO_CATEGORY_ACTION = 'assignConstructToCategory';
export const COPY_CONSTRUCT_ACTION = 'copyConstruct';
