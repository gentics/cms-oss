import { BoolQuery } from 'elastic-types/queries';
import { DirtQueueItem, Jobs } from './admin-info';
import { CmsFormData, Form, FormStatus } from './cms-form';
import { ConstructCategory } from './construct-category';
import { ContentPackage } from './content-package';
import { ContentRepository, ContentRepositoryType, CRElasticsearchModel } from './content-repository';
import { ContentRepositoryFragment } from './cr-fragment';
import { DataSource } from './data-source';
import { DataSourceEntry } from './data-source-entry';
import { ElasticSearchIndex } from './elastic-search-index';
import { NodeFeatureModel } from './feature';
import { File } from './file';
import { Folder } from './folder';
import { EntityIdType } from './gcms-normalizer/gcms-normalizer-types';
import { Group } from './group';
import { Image } from './image';
import { ItemType } from './item';
import { Language } from './language';
import { Node } from './node';
import { ObjectProperty } from './object-property';
import { ObjectPropertyCategory } from './object-property-category';
import { Package } from './package';
import { Page, PageStatus } from './page';
import { AccessControlledType, PermissionInfo, PrivilegeFlagName } from './permissions';
import { RoleAssignment } from './permissions/cms/roles';
import { Role, RolePermissions } from './role';
import { Schedule } from './schedule';
import { ScheduleExecution } from './schedule-execution';
import { ScheduleTask } from './schedule-task';
import { Tag, TagType, TemplateTag } from './tag';
import { TagmapEntry } from './tagmap-entry';
import { Template } from './template';
import { Raw } from './type-util';
import { User } from './user';
import { DependencyType } from './package-check';

export interface ElasticSearchQuery {
    query: BoolQuery;
    from: number;
    size: number;
    _source: boolean;
}

export type CommonSortFields = 'name' | 'cdate' | 'edate' | 'customordefaultcdate' | 'customordefaultedate' | 'masterNode' | 'excluded';
export type FileSortFields = 'type' | 'filesize';
export type GroupSortFields = keyof Pick<Group, 'id' | 'name'>;
export type PageSortFields = 'pdate' | 'filename' | 'template' | 'folder' | 'priority' | 'deletedate';
export type WastebinSortFields = 'deletedat';
export type SortField = CommonSortFields | PageSortFields | FileSortFields | WastebinSortFields;

export type SortOrder = 'asc' | 'desc' | 'none';

/**
 * Options which may be passed as query parameters to the `node/id` endpoints
 */
export interface NodeRequestOptions {

    /** true when the item should be fetched for updating  */
    update?: boolean;

}

/**
 * Options which may be passed as query parameters to the `<type>/load/id` endpoints
 */
export interface ItemRequestOptions {

    /** ID of the node (channel) for which the item shall be loaded (when multichannelling is used). */
    nodeId?: number;

    /** true when the item should be fetched for updating  */
    update?: boolean;

    /* true if the TagTypes should be fetched for the tags */
    construct?: boolean;

}

/**
 * Options which may be passed as query parameters to the `folder/load/id` endpoints
 */
export interface FolderRequestOptions extends ItemRequestOptions {

    /** flag whether the privileges should be added to the reponse */
    privileges?: boolean;

    /** true if the privileges shall be added to the items as map, false if not */
    privilegeMap?: boolean;

}

/**
 * Options which may be passed as query parameters to the `folder/load/id` endpoints
 */
export interface FormRequestOptions extends ItemRequestOptions {

    /** flag whether the privileges should be added to the reponse */
    privileges?: boolean;

    /** true if the privileges shall be added to the items as map, false if not */
    privilegeMap?: boolean;

}

/**
 * Options which may be passed as query parameters to the `page/load/id` endpoints
 *
 * When using loading pages, it is important to note that the returned tag data may be loaded differently
 * depending on whether or not the page is loaded for editing ("update" flag is set to true).
 * Loading a page with the update flag set to false allows you to load the page without causing it to be locked for editing,
 * but it means that any tags which have not been filled will be loaded without all their tag parts.
 * Tags that have not been filled, and that do not have default values will be loaded with empty properties in this case.
 * On the other hand, when loading pages for editing, any tags that have no been filled, will have their constituents part auto-generated.
 * Consequently, when loading pages via the REST-API, setting the update flag to true will cause all tags to be loaded with all
 * parts data (auto-generated if necessary), whereas when loading a page with update set to false,
 * any tags which have not had their parts filled will be loaded without their parts data.
 */
export interface PageRequestOptions extends ItemRequestOptions {

    /** If true, disinherited channel nodes shall be added */
    disinherited?: boolean;

    /** true if the folder information shall be added */
    folder?: boolean;

    /** true if the language variants shall be added */
    langvars?: boolean;

    pagevars?: boolean;

    /** true if the template information shall be added */
    template?: boolean;

    /** true if the translationstatus information shall be added */
    translationstatus?: boolean;

    /** true if version information shall be added  */
    versioninfo?: boolean;

    /** true if the workflow information shall be added */
    workflow?: boolean;

}

/**
 * Options which may be passed as query parameters to the `file/load/id` endpoints
 */
export interface FileRequestOptions extends ItemRequestOptions { }

/**
 * Options which may be passed as query parameters to the `image/load/id` endpoints
 */
export interface ImageRequestOptions extends ItemRequestOptions { }

/**
 * Options which may be passed as query parameters to the `node` endpoints
 */
export interface NodeListRequestOptions extends BaseListOptionsWithPaging<Node> {
    perms?: boolean;
}

export type ConstructEmbedTypes = 'category';

/**
 * Options which may be passed as query parameters to the `node/id/constructs` endpoints
 */
export interface PagedConstructListRequestOptions extends BaseListOptionsWithPaging<Tag>, EmbedListOptions<ConstructEmbedTypes> {
    perms?: boolean;
}

/**
 * Options which may be passed as query parameters to the `node/features` and `node/id/features` endpoints
 */
export interface NodeFeatureListRequestOptions extends BaseListOptionsWithPaging<NodeFeatureModel> { }

export interface TemplateMultiLinkRequest {
    /** Ids of the templates that should be (un-)linked from the folders. */
    templateIds: (string | number)[];
    /** Ids of the folders that the templates should be (un-)linked from. */
    folderIds: (string | number)[];
    /** The node id. */
    nodeId: number;
    /** If to (un-)link the templates recursively. */
    recursive?: boolean;
    /** If templates should be deleted if it has been unlinked from the last folder. */
    delete?: boolean;
}

/**
 * Query parameters usable for pageable list endpoints that use a `skipCount` to implement paging.
 */
export interface BaseListOptionsWithSkipCount {

    /** maximum number of items to be returned, set to -1 for returning all items */
    maxItems?: number;

    /** number of items to be skipped, set to 0 for skipping no items */
    skipCount?: number;

    /** Sorting order, defaults to "asc" */
    sortorder?: SortOrder;

}

