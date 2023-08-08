import { UserReference } from './users';

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
    order?: '' | 'asc' | 'desc';
}

export interface BasicListOptions extends PagingOptions, SortingOptions {
    /**
     * Specifies the branch to be used for loading data. The latest project branch will be used if this parameter is omitted.
     */
    branch?: string;
}

export interface PermissionListOptions {
    /**
     * The role query parameter take a UUID of a role and may be used to add permission information to the response
     * via the rolePerm property which lists the permissions for the specified role on the element.
     * This may be useful when you are logged in as admin but you want to retrieve the editor role permissions on a given node.
     */
    role?: string;
}

export enum Permission {
    CREATE = 'create',
    DELETE = 'delete',
    PUBLISH = 'publish',
    READ = 'read',
    READ_PUBLISHED = 'readPublished',
    UPDATE = 'update',
}

export interface PermissionInfo {
    /** Flag which indicates whether the create permission is granted. */
    [Permission.CREATE]: boolean;
    /** Flag which indicates whether the read permission is granted. */
    [Permission.READ]: boolean;
    /** Flag which indicates whether the update permission is granted. */
    [Permission.UPDATE]: boolean;
    /** Flag which indicates whether the delete permission is granted. */
    [Permission.DELETE]: boolean;
    /** Flag which indicates whether the publish permission is granted. */
    [Permission.PUBLISH]?: boolean;
    /** Flag which indicates whether the read published permission is granted. */
    [Permission.READ_PUBLISHED]?: boolean;
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

export interface Entity {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: UserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: UserReference;
    /** Uuid of the element */
    uuid: string;
}

