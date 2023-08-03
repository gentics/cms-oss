export interface PagingMetaInfo {
    /** Number of the current page. */
    currentPage: number;
    /** Number of the pages which can be found for the given per page count. */
    pageCount: number;
    /** Number of elements which can be included in a single page. */
    perPage: number;
    /** Number of all elements which could be found. */
    totalCount: number;
}

export interface PagingOptions {
    perPage?: number;
    page?: number;
}

export interface SortingOptions {
    /**
     * Field name to sort the result by.
     */
    sortBy?: string;
    order?: 'UNSORTED' | 'ASC' | 'DESC';
}

export interface BasicListOptions extends PagingOptions, SortingOptions {
    /**
     * Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted.
     */
    branch?: string;
}

export interface PermissionInfo {
    /** Flag which indicates whether the create permission is granted. */
    create: boolean;
    /** Flag which indicates whether the delete permission is granted. */
    delete: boolean;
    /** Flag which indicates whether the publish permission is granted. */
    publish?: boolean;
    /** Flag which indicates whether the read permission is granted. */
    read: boolean;
    /** Flag which indicates whether the read published permission is granted. */
    readPublished?: boolean;
    /** Flag which indicates whether the update permission is granted. */
    update: boolean;
}

/**
 * New node reference of the user. This can also explicitly set to null in order to
 * remove the assigned node from the user
 */
export interface ExpandableNode {
    uuid?: string;
}

export interface ElasticSearchSettings {
    [key: string]: any;
}

export interface GenericMessageResponse {
    /** Internal developer friendly message */
    internalMessage: string;
    /**
     * Enduser friendly translated message. Translation depends on the 'Accept-Language'
     * header value
     */
    message: string;
    /** Map of i18n properties which were used to construct the provided message */
    properties?: { [key: string]: any };
}
