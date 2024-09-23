import { ColorThemes } from './colors';

export const FALLBACK_TABLE_COLUMN_RENDERER = '__fallback__';

/** The sort order that can be be selected. */
export enum TableSortOrder {
    ASCENDING = 'asc',
    DESCENDING = 'desc',
}

/** The different types that are avialable for how the multi select can be used. */
export enum TableSelectAllType {
    /** The multi select is removed entirely and each item has to be selected manually. */
    NONE = 'none',
    /** The multi select toggles all currently displayed items/rows. */
    PAGE = 'page',
    /** The multi select emits a selectAll event which the parent component has to handle. */
    ALL = 'all',
}

/** A function to map a column value to the final display value. */
export type TableColumnMappingFn<T> = (value: any, column: TableColumn<T>) => any;

/** A column which is displayed in a table. */
export interface TableColumn<T> {
    /** The ID of the Column. */
    id: string;
    /** The label of the column which will be shown in the Header. */
    label?: string;
    /** Arbitrary additional data that should be saved to this column. */
    data?: any;
    /** The field this column displays. */
    fieldPath?: string | symbol | (string | symbol)[];
    /** Mapping function which will be executed for the found value. */
    mapper?: TableColumnMappingFn<T>;
    /** Alignment of the column values. */
    align?: 'left' | 'center' | 'right';
    /** If the column can be sorted by. */
    sortable?: boolean;
    /** The sort-value that should be used when actually sorting */
    sortValue?: string;
    /** If this column can be used for clicking. */
    clickable?: boolean;
    /** Override of the click which allows the implementation to handle the click manually. */
    clickOverride?: (row: TableRow<T>, column: TableColumn<T>, event: MouseEvent) => void;
}

/**
 * Represents a Row in a Table.
 * Usually one row is linked to a more complex item which it displays.
 */
export interface TableRow<T> {
    /** ID of the row. */
    id: string;
    /** The hash of this row. Is used in conjunction with the ID to detect changes properly. */
    hash?: string | number;
    /** The item this row represents. */
    item: T;
}

/** Represents a single trable row, which can have additional children */
export interface TrableRow<T> extends TableRow<T> {
    /** If the row is expanded/open. */
    expanded: boolean;
    /** The hierarchy level of this row in the trable. */
    level: number;
    /** If this row-item can be selected. */
    selectable: boolean;
    /** If this element can/has have children. */
    hasChildren: boolean;
    /** If this row has already loaded the children. */
    loaded: boolean;
    /** If this row is currently loading (it's children). */
    loading?: boolean;
    /** The children of this element/row */
    children?: TrableRow<T>[];
    /** The rows parent. */
    parent?: TrableRow<T>;
}

export interface TrableRowExpandEvent<T> {
    row: TrableRow<T>;
    expanded: boolean;
}

/**
 * A single action that can be performed in a table.
 * It can be triggered for a single-item, multi-item, or both.
 */
export interface TableAction<T> {
    /** ID of the action. */
    id: string;
    /** Label of the action which will be displayed as title. */
    label: string;
    /** The Icon that will be displayed. */
    icon: string;
    /** Specifies if the icon is filled. */
    iconHollow?: boolean;
    /** The type of button styling to use. */
    type?: ColorThemes;
    /** If this action can be used for single items */
    single?: boolean;
    /** If this action can be used for multiple items */
    multiple?: boolean;
    /** If the item is allowed for this action. */
    enabled: boolean | ((item?: T) => boolean);
}

/**
 * The event which is triggered when an action is being clicked on.
 */
export interface TableActionClickEvent<T> {
    /** The id of the action that has been clicked. */
    actionId: string;
    /** If the table selection is affected from this action or only the provided item. */
    selection: boolean;
    /** When `selection` is false, then this contains the item the action has been clicked on. */
    item?: T;
}