/** Used for defining the sort order in `BaseListOptionsWithPaging`. */
export enum PagingSortOrder {
    Asc = '+',
    Desc = '-',
    None = '',
}

/** Defines a sorting option in `BaseListOptionsWithPaging`. */
export interface PagingSortOption<T> {

    /** The order in which the attribute values should be sorted (default is `None`). */
    sortOrder?: PagingSortOrder;

    /** The attribute that should be sorted. */
    attribute: keyof T;

}

/**
 * Query parameters usable for pageable list endpoints that use a `page` and a `pageSize` to implement paging.
 *
 * **Important:** Client methods for endpoints that use this interface must stringify the `sort` property using
 * `ApiBase.stringifyPagingSortOptions()`.
 */
export interface BaseListOptionsWithPaging<T> {

    /** Returned page, if paging is used. Paging starts with 1. */
    page?: number;

    /**
     * Page size for paging. If this is set to -1 no paging is used (all matching items are returned).
     * Setting this to 0 will return no items.
     */
    pageSize?: number;

    /** Query string for filtering. */
    q?: string;

    /**
     * The attributes, on which sorting should be performed.
     *
     * **Important:** This property must be stringified using `ApiBase.stringifyPagingSortOptions()` before making the REST request.
     */
    sort?: PagingSortOption<T> | PagingSortOption<T>[] | string;

    /** true when the items shall be fetched recursively */
    recursive?: boolean;
}

export interface EmbedListOptions<T> {
    embed?: T | T[];
}

export interface IdSetRequest {
    ids: string[];
}

/**
 * Request object used to configure the behaviour of the `admin/actionlog` endpoint.
 */
export interface LogsListRequest {

    /** List of action names for filtering */
    action?: string;

    /** End timestamp for filtering */
    end?: number;

    /** Object ID for filtering */
    objId?: number;

    /** Returned page, if paging is used. Paging starts with 1 */
    page?: number;

    /** Page size for paging. If this is set to -1 no paging is used (all matching items are returned). Setting this to 0 will return no items */
    pageSize?: number;

    /** Start timestamp for filtering */
    start?: number;

    /** List of object type names for filtering */
    type?: string;

    /** Search string for filtering by user. The string may be contained in the firstname, lastname or login of the user */
    user?: string;
}

/**
 * Request object used to configure the behaviour of the `node/{nodeId}/languages` endpoint.
 */
export interface NodeLanguageListRequest {
    page?: number;
    pageSize?: number;
    /** Query string for filtering */
    q?: string;
}

/**
 * Query parameters for listing folder contents (folders, items)
 */
export interface FolderListOptions extends BaseListOptionsWithSkipCount {

    addPrivileges?: boolean;

    /** timestamp for restricting to items that were created before a given time, 0 for all items */
    createdbefore?: number;

    /** timestamp for restricting to items that were created since a given time, 0 for all items */
    createdsince?: number;

    /** Pattern for restricting items by creator */
    creator?: string;

    /** timestamp for restricting to items that were edited before a given time, 0 for all items */
    editedbefore?: number;

    /** timestamp for restricting to items that were edited since a given time, 0 for all items */
    editedsince?: number;

    /** Pattern for restricting items by editor */
    editor?: string;

    /** true if the folder information should be added to the pages */
    folder?: boolean;

    /**
     * true to only return inherited items in the given node,
     * false to only get local/localized items,
     * null to get local and inherited folders
     */
    inherited?: boolean;

    /** Code of the language in which the pages shall be fetched. */
    language?: string;

    /** true when the language variants should be added to the pages */
    langvars?: boolean;

    /** node id (for multichannelling) */
    nodeId?: number;

    /** true if the privileges shall be added to the items, false if not */
    privileges?: boolean;

    /** true if the privileges shall be added to the items as map, false if not */
    privilegeMap?: boolean;

    /** true when the items shall be fetched recursively */
    recursive?: boolean;

    /**
     * search string to search for in name, null if not searching
     * - this will filter the results if either the ID,
     * the name (partial match or the description (partial match) matches the given search string.
     */
    search?: string;

    /** The field to sort on. */
    sortby?: SortField;

    /**
     * true when folders shall be returned as tree(s).
     * Subfolders will be attached to their mothers.
     * This only makes sense, when recursive is true.
     */
    tree?: boolean;

    /**
     * exclude (default) to exclude deleted objects,
     * include to include deleted objects,
     * only to return only deleted objects.
     */
    wastebin?: 'exclude' | 'include' | 'only';

}

export interface FileListOptions extends FolderListOptions {
    /** The id of the folder the file have to reside in. */
    folderId?: number;
    /** true to only return broken files (where the binary data is missing), false to only get non-broken files, null to get both */
    broken?: boolean;
    /** optional regular expression to get files with a nice URL. */
    niceurl?: string;
    /** true to only return online files, false to only get offline files, null to get both online and offline files */
    online?: boolean;
}

