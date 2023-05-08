import { ListType, SelectType } from './tag-part-types';

/**
 * This file contains various types used as values in TagProperties.
 */

/**
 * SelectOption object representing a datasource value in GCN
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_SelectOption.html
 */
export interface SelectOption {

    /** The ID of this option within the DataSource. */
    id: number | string;

    /** The string that should be displayed for this option to the user. */
    key: string;

    /** The value that this option represents. */
    value: string | number;

}

/**
 * Possible order directions in TagProperties that support ordering.
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_OrderDirection.html
 */
export enum OrderDirection {

    /** Listed objects are sorted in ascending order. */
    ASC = 'ASC',

    /** Listed objects are sorted in descending order. */
    DESC = 'DESC',

    /** Overview is not defined. */
    UNDEFINED = 'UNDEFINED',

}

/**
 * Enumeration for the 'order by' setting of TagProperties that support ordering
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_OrderBy.html
 */
export enum OrderBy {

    /** Listed objects are sorted by name */
    ALPHABETICALLY = 'ALPHABETICALLY',

    /** Listed objects are sorted by priority (only if listType is PAGE) */
    PRIORITY = 'PRIORITY',

    /** Listed objects are sorted by publish date (only if listType is PAGE) */
    PDATE = 'PDATE',

    /** Listed objects are sorted by edit date */
    EDATE = 'EDATE',

    /** Listed objects are sorted by creation date */
    CDATE = 'CDATE',

    /** Listed objects are sorted by file size (only if listType is FILE or IMAGE) */
    FILESIZE = 'FILESIZE',

    /** Listed objects are sorted manually */
    SELF = 'SELF',

    /** Overview is not defined */
    UNDEFINED = 'UNDEFINED',
}

/**
 * Object containing nodeId and objectId of an object selected for an overview.
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_NodeIdObjectId.html
 */
export interface NodeIdObjectId {
    nodeId: number;
    objectId: number;
}

/**
 * Represents the values of an OverviewTagProperty.
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Overview.html
 */
export interface Overview {

    /** The chosen ListType */
    listType: ListType;

    /** The chosen SelectionType */
    selectType: SelectType;

    /** Order Direction */
    orderDirection: OrderDirection;

    /** Order By */
    orderBy: OrderBy;

    /**
     * List of selected item IDs.
     * If OverviewSetting.stickyChannel is false, this property needs to be set.
     */
    selectedItemIds?: number[];

    /**
     * List of selected item IDs with node IDs.
     * If OverviewSetting.stickyChannel is true, this property needs to be set.
     */
    selectedNodeItemIds?: NodeIdObjectId[];

    /** Overview Source */
    source: string;

    /** Maximum number of items */
    maxItems: number;

    /** True when objects shall be fetched also from subfolders (if selectType is FOLDER) */
    recursive: boolean;

}
