import { Observable } from 'rxjs';

/* BASIC TYPES ********************************************************************************************* */

/** Allowed data types */
export type GtxQueryAssemblerDataType =
    'boolean' |
    'number' |
    'string' |
    'date' |
    'objectid';

export type GtxChipSearchPropertyBooleanOperators = 'IS' | 'IS_NOT';
export interface GtxChipSearchPropertyBoolean {
    value: boolean;
    operator: GtxChipSearchPropertyBooleanOperators;
}

export type GtxChipSearchPropertyNumberOperators = 'IS' | 'IS_NOT';
export interface GtxChipSearchPropertyNumber {
    value: number | 'all';
    operator: GtxChipSearchPropertyNumberOperators;
}

export type GtxChipSearchPropertyStringOperators = 'CONTAINS' | 'CONTAINS_NOT' | 'IS' | 'IS_NOT';
export interface GtxChipSearchPropertyString {
    value: string;
    operator: GtxChipSearchPropertyStringOperators;
}

export type GtxChipSearchPropertyDateOperators = 'AT' | 'AFTER' | 'BEFORE' | 'BETWEEN';
export interface GtxChipSearchPropertyDate {
    value: number;
    operator: GtxChipSearchPropertyDateOperators;
}

export type GtxChipSearchPropertyObjectIdOperators = 'IS' | 'IS_NOT';
export interface GtxChipSearchPropertyObjectId {
    value: number | string;
    operator: GtxChipSearchPropertyObjectIdOperators;
}

export type GtxChipSearchProperty =
    GtxChipSearchPropertyBoolean |
    GtxChipSearchPropertyNumber |
    GtxChipSearchPropertyString |
    GtxChipSearchPropertyDate |
    GtxChipSearchPropertyObjectId;


/* STATE TYPES ********************************************************************************************* */

export type GtxChipSearchProperties = {
    [key: string]: GtxChipSearchProperty[];
}

export type GtxChipSearchPropertyKeys = string;

export type GtxChipSearchPropertyArrayElement<ArrayType extends GtxChipSearchProperty[]> = ArrayType[number];

export type GtxChipSearchPropertyInterface<K extends GtxChipSearchPropertyKeys>
    = GtxChipSearchPropertyArrayElement<GtxChipSearchProperties[K]>;

export type GtxChipSearchSearchFilterMap = {
    [P in keyof GtxChipSearchProperties]?: GtxChipSearchProperties[P] | null;
};


/* PRESENTATIONAL TYPES ********************************************************************************************* */

export interface GtxChipInputSelectOption {
    value: number | string;
    label: string;
}

/**
 * Entity property operator
 * @sample
 * `CONTAINS` | `CONTAINS_NOT` | `IS` | `IS_NOT`
 */
export type GtxChipOperator<K extends GtxChipSearchPropertyKeys> = GtxChipSearchPropertyInterface<K>['operator'];

/** Search-chip property value of defined data types. */
export type GtxChipValue<K extends GtxChipSearchPropertyKeys> = GtxChipSearchPropertyInterface<K>['value'];

/** Search-chip property option, defining search-chip form behavior */
export interface GtxChipSearchChipPropertyOption {
    /** user-faced string */
    label: string;
    /** The property filterable by Elastic Search. */
    value: GtxChipSearchPropertyKeys;
    /** The type of the value to be selectectable by the user. */
    type: GtxQueryAssemblerDataType;
    /** @Optional: define subset of generic options. */
    context?: GtxChipSearchChipPropertyOptionContext;
}

/** Context provided for user-input configuration. */
export interface GtxChipSearchChipPropertyOptionContext {
    /**
     * Provides array of key-label-pairs defining available options of operator dropdown menus.
     * @sample
     * ```
     * {
     *     relationOperators: [
     *         // order matters:
     *         // first element will be pre-selected by default
     *         {
     *             value: 'IS',
     *             label: 'search.is',
     *         },
     *         {
     *             value: 'CONTAINS',
     *             label: 'search.contains',
     *         },
     *         {
     *             value: 'CONTAINS_NOT',
     *             label: 'search.contains_not',
     *         },
     *     ],
     * }
     *
     * {
     *     relationOperators: [
     *         // if there is only one element it will be selected
     *         // and input be disabled
     *         {
     *             value: 'IS',
     *             label: 'search.is',
     *         },
     *     ],
     * }
     * ```
     */
    relationOperators?: GtxChipSearchChipOperatorOption<any>[];
    /** Data stream of key-label-pairs defining drop-down options of search-chips. */
    inputselectOptionsAsync?: Observable<GtxChipInputSelectOption[]>;
    /** Data stream of default (pre-selected) value of drop-down options of search-chips. */
    inputselectOptionsDefaultValueAsync?: Observable<string>;
}

/** Provides array of relation operators, e. g. `IS` or `IS_NOT`. */
export interface GtxChipSearchChipOperatorOption<K extends GtxChipSearchPropertyKeys> {
    /** user-faced string */
    label: string;
    /** The operator to be used by Elastic Search. */
    value: GtxChipOperator<K>;
}

/** Filter chip form data */
export interface GtxChipSearchChipData<K extends GtxChipSearchPropertyKeys> {
    /** Property to be filtered by Elastic Search */
    chipProperty: K;
    /** Property-value relation for the search query */
    chipOperator: GtxChipOperator<K>;
    /** User-defined value of search query */
    chipValue: GtxChipValue<K>;
}

/** Complete set of data contained in ChipSearch component controller. */
export interface GtxChipSearchData<K extends GtxChipSearchPropertyKeys> {
    /** Standalone text in search bar not defined as chip, filtering only, not querying Elastic Search */
    searchBar: string;
    /** Array of chips defining Elastic Search query */
    chips: GtxChipSearchChipData<K>[];
}

/**
 * ## GtxChipSearchComponent configuration.
 * Contains the query parts for representational components for the user to choose from.
 */
export interface GtxChipSearchConfig {
    searchableProperties: GtxChipSearchChipPropertyOption[];
    /** On ChipSearchBar.keyDown($event.key === 'Enter') this filters will be added to the `all` filter. */
    defaultFilters?: GtxChipSearchChipData<string>[];
}