/** Collection of all query parameters allowed by GCMS entities Folder, Form, File, Image, Item */
export type GtxCmsQueryOptions = FolderListOptions & PageListOptions & FolderListOptions & FormListOptions &
{
    /** path (e.g. /_search) */
    path?: string;
    /** true to add content tags to pages */
    contenttags?: boolean;
    /** true to add the folder to search hits */
    folder?: boolean;
    /** optional: folderId to restrict search to specific folder(s) */
    folderId?: number;
    /** true to add language variants to pages */
    langvars?: boolean;
    /** optional: nodeId to restrict search to a specific node / default = 0 */
    nodeId?: number;
    /** true to add object tags to search hits */
    objecttags?: boolean;
    /** true to add privilege maps to folders */
    privilegeMap?: boolean;
    /** true to add privilege information to folders */
    privileges?: boolean;
    /** flag for restricting search to given folderIds and their subfolders */
    recursive?: boolean;
    /** true to add the template to pages */
    template?: boolean;
    /** true to add the translation status to pages */
    translationstatus?: boolean;
    /** code of the language in which the pages shall be fetched. */
    language?: string | string[];
    /** timestamp to search pages, which were created before a given time (0 for all pages) */
    ceatedbefore?: number;
    /** timestamp to search pages, which were created since a given time (0 for all pages) */
    createdsince?: number;
    /** pattern for restricting pages by creator */
    creator?: number;
    /** timestamp to search pages, which were edited before a given time (0 for all pages) */
    editedbefore?: number;
    /** timestamp to search pages, which were edited since a given time (0 for all pages) */
    editedsince?: number;
    /**
     * (optional) search string for filenames (may be empty)
     *
     * @Note
     * compared to advanced search there are no "is not" option ... on "contains" wildcards need to be set as "%"
     */
    filename?: string;
    /** true to restrict to modified pages, false to restrict to unmodified pages */
    modified?: boolean;
    /** regular expression to get pages with a nice URL. */
    niceurl?: string;
    /** true to restrict to online pages, false to restrict to offline pages */
    online?: boolean;
    /** true to restrict to planned pages, false to restrict to unplanned pages */
    planned?: boolean;
    /** priority of the page */
    priority?: number;
    /** timestamp to search pages, which were published before a given time (0 for all pages) */
    publishedbefore?: number;
    /** timestamp to search pages, which were published since a given time (0 for all pages) */
    publishedsince?: number;
    /** pattern for restricting pages by publisher */
    publisher?: number;
    /** pattern for restricting pages by editor */
    editor?: number;
    /** true to restrict to queued pages, false to restrict to unqueued pages */
    queued?: boolean;
    /** true, if also the content shall be searched, false if not */
    searchcontent?: boolean;
    /** list of template ids */
    template_id?: number;
    /** exclude (default) to exclude deleted objects, include to include deleted objects, only to return only deleted objects */
    wastebin?: 'exclude' | 'include' | 'only';
    /** true to only return inherited folders in the given node, false to only get local/localized folders, null to get local and inherited folders */
    inherited?: boolean;
    /** true to only return broken files (where the binary data is missing), false to only get non-broken files, null to get both */
    broken?: boolean;
    /**
     * true if only used files that are referenced somewhere shall be fetched,
     * false for unused files. If "usedIn" is not specified, this filter will only check in the current channel.
     */
    used?: boolean;
    /** list of channel IDs for extending the "used" filter to multiple channels. */
    usedIn?: number[];

    /**
     * convention "legacy":
     * search string to search for in name, null if not searching
     * - this will filter the results if either the ID,
     * the name (partial match or the description (partial match) matches the given search string.
     */
    search?: string;
    /**
     * convention "new":
     * search string to search for in name, null if not searching
     * - this will filter the results if either the ID,
     * the name (partial match or the description (partial match) matches the given search string.
     */
    q?: string;

    /** maximum number of items to be returned, set to -1 for returning all items */
    maxItems?: number;
    /** Only acknowledged by `form`. */
    pageSize?: number;
    // -- sorting parameter convention "legacy" --
    /** get results using from as offset */
    from?: number;
    /** The field to sort on. */
    sortby?: SortField;
    // -- sorting parameter convention "new" --
    /** number of items to be skipped, set to 0 for skipping no items */
    skipCount?: number;
    /** Sorting order, defaults to "asc" */
    sortorder?: SortOrder;

}

/**
 * Query parameters for listing the pages in a folder
 */
export interface PageListOptions extends FolderListOptions {

    /** true if the contenttags shall be attached to all returned pages. Default is false */
    contenttags?: boolean;

    status?: PageStatus;

    /**
     * List of folder permissions which must be granted
     * for folders in order to include their pages in the result
     */
    permission?: string;

    /** true when the template information should be added to the pages */
    template?: boolean;
    /** (optional) search string for filenames (may be empty) */
    filename?: string;
    /** List of markup language IDs for restricting to pages that have templates with none of the given markup languages */
    excludeMlId?: number[];
    /** List of markup language IDs for restricting to pages that have templates with one of the given markup languages */
    includeMlId?: number[];
    /** true to restrict to modified pages, false to restrict to unmodified pages */
    modified?: boolean;
    /** regular expression to get pages with a nice URL. */
    niceurl?: string;
    /** true to restrict to online pages, false to restrict to offline pages */
    online?: boolean;
    /** true to restrict to planned pages, false to restrict to unplanned pages */
    planned?: boolean;
    /** priority of the page */
    priority?: number;
    /** timestamp to search pages, which were published before a given time (0 for all pages) */
    publishedbefore?: number;
    /** timestamp to search pages, which were published since a given time (0 for all pages) */
    publishedsince?: number;
    /** true to restrict to queued pages, false to restrict to unqueued pages */
    queued?: boolean;
    /** true, if also the content shall be searched, false if not */
    searchcontent?: boolean;
    /** list of template ids */
    template_id?: number;
    /** exclude (default) to exclude deleted objects, include to include deleted objects, only to return only deleted objects */
    wastebin?: 'exclude' | 'include' | 'only';

}

/**
 * Query parameters for listing the forms in a folder
 */
export interface FormListOptions extends BaseListOptionsWithPaging<Form> {

    /** ID of folder form will be created in. */
    folderId: number;

    status?: FormStatus;

    /**
     * List of folder permissions which must be granted
     * for folders in order to include their forms in the result
     */
    permission?: string;

    /** Returned page, if paging is used. Paging starts with 1 */
    page?: number;

    /** timestamp for restricting to items that were created before a given time, 0 for all items */
    createdbefore?: number;
    /** timestamp for restricting to items that were created since a given time, 0 for all items */
    createdsince?: number;
    /** timestamp for restricting to items that were edited before a given time, 0 for all items */
    editedbefore?: number;
    /** timestamp for restricting to items that were edited since a given time, 0 for all items */
    editedsince?: number;
    /** timestamp to search pages, which were published before a given time (0 for all pages) */
    publishedbefore?: number;
    /** timestamp to search pages, which were published since a given time (0 for all pages) */
    publishedsince?: number;

    /** true to restrict to modified objects, false to restrict to unmodified objects */
    modified?: boolean;
    /** true to restrict to online objects, false to restrict to offline objects */
    online?: boolean;

}

export interface LinkCheckerOptions extends BaseListOptionsWithPaging<Page> {
    editable?: boolean;
    iscreator?: boolean;
    iseditor?: boolean;
    nodeId?: number;
    status?: string;
}

export interface DirtQueueListOptions extends BaseListOptionsWithPaging<DirtQueueItem> {
    /** If the response should only include failed tasks. */
    failed?: boolean;
    /** Timestamp from which time on the tasks should be listed. */
    start?: number;
    /** Timestamp util when the tasks should be listed. */
    end?: number;
}

export interface ElasticSearchIndexListOptions extends BaseListOptionsWithPaging<ElasticSearchIndex> {}

export interface JobListRequestOptions extends BaseListOptionsWithPaging<Jobs> {
    /**
     * If set to true, only active jobs will be returned.
     * If set to false, only inactive jobs will be returned. If not set (default), all jobs will be returned.
     */
    failed?: boolean;

    /**
     * If set to true, only failed jobs will be returned.
     * If set to false, only successful jobs will be returned (include jobs that were not yet executed).
     * If not set (default), all jobs will be returned.
     */
    active?: boolean;
}


export interface PublishQueueOptions {
    skipCount?: number;
    maxItems?: number;
    search?: string;
    sortBy?: CommonSortFields | PageSortFields;
    sortOrder?: 'asc' | 'desc';
}

export type UserEmbedTypes = 'group';

export interface UserListOptions extends BaseListOptionsWithPaging<User>, EmbedListOptions<UserEmbedTypes> {
    perms?: boolean;
}

/**
 * Request object used to configure the behaviour of the
 * `folder/getFolders` and `folder/getItems` endpoint.
 */
export interface FolderListRequest extends Omit<FolderListOptions, 'sortorder'> {

    /** The ID of folder, for which to get the items. */
    id?: number;

    /** The type of items that should be fetched. */
    type?: (ItemType | 'template') | (ItemType | 'template')[];

    createdbefore?: number;
    createdsince?: number;
    editedbefore?: number;
    editedsince?: number;
    maxItems?: number;
    recursive?: boolean;
    recursiveIds?: any[];
    skipCount?: number;
    sortby?: CommonSortFields;
    sortorder?: SortOrder;
    tree?: boolean;
    wastebin?: 'exclude' | 'include' | 'only';
}

export interface PageListRequest extends FolderListRequest {
    status: PageStatus;
    permission: string;
}

export interface FormListRequest extends FolderListRequest {
    folderId: number;
}

export interface FormReportsRequest {
    page?: number;
    pageSize?: number;
    /** Query string for filtering */
    q?: string;
}

export interface FormExportRequest {
    /** Export format (CSV) */
    format?: string;
    /** Options for the export format */
    options?: string;
}

/**
 * Request object used to configure the behaviour of the `folder/getTemplates` endpoint.
 */
export interface TemplateFolderListRequest extends FolderListOptions {
    checkPermission?: boolean;
    reduce?: boolean;
}

export interface TemplateListRequest extends BaseListOptionsWithPaging<Template> {
    nodeId?: number | number[];
    perms?: boolean;
    reduce?: boolean;
}

export interface NodeMultiLinkRequest {
    nodeIds: number[];
    ids: string[];
}

// LOGS /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Request object used to configure the behaviour of the `admin/actionlog/types` endpoint.
 */
export interface LogTypesRequest {
    page?: number;

    pageSize?: number;

    /** Query string for filtering */
    q?: string;

    sort?: CommonSortFields;
}

/**
 * Request object used to configure the behaviour of the `admin/actionlog/actions` endpoint.
 */
export interface LogActionsRequest {
    page?: number;

    pageSize?: number;

    /** Query string for filtering */
    q?: string;

    sort?: CommonSortFields;
}

// ADMIN /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * REST Model of a content maintenance action request for endpoint `POST admin/content/publishqueue`
 */
export interface ContentMaintenanceActionRequest {
    /** Maintenance action */
    action: ContentMaintenanceAction;
    /** ContentMaintenanceType Object types for restricting the action */
    types?: ContentMaintenanceType[];
    /** True to clear the publish cache for dirted objects */
    clearPublishCache?: boolean;
    /** Array of string Attributes for restricted dirting (only the dirted attributes will be updated) */
    attributes?: string[];
    /** Node IDs for node restriction */
    nodes?: number[];
    /** ContentRepository IDs for restriction */
    contentRepositories?: number[];
    /** Start timestamp for cdate restriction */
    start?: number;
    /** End timestamp for cdate restriction */
    end?: number;
}

/**
 * Maintenance actions
 */
export enum ContentMaintenanceAction {
    /** Mark objects for (re-)publishing. */
    dirt = 'dirt',
    /** Delay dirted objects. */
    delay = 'delay',
    /** Republish delayed objects. */
    publishDelayed = 'publishDelayed',
    /** Mark dirted objects as "published" */
    markPublished = 'markPublished',
}

/**
 * Types of objects, which can be handled with maintenance actions
 */
export enum ContentMaintenanceType {
    file = 'file',
    folder = 'folder',
    page = 'page',
    form = 'form',
}

// ENTITIES /////////////////////////////////////////////////////////////////////////////////////////////////////


/** * Request object used to configure the behaviour of the
 * `folder/create` endpoint.
 */
export interface FolderCreateRequest {
    nodeId: number;
    motherId: number;
    publishDir: string;
    description: string;
    name: string;
}

/**
 * Request object used to configure the behaviour of the
 * `/language` endpoint.
 */
export interface LanguageCreateRequest {
    name: string;
    code: string;
}

export interface LanguageListOptions extends BaseListOptionsWithPaging<Language> {}

/**
 * Request object used to configure the behaviour of the
 * `/i18n/set` endpoint.
 */
export interface I18nLanguageSetRequest {
    code: string;
}

/**
 * Request object used to configure the behaviour of the
 * `page/create` endpoint.
 */
export interface PageCreateRequest {
    nodeId: number;
    folderId: number;
    pageName: string;
    fileName: string;
    description: string;
    language: string;
    priority: number;
    templateId: number;
}

/**
 * Request object used to configure the behaviour of the
 * `POST /form` endpoint.
 */
export interface FormCreateRequest {
    folderId: number;
    name: string;
    description?: string;
    successPageId?: number;
    successNodeId?: number;
    languages: string[];
    data: CmsFormData;
}

/**
 * Request object used to configure the behaviour of the
 * `POST /file/create` endpoint, requesting CMS to fetch binary response fom `sourceURL`.
 */
export interface FileCreateRequest {
    overwriteExisting: boolean,
    folderId: number,
    nodeId: number,
    name: string,
    description: string,
    sourceURL: string,
    niceURL?: string;
    alternateURLs?: string[];
    properties?: { [key: string]: any };
}

/**
 * Request object used to configure the behaviour of the
 * `file/copy` endpoint.
 */
export interface FileCopyRequest {
    /** Get the new filename for the copied file being created. */
    newFilename: string;
    /** File object to be copied; id only is sufficient. */
    file: {
        id: number;
    };
    /** Get the node ID the file should be copied to. */
    nodeId: number;
    /** Get the target folder the file should be copied to. */
    targetFolder: TargetFolder;
}

/**
 * Target folder object for a page copy call. A folder is identified by
 * its id and the channelId. You can omit the channelId when you want
 * to copy to master folders.
 */
export interface TargetFolder {
    /** Get the target folder Id. */
    id: number;
    /** Get the target folder channel id. */
    channelId?: number;
}

/**
 * Request object used to configure the behaviour of the
 * `page/copy` endpoint.
 */
export interface PageCopyRequest {
    /** Whether new copies should be created in folders in which already pages with the same name reside. */
    createCopy: boolean;

    /** Node ID for the source pages. If this is set to a channel, the channel variant of the given page will be copied. */
    nodeId: number;

    /** The list of source page id's that should be copied to the target folders. */
    sourcePageIds: number[];

    /** The list of target folder in which the pages should be copied. */
    targetFolders: { id: number, channelId: number }[];
}

/**
 * Request object used to configure the behaviour of the
 * `form/copy` endpoint.
 */
export type FormCopyRequest = PageCopyRequest;

/**
 * Object which is expected by an `<item>/disinherit/<id>` endpoint to change
 * the inheritance settings of an item.
 */
export interface InheritanceRequest {
    disinherit: number[];
    reinherit: number[];
    exclude?: boolean;
    disinheritDefault?: boolean;
    recursive?: boolean;
}

/**
 * Object which is expected by an `<item>/push2master/<id>` endpoint to sync the
 * localized item with the master node.
 */
export interface ChannelSyncRequest {
    masterId: number;
    channelId: number;
    recursive?: boolean;
    types?: string[];
    ids?: number[];
}

export interface SyncObjectsRequest {
    dstNodeId: number;
    ids: number;
    srcNodeId: number;
    recursive?: boolean;
    types?: string[];
}

/**
 * Request to unlocalize an object via the `<itemType>/unlocalize/<id>` endpoint.
 */
export interface UnlocalizeRequest {
    /** Id of the channel of the localized version */
    channelId: number;

    /** True if unlocalizing should be done recursively, false if not */
    recursive: boolean;

    /** Number of seconds the job may run in the foreground */
    foregroundTime?: number;

    /**
     * List of types to be unlocalized (if the unlocalized object is a folder).
     * If not given, no subobjects will be unlocalized
     */
    types?: ('folder' | 'page' | 'image' | 'file' | 'template')[];
}

/**
 * Request to unlocalize multiple objects via the `<itemType>/unlocalize` endpoint.
 */
export interface MultiUnlocalizeRequest extends UnlocalizeRequest {
    // The IDs of the items to unlocalize.
    ids: number[];
}

/**
 * This object is derived from the gcnImagePlugin when using the crop and resize. It is used in making a request to the
 * `image/resize` endpoint to do a crop/resize on an image.
 */
export interface CropResizeParameters {
    image: {
        id: number;
    };
    cropHeight: number;
    cropWidth: number;
    cropStartX: number;
    cropStartY: number;
    width: number;
    height: number;
    mode: string;
    resizeMode: string;
    targetFormat: 'png' | 'jpg';
    copyFile: boolean;
    fpX?: number;
    fpY?: number;
}

/**
 *  This object is used in making a request to the`image/rotate` endpoint to perform a rotation on an image.
 */
export interface RotateParameters {
    image: {
        id: number;
    };
    targetFormat: 'png' | 'jpg';
    copyFile: boolean;
    rotate: 'cw' | 'ccw';
}

/**
 * Options to be passed to `page/search`
 */
export interface SearchPagesOptions {
    update?: boolean;
    template?: boolean;
    folder?: boolean;
    langvars?: boolean;
    pagevars?: boolean;
    workflow?: boolean;
    translationstatus?: boolean;
    versioninfo?: boolean;
    disinherited?: boolean;
    nodeId?: number;
    liveUrl?: string;
}

/**
 * Options for `msg/send`
 */
export interface SendMessageRequest {
    /** Message to be sent. May be a Content.Node i18n key */
    message: string;

    /** Translated messages */
    translations?: { [languageCode: string]: string };

    /** Optional: list of parameters which are filled into the message if the message is an i18n key */
    parameters?: string[];

    /** List of users who should receive the message */
    toUserId: number[];

    /** List of groups who should receive the message */
    toGroupId: number[];

    type: 'INFO';
}

/**
 * Options for saving a `File`.
 */
export interface FileSaveRequestOptions {

    /**
     * True if saving the file with a duplicate filename will fail.
     * If false (default) the filename will be made unique before saving
     */
    failOnDuplicate?: boolean;

}

/**
 * Request used for saving a `File`.
 */
export interface FileSaveRequest extends FileSaveRequestOptions {

    /** The properties of the file that should be saved/updated. */
    file: Partial<File<Raw>>;

}

/**
 * Options for saving/updating a `Folder`.
 */
export interface FolderSaveRequestOptions {

    /** Node ID, when setting the publish directory recursively. */
    nodeId?: number;

    /**
     * List of object tags, that shall be passed on to subfolders.
     * May contain names (with or without the prefix 'object.') or IDs
     */
    tagsToSubfolders?: string[];

    /** True to set the publish directory recursively, false (default) to only change this given folder. */
    recursive?: boolean;

    /**
     * True if saving the folder with a duplicate name will fail.
     * If false (default) the name will be made unique before saving.
     */
    failOnDuplicate?: boolean;

    /** Number of seconds, the job may run in the foreground. */
    foregroundTime?: number;

}

/**
 * Request used for saving/updating a `Folder`.
 */
export interface FolderSaveRequest extends FolderSaveRequestOptions {
    /** The properties of the folder that should be saved/updated. */
    folder: Partial<Folder<Raw>>;
}

/**
 * Request used for saving/updating a `Language`.
 */
export type LanguageUpdateRequest = Omit<Language, 'id'>;

/**
 * Options for saving an `Image`.
 */
export interface ImageSaveRequestOptions { }

/**
 * Request used for saving an `Image`.
 */
export interface ImageSaveRequest extends ImageSaveRequestOptions {

    /** The properties of the image that should be saved/updated. */
    image: Partial<Image<Raw>>;

}

/**
 * Options for saving a `Node`.
 */
export interface NodeSaveRequestOptions {

    /** The description of the node. */
    description?: string;

}

/**
 * Request used for saving a `Node`.
 */
export interface NodeSaveRequest extends NodeSaveRequestOptions {

    /** The properties of the node that should be saved/updated. */
    node: Partial<Node<Raw>>;

}

/**
 * Request used for creating a `Node`.
 */
export interface NodeCreateRequest extends NodeSaveRequest {

    node: Partial<Node<Raw>> & {
        /** The ID of the root folder of the parent of the new node. */
        masterId?: number;
    };

}

/**
 * Request for copying a `Node`.
 */
export interface NodeCopyRequest {

    /* `true`, if pages should be copied. */
    pages: boolean;

    /* `true`, if templates should be copied. */
    templates: boolean;

    /* `true`, if files should be copied. */
    files: boolean;

    /* `true`, if workflows should be copied. */
    workflows: boolean;

    /* The number of copies to create. */
    copies: number;

}

/** Additional options for a `NodeCopyRequest`. */
export interface NodeCopyRequestOptions {

    /** Wait timeout in milliseconds. */
    wait?: number;

}

/** Additional options for a `DELETE /node/{id}`. */
export interface NodeDeleteRequestOptions {

    /** Wait timeout in milliseconds. */
    wait?: number;

}

/**
 * Options for saving a `Page`.
 *
 * If not set otherwise, a new version will be created (if necessary) and the page will remain locked for the user.
 */
export interface PageSaveRequestOptions {

    /**
     * True if the page shall be unlocked after saving, false if not.
     * The default is false.
     */
    unlock?: boolean;

    /**
     * True if a page version shall be created, false if not.
     * The default is true.
     */
    createVersion?: boolean;

    /** List of tag names of tags, that shall be deleted. */
    delete?: string[];

    /**
     * True if saving the page with a duplicate name will fail.
     * If false (default) the name will be made unique before saving
     */
    failOnDuplicate?: boolean;

    /**
     * Indicates whether the filename should be derived from the page name, when no filename is given in the request.
     *
     * By default, the filename will not be derived from the page name. When the filename is provided in the request, this flag is ignored.
     */
    deriveFileName?: boolean;

}

/**
 * Request used for saving a `Page`.
 *
 * If not set otherwise, a new version will be created (if necessary) and the page will remain locked for the user.
 */
export interface PageSaveRequest extends PageSaveRequestOptions {

    /** The properties of the page that should be saved/updated. */
    page: Partial<Page<Raw>>;

}

/**
 * Options for saving a `Form`.
 *
 * If not set otherwise, a new version will be created (if necessary) and the page will remain locked for the user.
 */
export interface FormSaveRequestOptions {

    /**
     * True if the page shall be unlocked after saving, false if not.
     * The default is false.
     */
    unlock?: boolean;

    /** List of tag names of tags, that shall be deleted. */
    delete?: string[];

    /**
     * True if saving the page with a duplicate name will fail.
     * If false (default) the name will be made unique before saving
     */
    failOnDuplicate?: boolean;
}

/**
 * Request used for saving a `Form`.
 *
 * If not set otherwise, a new version will be created (if necessary) and the form will remain locked for the user.
 */
export type FormSaveRequest = Partial<Omit<Form,
'creator' |
'deleted' |
'edate' |
'editor' |
'folder' |
'folderDeleted' |
'locked' |
'lockedBy' |
'lockedSince' |
'master' |
'masterDeleted' |
'modified' |
'online' |
'published' |
'publisher' |
'usage' |
'versions'
>>;

/** Used to map item types to their save request options for use in generic methods. */
export interface FolderItemSaveOptionsMap {
    file: FileSaveRequestOptions;
    folder: FolderSaveRequestOptions;
    form: FormSaveRequestOptions;
    image: ImageSaveRequestOptions;
    page: PageSaveRequestOptions;
    template: never;
}

/** Used to map item types to their save requests for use in generic methods. */
export interface FolderItemOrNodeSaveOptionsMap extends FolderItemSaveOptionsMap {
    node: NodeSaveRequestOptions;
    channel: NodeSaveRequestOptions;
}

/**
 * Request to sanitize a folder publish directory
 */
export interface FolderPublishDirSanitizeRequest {
    nodeId: number;
    publishDir: string;
}

/**
 * Request object for suggesting a filename
 */
export interface SuggestPageFileNameRequest {
    folderId: number;
    nodeId: number;
    templateId: number;
    language: string;
    pageName: string;
    fileName: string;
}

/**
 * Options which may be passed as query parameters to the `<type>/user/id` endpoints
 */
export interface UserRequestOptions {
    /** If true, response will contain all groups of users */
    groups?: boolean;
}

/**
 * Options which may be passed as query parameters to the `<type>/user/id/groups` endpoints
 */
export interface UserGroupsRequestOptions extends BaseListOptionsWithPaging<Group> {
    id?: number;
    perms?: boolean;
}

/**
 * Request used for saving/updating a `User`.
 */
export type UserUpdateRequest = User<Raw>;

/**
 * Request object used to create a new group using the `group/{id}/groups` endpoint.
 *
 * Groups can only be created as subgroups. There is always one root group existing in the CMS instance.
 */
export type GroupCreateRequest = Omit<Group<Raw>, 'id' | 'children'>;

/**
 * Request used for saving/updating a `Group`.
 */
export type GroupUpdateRequest = Omit<Group<Raw>, 'id' | 'children'>;

/** * Request object used to configure the behaviour of the
 * `group/id/users` endpoint.
 *
 * Users can only be created within a group.
 */
export interface GroupUserCreateRequest extends Omit<User<Raw>, 'id'> {
    password: string;
}

/**
 * Query parameters for `/group/list`
 */
export interface GroupTreeOptions extends BaseListOptionsWithSkipCount {

    /** List only groups that have these child groups. */
    children?: number[];

    /**
     * List only groups that have the specified permissions in these folders
     * (used together with `privileges`).
     */
    folder?: number[];

    /** ID(s) of the group(s) to list. */
    id?: number[];

    /** List only groups that have these users as members. */
    memberid?: number[];

    /** List only groups that have these users (usernames) as members. */
    memberlogin?: string[];

    /** List only groups, whose names match this pattern. */
    name?: string[];

    /** List only groups that have these folder permissions (privileges). */
    privileges?: PrivilegeFlagName[];

    /** If set, the list of groups will be reduced: for nested groups, only parent group(s) or child group(s) will be returned. */
    reduce?: 'child' | 'parent';

    sortby?: 'id' | 'name';

    /** flag whether the permissions should be added to the reponse */
    perms?: boolean;
}

/**
 * Query parameters for `/group/list`
 */
export interface GroupListOptions extends BaseListOptionsWithPaging<Group>, EmbedListOptions<'group'> {
    perms?: boolean;
}

/**
 * Query parameters for listing the permissions assigned to a `Group`.
 */
export interface GroupPermissionsListOptions {

    /** The channel, for which to list the permissions. */
    channelId?: number;

    /**
     * ID of the instance of the `AccessControlledType`, for which
     * to list the permissions on its children.
     */
    parentId?: number;

    /**
     * `AccessControlledType`, for which to list the permissions on its child types.
     */
    parentType?: AccessControlledType;

}

/**
 * Request for setting permissions for a `Group`.
 */
export interface GroupSetPermissionsRequest {

    /** List of permissions to change */
    perms?: Pick<PermissionInfo, 'type' | 'value'>[];

    /** List of role assignments to change. */
    roles?: Pick<RoleAssignment, 'id' | 'value'>[];

    /** True to set permissions also to subgroups, false for only the given group. */
    subGroups: boolean;

    /** True to set permissions also for subobjects, false for only the given object. */
    subObjects: boolean;

}

/**
 * Query parameters for `group/{id}/users`.
 */
export interface GroupUsersListOptions extends BaseListOptionsWithPaging<Omit<User<Raw>, 'groups'>> { }

// DATASOURCE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/datasources`
 */
export type DataSourceListOptions = BaseListOptionsWithPaging<DataSource>;

/**
 * Request used for saving a `DataSource`.
 */
export type DataSourceCreateRequest = Partial<DataSource<Raw>>;

export type DataSourceUpdateRequest = DataSourceCreateRequest;


/**
 * Request used for saving a `DataSource Entry`.
 */
export type DataSourceEntryCreateRequest = Partial<DataSourceEntry<Raw>>;

export type DataSourceEntryListUpdateRequest = Partial<DataSourceEntry<Raw>>[];

/**
 * Query parameters for `/datasources/{id}/constructs`
 */
export type DataSourceConstructListOptions = BaseListOptionsWithPaging<TagType>;

// PACKAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/package`
 */
export type PackageListOptions = BaseListOptionsWithPaging<Package>;

export interface PackageSyncOptions {
    wait?: number;
}

export interface PackageCheckFilter {
    type: DependencyType;
    filter: Filter;
}

enum Filter {
    INCOMPLETE,
    ALL
}



export interface PackageCheckOptions extends PackageSyncOptions{
    checkAll?: boolean;
    filter: PackageCheckFilter
}


/**
 * Request used for saving a `Package`.
 */
export interface PackageCreateRequest {
    name: string;
    subPackages?: PackageCreateRequest[];
}

export type PackageUpdateRequest = PackageCreateRequest;

// ROLE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for GET `/roles`
 */
export type RoleListOptions = BaseListOptionsWithPaging<Role>;

/**
 * Request used for saving a `Role`.
 */
export type RoleCreateRequest = Pick<Role, 'nameI18n' | 'descriptionI18n'>;

export type RoleUpdateRequest = Pick<Role, 'id'> & Partial<Pick<Role, 'nameI18n' | 'descriptionI18n'>>;

/**
 * Query parameters for POST `/roles/{id}/perm`
 */
export type RolePermissionsUpdateRequest = RolePermissions;

// CONSTRUCT/TAGTYPE /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/construct/list`
 */
export type ConstructListOptions = BaseListOptionsWithPaging<TagType>;

/**
 * Request used for saving a `TagType`.
 */
export type ConstructCreateRequest = Partial<Omit<TagType<Raw>, 'name' | 'description'>>;

/**
 * Query Parameter options for creating a new `TagType`/Construct
 */
export interface ConstructCreateOptions {
    nodeId: number[];
}

export interface ConstructNodeLinkRequest {
    ids: number[];
    targetIds: EntityIdType[];
}

export type ConstructUpdateRequest = ConstructCreateRequest;

// CONSTRUCT/TAGTYPE CATEGORY /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/construct/category`
 */
export type ConstructCategoryListOptions = BaseListOptionsWithPaging<ConstructCategory>;

/**
 * Request used for saving a `ConstructCategory`.
 */
export type ConstructCategoryCreateRequest = Partial<ConstructCategory<Raw>>;

export type ConstructCategoryUpdateRequest = ConstructCategoryCreateRequest;

export type ConstructCategorySortRequest = IdSetRequest;

// CONTENTREPOSITORY /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/contentrepositories`
 */
export type ContentRepositoryListOptions = BaseListOptionsWithPaging<ContentRepository>;

/**
 * Request used for saving a `ContentRepository`.
 */
export interface ContentRepositoryCreateRequest {
    /** Name of the ContentRepository */
    name: string;
    /** Type of the ContentRepository */
    crType: ContentRepositoryType;
    /** DB Type of the ContentRepository */
    dbType: string;
    /** Username for accessing the ContentRepository */
    username: string;
    /** Password for accessing the ContentRepository */
    password?: string;
    /** True when a password is set */
    usePassword: boolean;
    /** URL for accessing the ContentRepository */
    url: string;
    /** Basepath for filesystem attributes */
    basepath: string;
    /** Flag for instant publishing */
    instantPublishing: boolean;
    /** Flag for publishing language information */
    languageInformation: boolean;
    /** Flag for publishing permission information */
    permissionInformation: boolean;
    /** Property containing the permission (role) information for Mesh CRs */
    permissionProperty: string;
    /** Default permission (role) to be set on objects in Mesh CRs */
    defaultPermission: string;
    /** Flag for differential deleting of superfluous objects */
    diffDelete: boolean;
    /** Get the elasticsearch specific configuration of a Mesh CR */
    elasticsearch?: CRElasticsearchModel;
    /** Flag for publishing every node into its own project for Mesh contentrepositories */
    projectPerNode: boolean;
    /** Flag for HTTP/2 support */
    http2: boolean;
}

export type ContentRepositoryUpdateRequest = Partial<ContentRepositoryCreateRequest>;

// CR_FRAGMENT //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/cr_fragments`
 */
export type ContentRepositoryFragmentListOptions = BaseListOptionsWithPaging<ContentRepositoryFragment>;

/**
 * Request used for saving a `ContentRepositoryFragment`.
 */
export interface ContentRepositoryFragmentCreateRequest {
    name: string;
}

export type ContentRepositoryFragmentUpdateRequest = ContentRepositoryFragmentCreateRequest;

// OBJECTPROPERTY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export type ObjectPropertyEmbedTypes = 'construct' | 'category';

/**
 * Query parameters for `/objectproperty`
 */
export interface ObjectPropertyListOptions extends BaseListOptionsWithPaging<ObjectProperty>, EmbedListOptions<ObjectPropertyEmbedTypes> {
    type?: number[];
}

type WriteableObjectProperty = Omit<ObjectProperty<Raw>, 'id' | 'globalId' | 'name' | 'construct' | 'category'>;

/**
 * Request used for saving a `ObjectProperty`.
 */
export type ObjectPropertyCreateRequest = WriteableObjectProperty;

export type ObjectPropertyUpdateRequest = Partial<Omit<WriteableObjectProperty, 'keyword'>>;

// OBJECTPROPERTYCATEGORIES //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/objectproperty`
 */
export type ObjectPropertyCategoryListOptions = BaseListOptionsWithPaging<ObjectPropertyCategory>;

/**
 * Request used for saving a `ObjectPropertyCategory`.
 */
export type ObjectPropertyCategoryCreateRequest = Partial<ObjectPropertyCategory<Raw>>;

export type ObjectPropertyCategoryUpdateRequest = ObjectPropertyCategoryCreateRequest;

// TAGMAPENTRY /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Query parameters for `/contentrepositories`
 */
export type TagmapEntryListOptions = BaseListOptionsWithPaging<TagmapEntry>;

/**
 * Request used for saving a `TagmapEntry`.
 */
export interface TagmapEntryCreateRequest {
    /** Tag name (property to resolve for the object) */
    tagname: string;
    /** Map name (name of the attribute in the ContentRepository) */
    mapname: string;
    /** Type of the object */
    object: number;
    /** Type of the object */
    objType: number;
    /** Attribute Type */
    attributeType: number;
    /** Type of the target object for link attributes */
    targetType: number;
    /** Multivalue flag */
    multivalue: boolean;
    /** Optimized flag */
    optimized: boolean;
    /** Reserved flag */
    reserved: boolean;
    /** Filesystem flag */
    filesystem: boolean;
    /** Name of the foreign attribute for foreignlink attributes */
    foreignlinkAttribute: string;
    /** Rule for restricting foreign linked objects */
    foreignlinkAttributeRule: string;
    /** Entry category */
    category?: string;
    /** True when the entry is a segmentfield (of a Mesh ContentRepository) */
    segmentfield?: boolean;
    /** True when the entry is a displayfield (of a Mesh ContentRepository) */
    displayfield?: boolean;
    /** True when the entry is a urlfield (of a Mesh ContentRepository) */
    urlfield?: boolean;
    /** Get the elasticsearch specific configuration of a Mesh CR */
    elasticsearch?: object;
    /** Get the micronode filter (for entries of type "micronode") */
    micronodeFilter?: string;
    /** Name of the CR Fragment, this entry belongs to. Null, if the entry directly belongs to the ContentRepository. */
    fragmentName?: string;
}

export type TagmapEntryUpdateRequest = Partial<TagmapEntryCreateRequest>;

/**
 * Update a link (optionally together with other occurrences) by replacing the URL with the given URL.
 * Additional query options for `linkChecker/pages/{pageId}/links/{id}`
 *
 * https://www.gentics.com/Content.Node/guides/restapi/resource_LinkCheckerResource.html#resource_LinkCheckerResource_updateLink_POST
 */
export interface UpdateExternalLinkRequestOptions {
    /** Wait timeout in milliseconds. */
    wait?: number;
}


// MAINTENANCE MODE /////////////////////////////////////////////////////////////////////////////////////////////////////

export interface MaintenanceModeRequestOptions {
    maintenance: boolean;
    banner: boolean;
    message: string;
}

// TEMPLATES /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Options which may be passed as query parameters to the `template/load/id` endpoints
 */
export interface TemplateRequestOptions {

    /** The node-id in which context the template should be loaded from. */
    nodeId?: number;

    /** True if it should load the referenced constructs as well. Defaults to false. */
    construct?: boolean;

    /** True if the template is being loaded for editing/updating. Response will then contain the `objectTags` as well. Defaults to false. */
    update?: boolean;
}

/**
 * Options which need to be passed as JSON body properties to the `template/link/id` and `template/unlink/id` endpoints
 */
export interface TemplateLinkRequestOptions {
    /**
     * Set of folder IDs to handle. Folder IDs may either be local or global IDs.
     */
    folderIds: EntityIdType[];
    /**
     * Node ID for handling channel local folders. Note that linking templates to folders
     * is always done for the master objects. It is not possible to have a different linking
     * for inherited or localized templates or folders.
     */
    nodeId: number;
    /**
     * True if the template shall be linked to or unlinked from the given folders
     * recursively (including all subfolders). The default is false (not recursive).
     */
    recursive?: boolean;
    /**
     * Flag to delete the template(s) when unlinked from the last folder.
     * Defaults to false.
     */
    delete?: boolean;
}

/**
 * Query-Parameters to control the pagination of the `GET template/{id}/folders` endpoint.
 */
export interface TemplateLinkListOptions {
    /** By which property should be sorted by */
    sort?: CommonSortFields;

    /** The query term to filter items */
    q?: string;

    /** Which page should be loaded */
    page?: number;

    /** How big a page should be */
    pageSize?: number;
}

export type TemplateTagEmbedTypes = 'construct';

export interface TemplateTagsRequestOptions extends BaseListOptionsWithPaging<TemplateTag>, EmbedListOptions<TemplateTagEmbedTypes> {
}

/**
 * Request object to update a template via `POST template/{id}` endpoint.
 */
export interface TemplateSaveRequest {
    /** List of tag names which need to be deleted. */
    delete?: string[];
    /** If it should force a sync of all incompatible tags in pages. */
    forceSync?: boolean;
    /** List of tag names which need to be synced. */
    sync?: string[];
    /** If the update should sync all template-tags to all pages which are based on this template. */
    syncPages?: boolean;
    /** If this update unlocks the template to be editable by other users again. */
    unlock?: boolean;
    /** The changes to the template */
    template?: Partial<Template<Raw>>;
}

/** Query-Parameters for the `POST template/{id}` endpoint. */
export interface TemplateSaveOptions extends TemplateRequestOptions { }

export interface TagStatusOptions {
    sort?: string;
    q?: string;
    page?: number;
    pageSize?: number;
}

export interface TemplateCreateRequest {
    folderIds: (number | string)[];
    nodeId: number;
    template: Partial<Template<Raw>>;
}

// SCHEDULER /////////////////////////////////////////////////////////////////////////////////////////////////////

export type ScheduleEmbedTypes = 'task';

/**
 * Query-Parameters for `GET /scheduler/schedule`
 */
export interface ScheduleListOptions extends BaseListOptionsWithPaging<Schedule>, EmbedListOptions<ScheduleEmbedTypes> {
    perms?: boolean;
}

/**
 * Query-Parameters for `GET /scheduler/task`
 */
export interface ScheduleTaskListOptions extends BaseListOptionsWithPaging<ScheduleTask> {
    perms?: boolean;
}

/**
 * Query-Parameters for `GET /scheduler/schedule/{id}/executions`
 */
export interface ScheduleExecutionListOptions extends BaseListOptionsWithPaging<ScheduleExecution> {
    /**
     * Filter for result status.
     * If set to `true`, only failed executions will be returned.
     * If set to `false`, only successful executions will be returned (include executions that were not yet executed).
     * If not set (default), all jobs will be returned.
     */
    failed?: boolean;

    /**
     * Filter for minimum starting time.
     * Only executions which have been started ***after*** the given timestamp will be returned.
     */
    ts_min?: number;

    /**
     * Filter for maximum starting time.
     * Only executions which have been started ***before*** the given timestamp will be returned.
     * **Note**: If `ts_min` is less than `ts_max`, then `ts_max` will be ignored.
     */
    ts_max?: number;
}

export interface SchedulerSuspendRequest {
    /** Ids of Schedules which are still allowed to be executed/run. */
    allowRun?: number[];
}

type WritableScheduleTask = Omit<ScheduleTask, 'id' | 'creatorId' | 'cdate' | 'editorId' | 'edate' | 'internal'>;

export type ScheduleTaskCreateRequest = WritableScheduleTask;

export type ScheduleTaskSaveRequest = Partial<WritableScheduleTask>;

type WriteableSchedule = Omit<Schedule, 'id' | 'creatorId' | 'cdate' | 'editorId' | 'edate' | 'status' | 'runs' | 'averageTime' | 'lastExecution'>;

export type ScheduleCreateReqeust = WriteableSchedule;

export type ScheduleSaveReqeust = Partial<WriteableSchedule>;

// CONTENT STAGING /////////////////////////////////////////////////////////////////////////////////////////////////////

export interface ContentPackageListOptions extends BaseListOptionsWithPaging<ContentPackage> {}

export interface ContentPackageSyncOptions {
    wait?: number;
}

export type ContentPackageCreateRequest = Required<Pick<ContentPackage, 'name'>> & Pick<ContentPackage, 'description'>;

export type ContentPackageSaveRequest = Partial<Pick<ContentPackage, 'name'> & Pick<ContentPackage, 'description'>>;

export interface AssignEntityToContentPackageOptions {
    recursive?: boolean;
}
