import { Form, FormDownloadInfo } from './cms-form';
import { ConstructCategory } from './construct-category';
import {
    ContentPackage,
    ContentPackageChannel,
    ContentPackageFile,
    ContentPackageFolder,
    ContentPackageForm,
    ContentPackageImage,
    ContentPackageImportError,
    ContentPackageNode,
    ContentPackagePage,
    ContentPackageSyncProgress,
} from './content-package';
import { ContentRepository } from './content-repository';
import { ContentRepositoryFragment } from './cr-fragment';
import { DataSource } from './data-source';
import { DataSourceEntry } from './data-source-entry';
import { ElasticSearchIndex } from './elastic-search-index';
import { EmbeddedTool } from './embedded-tool';
import { ExternalLink } from './external-link';
import { Feature, NodeFeature, NodeFeatureModel, NodeFeatures } from './feature';
import { File } from './file';
import { Folder } from './folder';
import { Group } from './group';
import { PermissionsAndRoles, PermissionsSet } from './group-permissions';
import { I18nLanguage } from './i18n-language';
import { Image } from './image';
import { FolderItemType, InheritableItem, Item, Usage } from './item';
import { Language } from './language';
import { ContentRepositoryLicense, LicenseCheckResult } from './license';
import { MarkupLanguage } from './markup-language';
import { MessageFromServer } from './message';
import { Node } from './node';
import { ObjectProperty } from './object-property';
import { ObjectPropertyCategory } from './object-property-category';
import { Package } from './package';
import { Page, PageWithExternalLinks } from './page';
import { GcmsPermission, PermissionsMapCollection } from './permissions';
import { PrivilegeMapFromServer } from './privileges';
import { Role, RolePermissions } from './role';
import { Schedule } from './schedule';
import { ScheduleExecution } from './schedule-execution';
import { ScheduleTask } from './schedule-task';
import { SchedulerStatus } from './scheduler';
import { StagedItemsMap, StagingStatus } from './staging-status';
import { Construct, Tag, TagStatus, TagType } from './tag';
import { TagmapEntry, TagmapEntryError } from './tagmap-entry';
import { Template } from './template';
import { Raw } from './type-util';
import { User } from './user';
import { UsersnapSettings } from './usersnap';
import { NodeVersionInfo, Variant } from './version';

// GENERIC RESPONSE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export enum ResponseCode {
    OK = 'OK',
    NOT_FOUND = 'NOTFOUND',
    INVALID_DATA = 'INVLIDDATA',
    FAILURE = 'FAILURE',
    PERMISSION = 'PERMISSION',
    AUTH_REQUIRED = 'AUTHREQUIRED',
    MAINTENANCE_MODE = 'MAINTENANCEMODE',
    NOT_LICENSED = 'NOTLICENSED',
    LOCKED = 'LOCKED',
}

/**
 * Most responses contain a responseInfo property, so should
 * extend this interface.
 */
export interface Response {
    [x: string]: any;

    responseInfo: {
        responseCode: ResponseCode;
        responseMessage?: string;
    };

    /** Messages contained in the response (which should be shown to the user). */
    messages?: ResponseMessage[];

}

export type UnixTimestamp = number;

/**
 * Many responses contain a human-readable list of status messages,
 * sometimes in a different Language than the responseInfo.responseMessage.
 */
export interface ResponseMessage {
    id?: number;
    timestamp: UnixTimestamp;
    message?: string;
    type: 'CRITICAL' | 'INFO' | 'SUCCESS' | 'WARNING';
    fieldName?: string;
    image?: string;
}

/**
 * A response with normalized entities (produced by normalizr) added.
 */
export interface NormalizedResponse extends Response {
    _normalized?: {
        result: number | number[];
        entities: { [key: string]: any; };
    };
    [key: string]: any;
}

/**
 * Common properties shared by lists of folders, pages, files, images & templates.
 */
export interface BaseListResponse extends Response {
    hasMoreItems: boolean;
    messages: ResponseMessage[];
    numItems: number;
}

/**
 * Generic list response.
 *
 * @note This cannot be used for all list endpoints, because some of them do not use the
 * `items` property to store the elements.
 */
export interface ListResponse<T> extends BaseListResponse {
    items: T[];
}

export interface PermissionListResponse<T> extends ListResponse<T> {
    perms?: {
        [itemId: number]: GcmsPermission[];
    }
}

export interface StagableItemResponse {
    stagingStatus?: StagingStatus;
}

export interface StageableListResponse<T> extends ListResponse<T>, StagedItemsList { }

export interface StagedItemsList {
    stagingStatus?: StagedItemsMap;
}

/**
 * Generic single item response.
 */
export interface GenericItemResponse<T> extends Response {
    item: T;
}

/**
 * Response from `folder/getItems`
 */
export interface ItemListResponse extends ListResponse<Item<Raw>>, StagedItemsList { }

export interface ImageListResponse extends ListResponse<Image<Raw>>, StagedItemsList { }

/**
 * Response from `/admin/actionlog`
 */
export interface LogsListResponse extends ListResponse<any> { }

/**
 * When loading item lists, we add the requested item type to the response.
 * Response from `folder/getItems`
 */
export interface TypedItemListResponse extends ItemListResponse, StagedItemsList {
    type: 'page' | 'file' | 'image';
}

/**
 * Response from `folder/delete` / `page/delete` / `file/delete` / `image/delete`
 */
export interface ItemDeleteResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from POST `node/id`
 */
export interface ItemSaveResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from `<type>/copy`
 */
export interface ItemCopyResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from `<type>/move`
 */
export interface ItemMoveResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from the `<type>/disinherit/<id>` endpoint.
 */
export interface InheritanceResponse extends Response {
    exclude: boolean;
    disinherit: number[];
    disinheritDefault: boolean;
    inheritable: number[];
}

export interface MultipleInheritanceResponse extends Response {
    exclude: boolean;
    disinherit: number[];
    partialDisinherit: number[];
    inheritable: number[];
}

// FOLDER //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `folder/load/:id`
 */
export interface FolderResponse extends Response {
    folder: Folder<Raw>;
}

/**
 * Response from `folder/getList`
 */
export interface FolderListResponse extends BaseListResponse, StagedItemsList {
    folders: Folder<Raw>[];
}

/**
 * Response from `folder/create`
 */
export interface FolderCreateResponse extends Response {
    folder: Folder<Raw>;
    messages: ResponseMessage[];
}

export interface PageExternalLink {
    pageId: number;
    pageName: string;
    links: string[];
}

export interface FolderExternalLinksResponse extends Response {
    pages: PageExternalLink[];
}

// INDEX //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `index`
 */
export interface ElasticSearchIndexListResponse extends ListResponse<ElasticSearchIndex<Raw>> { }

// USER //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `GET /user`
 */
export interface UserListResponse extends ListResponse<User<Raw>> { }

/**
 * Response from `user/id/groups`
 */
export interface UserGroupsResponse extends PermissionListResponse<Group<Raw>> { }

/**
 * Response from `user/id/groups/id/nodes`
 */
export interface UserGroupNodeRestrictionsResponse extends Response {
    nodeIds: number[];
    hidden: number;
}

/**
 * Response from `user/load/:id`
 */
export interface UserResponse extends Response {
    user: User<Raw>;
}

/**
 * Response from `PUT group/id/users`
 */
export interface UserUpdateResponse extends Response {
    user: User<Raw>;
    messages: ResponseMessage[];
}

/**
 * Response from `GET language/${id}` and `PUT /language/{id}`
 */
export interface LanguageResponse extends Response {
    language: Language;
}

// GROUP //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `GET /group/load`
 */
export interface GroupTreeResponse extends Response {
    groups: Group<Raw>[];
}

/**
 * Response from `GET /group`
 */
export interface GroupListResponse extends PermissionListResponse<Group<Raw>> {}

/**
 * Response from `POST group/id/users`
 */
export interface GroupUserCreateResponse extends Response {
    user: User<Raw>;
    messages: ResponseMessage[];
}

/**
 * Response from `GET group/{id}/users`
 */
export interface GroupUsersResponse extends ListResponse<User<Raw>> { }

/**
 * Response from `group/load/:id`
 */
export interface GroupResponse extends Response {
    group: Group<Raw>;
}

/**
 * Response from `group/create`
 */
export interface GroupCreateResponse extends Response {
    group: Group<Raw>;
    messages: ResponseMessage[];
}

/**
 * Response from `group/{id}/perms`
 */
export interface GroupPermissionsListResponse extends ListResponse<PermissionsSet> { }

/**
 * Response from `group/{id}/perms/{type}` and `group/{id}/perms/{type}/{instanceId}`
 */
export interface GroupTypeOrInstancePermissionsResponse extends Response, PermissionsAndRoles { }

// DATA //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `user/me/data`
 */
export interface UserDataResponse extends Response {
    data: any;
}

// TEMPLATE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `template/load/:id`
 */
export interface TemplateResponse extends Response {
    template: Template<Raw>;
}

/**
 * Response from `folder/getTemplates`
 */
export interface TemplateListResponse extends BaseListResponse {
    templates: Template<Raw>[];
}

/**
 * Response from `node/{nodeId}/templates`
 */
export interface PagedTemplateListResponse extends PermissionListResponse<Template<Raw>> { }

/**
 * Response from `node/{nodeId}/constructs` and the like
 */
export interface PagedConstructListResponse extends PermissionListResponse<Construct> { }

/**
 * Response from `template/link/id` and `template/unlink/id`
 */
export interface TemplateLinkResponse extends Response { }

/**
 * Response from `GET template/{id}/folders`
 */
export interface PagedTemplateLinkListResponse extends ListResponse<Folder<Raw>> { }

/**
 * Response from `POST template/{id}`
 */
export interface TemplateSaveResponse extends Response { }

/**
 * Response from `GET template/{id}/tagstatus`
 */
export interface TemplateTagStatusResponse extends ListResponse<TagStatus> { }

/**
 * Response from `GET template/getTags/{id}`
 */
export interface TemplateTagsResponse extends ListResponse<Tag> {}

// PAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `page/create`
 */
export interface PageCreateResponse extends Response {
    page: Page<Raw>;
    messages: ResponseMessage[];
}

/**
 * Response from `page/load/:id`
 */
export interface PageResponse extends Response {
    page: Page<Raw>;
}

/**
 * Response from `folder/getPages`
 */
export interface PageListResponse extends BaseListResponse, StagedItemsList {
    pages: Page<Raw>[];
}

export interface PageCopyResultInfo {
    newPageId: number;
    targetFolderId: number;
    targetFolderChannelId: number;
    sourcePageId: number;
}

export interface PageCopyResponse extends Response {
    pages: Page<Raw>[];
    pageCopyMappings: PageCopyResultInfo[];
}

export interface PagePreviewResponse extends Response {
    preview: string;
}

export interface PageRenderResponse extends Response {
    content: string;
    properties?: Record<string, any>;
    tags?: Tag[];
    metaeditables?: any[];
    time: number;
    inheritedContent?: string;
    inheritedProperties?: Record<string, string>;
}

export interface PageTagListResponse extends Response {
    tags: Tag[];
}

export interface TagCreateResponse extends Response {
    tag: Tag;
}

export interface MultiTagCreateResponse extends Response {
    created: Record<string, Tag>;
}

export type ICreateResponse = FolderCreateResponse | PageCreateResponse;

// FILE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `file/load/:id`
 */
export interface FileResponse extends Response {
    file: File<Raw>;
}

/**
 * Response from `folder/getFiles`
 */
export interface FileListResponse extends BaseListResponse, StagedItemsList {
    files: File<Raw>[];
}

/**
 * Response from `file/upload` and `file/copy`
 */
export interface FileUploadResponse extends FileResponse {
    success: boolean;
}

// IMAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `image/load/:id`
 */
export interface ImageResponse extends Response {
    image: Image<Raw>;
}

// FORM //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `POST /form`
 */
export interface FormCreateResponse extends Response {
    item: Form<Raw>;
    messages: ResponseMessage[];
}

/**
 * Response from `GET /form/:id`
 */
export interface FormResponse extends Response, StagableItemResponse {
    item: Form<Raw>;
}

/**
 * Response from `GET|POST /form/:id/(binaries|export)`
 */
export interface FormDownloadInfoResponse extends FormDownloadInfo, Response {}

/**
 * Response from `GET /form`
 */
export interface FormListResponse extends StageableListResponse<Form<Raw>> {

}

/**
 * Response from `GET /form/{id}/data`
 */
export interface FormDataListResponse {
    totalCount: number,
    currentPage: number,
    pageCount: number,
    perPage: number,
    entries: FormDataListEntry[];
    elements: FormDataListElement[]
}

/**
 * Interface for entries for FormDataListResponse
 */
export interface FormDataListEntry {
    fields: { [key: string]: any };
    uuid: string;
}

/**
 * Interface for elements for FormDataListResponse
 */
export interface FormDataListElement {
    active: boolean;
    elements: FormDataListElement[];
    globalId: string;
    mandatory: boolean;
    multivalue: boolean;
    type: string;
}

// LANGUAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `language`
 */
export interface LanguageListResponse extends ListResponse<Language> { }

// LANGUAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `POST /i18n/set`
 */
export type I18nLanguageSetResponse = Response;

/**
 * Response from `GET /i18n/get`
 */
export interface I18nLanguageResponse extends Response {
    code: string;
}

/**
 * Response from `GET /i18n/list`
 */
export interface I18nLanguageListResponse extends ListResponse<I18nLanguage> { }

// MARKUPLANGUAGE /////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `GET /markupLanguage`
 */
export interface MarkupLanguageListResponse extends ListResponse<MarkupLanguage> { }

// NODE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `node/load/:id`
 */
export interface NodeResponse extends Response {
    node: Node<Raw>;
}

/**
 * Response from `node`
 */
export interface NodeListResponse extends ListResponse<Node<Raw>> { }

/**
 * Response from `node/getLanguages`
 */
export interface NodeLanguagesResponse extends BaseListResponse {
    languages: Language[];
}

/**
 * Response from `folder/sanitize/publishDir`
 */
export interface FolderPublishDirSanitizeResponse extends Response {
    messages: ResponseMessage[];
    publishDir: string;
}

/**
 * Response from `node/:nodeId/languages`
 */
export interface NodeLanguagesListResponse extends ListResponse<Language> { }

// FEATURE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `node/features/:nodeId`
 */
export interface NodeFeatureResponse extends Response {
    features: NodeFeature[];
}

/**
 * Response from `node/features`
 */
export interface FeatureModelListResponse extends ListResponse<NodeFeatureModel> { }

/**
 * Response from `node/:nodeId/features`
 */
export interface FeatureListResponse extends ListResponse<keyof NodeFeatures> { }

// SETTING //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `node/:nodeId/settings`
 */
export interface NodeSettingsResponse extends Response {
    global: boolean;
    data: any;
}

// CONTENTREPOSITORY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `GET contentrepositories/:id`
 */
export interface ContentRepositoryResponse extends Response {
    contentRepository: ContentRepository;
}

/**
 * Response from `GET contentrepositories`
 */
export interface ContentRepositoryListResponse extends ListResponse<ContentRepository> { }

/**
 * Response from `POST contentrepositories`
 */
export type ContentRepositoryCreateResponse = ContentRepositoryResponse;

/**
 * Response from `PUT contentrepositories`
 */
export type ContentRepositoryUpdateResponse = ContentRepositoryResponse;

export interface ContentRepositoryListRolesResponse extends Response {
    roles: string[];
}

// CR_FRAGMENT //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `cr_fragments/:id`
 */
export interface ContentRepositoryFragmentResponse extends Response {
    contentRepositoryFragment: ContentRepositoryFragment;
}

/**
 * Response from `cr_fragments`
 */
export interface ContentRepositoryFragmentListResponse extends ListResponse<ContentRepositoryFragment> { }

// TAGMAPENTRY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `GET contentrepositories/:id`
 */
export interface TagmapEntryResponse extends Response {
    entry: TagmapEntry;
}

/**
 * Response from `GET contentrepositories`
 */
export interface TagmapEntryListResponse extends ListResponse<TagmapEntry> { }

/**
 * Response from `POST contentrepositories`
 */
export type TagmapEntryCreateResponse = TagmapEntryResponse;

/**
 * Response from `PUT contentrepositories`
 */
export type TagmapEntryUpdateResponse = TagmapEntryResponse;

/**
 * Response from `GET contentrepositories/:id/entries/check`
 */
export interface TagmapEntryCheckResponse extends ListResponse<TagmapEntryError> {}


// QUEUE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `/page/pubqueue/approve`
 */
export interface QueueApproveResponse extends Response {
    messages: ResponseMessage[];
}

// LOGIN //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * The response received from an auth request to `auth/login`
 */
export interface LoginResponse extends Response {
    sid: number;
    user: User<Raw>;
}

// VERSION //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * The response received from a request to `admin/version`
 */
export interface VersionResponse extends Response {
    cmpVersion: string;
    version: string;
    variant: Variant;
    nodeInfo: { [key: string]: NodeVersionInfo; };
}

/**
 * The response received from a request to `admin/features/<feature>`
 */
export interface FeatureResponse extends Response {
    name: Feature;
    activated: boolean;
}

// EMBEDDED TOOL //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * The response received from a request to `admin/tools`
 */
export interface EmbeddedToolsResponse extends Response {
    tools: EmbeddedTool[];
}

// MESSAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `msg/list`
 */
export interface MessageListResponse extends Response {
    messages: MessageFromServer[];
}

/**
 * Response from the `auth/validate` endpoint.
 */
export interface ValidateSidResponse extends Response {
    user: User<Raw>;
}

// MAINTENANCE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/** Response from the `info/maintenance` endpoint. */
export interface MaintenanceModeResponse extends Response {
    /** Whether or not the maintenance mode is active. */
    maintenance: boolean;

    messages: ResponseMessage[];
    /** Whether to show a banner or not. */
    banner: boolean;
    /** The message to display to the user, set when enabling maintenance mode. */
    message: string;
}

// SYNC //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface SyncObjectsResponse extends Response {
    haveDependencies: boolean;
    dependencies: SyncObjectsResponseDependencies;
    pages: Page<Raw>[];
    files: File<Raw>[];
    images: Image<Raw>[];
    pagesTotal: number;
    filesTotal: number;
    imagesTotal: number;
}

/**
 * @example
 * "dependencies" : {
 *     "page" : {
 *         "119" : {
 *             "page" : {
 *                 "116" : 3
 *             }
 *         },
 *         "123" : {
 *             "page" : {
 *                 "124" : 5,
 *                 "125" : 5,
 *                 "126" : 5
 *             },
 *             "image" : {
 *                  "190" : 5
 *             },
 *             "file" : {
 *                 "186" : 5
 *             }
 *         }
 *     }
 * }
 */
export interface SyncObjectsResponseDependencies {
    page?: { [dependencyId: number]: SyncObjectsResponseTransientDependencies };
    image?: { [dependencyId: number]: SyncObjectsResponseTransientDependencies };
    file?: { [dependencyId: number]: SyncObjectsResponseTransientDependencies };
}

export interface SyncObjectsResponseTransientDependencies {
    page?: { [nodeId: number]: number };
    image?: { [nodeId: number]: number };
    file?: { [nodeId: number]: number };
}

// PERMISSION //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from the `perm/<type>/<id>` endpoint.
 */
export interface PermissionResponse extends Response {

    /** Permission bits. */
    perm: string;

    /** Role permission bits. */
    rolePerm?: string;

    /**
     * Map representation of all privileges.
     *
     * @deprecated Use `permissionsMap` instead.
     */
    privilegeMap?: PrivilegeMapFromServer;

    /**
     * Map representation of all permissions
     */
    permissionsMap?: PermissionsMapCollection;
}

export interface GroupPermissionBitsResponse extends Response {
    groups: Record<string, string>;
}

/**
 * A response for a single permission type (i.E. 'view', 'edit') for a single instance.
 * Response from `perm/<permission>/<type>/<id>` endpoint.
 */
export interface SinglePermissionResponse extends Response {
    granted: boolean;
}

export interface PolicyResponse {
    name: string;
    uri: string;
}

export interface GroupPolicy {
    default: boolean;
    name: string;
    uri: string;
}

export interface PolicyGroupResponse {
    policy: GroupPolicy[];
}

// USAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `<type>/usage/total` endpoint.
 */
export interface TotalUsageResponse extends Response {
    infos: {
        [itemId: number]: Usage,
    };
}

// LOCALIZATION //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `<type>/localizations/<id>` endpoint.
 */
export interface LocalizationsResponse extends Response {
    masterId: number;
    masterNodeId: number;
    nodeIds: { [itemId: number]: number };
    total: number;
    hidden: number;
    online: number[];
    offline: number[];
}

export interface ChannelLocalizationInfo extends ObjectLocalizationInfo {
    channelId: number;

    folder: ObjectLocalizationInfo;
    page: ObjectLocalizationInfo;
    image: ObjectLocalizationInfo;
    file: ObjectLocalizationInfo;
    template: ObjectLocalizationInfo;
}

export interface ObjectLocalizationInfo {
    inherited: number,
    localizaed: number,
    local: number;
}

export interface LocalizationInfoResponse extends Response {
    channels: ChannelLocalizationInfo[];
}

// USAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Base response for calls to `<type>/usage/<usedBy>`
 */
export interface BaseUsageResponse extends Response {
    total: number;
    withoutPermission: number;
}

/**
 * Response from calls to `<type>/usage/<page | variant | tag>`
 * Pages which have variants
 */
export interface PageUsageResponse extends BaseUsageResponse {
    pages: Page<Raw>[];
}

/**
 * Response from calls to `<type>/usage/<file | image>`
 */
export interface FileUsageResponse extends BaseUsageResponse {
    files: File<Raw>[];
}

/**
 * Response from calls to `<type>/usage/folder`
 */
export interface FolderUsageResponse extends BaseUsageResponse {
    folders: Folder<Raw>[];
}

/**
 * Response from calls to `<type>/usage/template`
 */
export interface TemplateUsageResponse extends BaseUsageResponse {
    templates: Template<Raw>[];
}

// CONSTRUCT //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from calls to `/construct/load/<id>`
 */
export interface ConstructLoadResponse extends Response {
    construct: TagType;
}

/**
 * Response from the `/constructs` endpoint
 */
export interface ConstructListResponse extends BaseListResponse {
    items?: TagType[];
}

/**
 * Response from the `/devtools/packages/{packageName}/constructs` endpoint.
 */
export interface DevToolsConstructListResponse extends ListResponse<TagType> { }

/**
 * Response from `POST /construct`
 */
export type ConstructCreateResponse = ConstructLoadResponse;

/**
 * Response from `PUT /construct`
 */
export type ConstructUpdateResponse = ConstructLoadResponse;

/**
 * Response from `GET /construct/{id}/nodes`
 */
export interface ConstructLinkedNodesResponse extends ListResponse<Node> { }

export interface ConstructNodeLinkResponse extends Response { }

// CONSTRUCT CATEGORY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from calls to `/construct/category/<id>`
 */
export interface ConstructCategoryLoadResponse extends Response {
    constructCategory: ConstructCategory;
}

/**
 * Response from endpoint `/constructs/category`
 */
export interface ConstructCategoryListResponse extends ListResponse<ConstructCategory> { }

/**
 * Response from `POST /construct/category`
 */
export type ConstructCategoryCreateResponse = ConstructCategoryLoadResponse;

/**
 * Response from `PUT /construct/category`
 */
export type ConstructCategoryUpdateResponse = ConstructCategoryLoadResponse;

// DATASOURCE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from GET `/datasource/<id>`
 */
export interface DataSourceLoadResponse extends Response {
    datasource: DataSource;
}

/**
 * Response from GET `/datasource`
 */
export interface DataSourceListResponse extends ListResponse<DataSource> { }

/**
 * Response from POST `/datasource`
 */
export interface DataSourceCreateResponse extends Response {
    datasource: DataSource;
}

/**
 * Response from PUT `/datasource/<id>`
 */
export interface DataSourceUpdateResponse extends Response {
    datasource: DataSource;
}

/**
 * Response from GET `/datasource/<id>/entries/<entryId>`
 */
export interface DataSourceEntryLoadResponse extends Response {
    entry: DataSourceEntry;
}

/**
 * Response from GET `/datasource/<id>/entries`
 */
export interface DataSourceEntryListResponse extends ListResponse<DataSourceEntry> { }

/**
 * Response from POST `/datasource/<id>/entries`
 */
export interface DataSourceEntryCreateResponse extends Response {
    entry: DataSourceEntry;
}

/**
 * Response from PUT `/datasource/<id>/entries`
 */
export interface DataSourceEntryListUpdateResponse extends ListResponse<DataSourceEntry> { }

/**
 * Response from GET `/datasource/<id>/constructs`
 */
export interface DataSourceConstructListResponse extends ListResponse<TagType> { }

// PACKAGE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `/devtools/packages`
 */
export interface PackageListResponse extends ListResponse<Package> { }

/**
 * Response from calls to `/devtools/packages/<id>`
 */
export type PackageLoadResponse = Package;

export interface PackageCreateResponse extends Response {
    package: Package<Raw>;
}

export interface PackageSyncResponse extends Response {
    enabled: boolean;
    /** The user which enabled the sync. */
    user?: User;
}

// OBJECTPROPERTY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from calls to `/objectproperty/<id>`
 */
export interface ObjectPropertyLoadResponse extends Response {
    objectProperty: ObjectProperty;
}

/**
 * Response from `/objectproperty` and `/devtools/packages/{name}/objectproperties`
 */
export interface ObjectPropertyListResponse extends ListResponse<ObjectProperty> { }

/**
 * Response from `POST contentrepositories`
 */
export type ObjectPropertyCreateResponse = ObjectPropertyLoadResponse;

/**
 * Response from `PUT contentrepositories`
 */
export type ObjectPropertyUpdateResponse = ObjectPropertyLoadResponse;

// OBJECTPROPERTYCATEGORY //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from calls to `/objectpropertycategory/<id>`
 */
export interface ObjectPropertyCategoryLoadResponse extends Response {
    objectPropertyCategory: ObjectPropertyCategory;
}

/**
 * Response from `/objectproperty` and `/devtools/packages/{name}/objectproperties`
 */
export interface ObjectPropertyCategoryListResponse extends ListResponse<ObjectPropertyCategory> { }

/**
 * Response from `POST contentrepositories`
 */
export type ObjectPropertyCategoryCreateResponse = ObjectPropertyCategoryLoadResponse;

/**
 * Response from `PUT contentrepositories`
 */
export type ObjectPropertyCategoryUpdateResponse = ObjectPropertyCategoryLoadResponse;

// FILE NAME //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response containing the suggested fileName
 */
export interface SuggestPageFileNameResponse extends Response {
    fileName: string;
}

// ELASTIC SEARCH //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from calls to `elastic/_search`
 */
export interface ElasticSearchQueryResponse<T extends InheritableItem<Raw>> {
    _shards: {
        failed: number;
        successful: number;
        total: number;
    };
    hits: {
        hits: Array<{
            _id: number;
            _index: string;
            _score: number;
            _source?: {
                description: string;
                edited: number;
                editor: string;
                name: string;
                nodeId: number;
                templateId: number;

            };
            _object: T
            _type: FolderItemType;
        }>;
        max_score: number;
        total: number | {
            value: number;
            relation: 'eq';
        };
        /** JSON encoded "{ [packageName]: StagedItemsMap }" when in staging mode */
        staging?: string;
    };
    timed_out: boolean;
    took: number;
}

// ROLE //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from GET `/role/<id>`
 */
export interface RoleLoadResponse extends Response {
    role: Role;
}

/**
 * Response from GET `/role`
 */
export interface RoleListResponse extends ListResponse<Role> { }

/**
 * Response from PUT `/role`
 */
export interface RoleCreateResponse extends Response {
    role: Role;
}

/**
 * Response from POST `/role/<id>`
 */
export interface RoleUpdateResponse extends Response {
    role: Role;
}

/**
 * Response from GET `/role/<id>/perm`
 */
export interface RolePermissionsLoadResponse extends Response {
    perm: RolePermissions;
}

/**
 * Response from POST `/role/<id>/perm`
 */
export interface RolePermissionsUpdateResponse extends Response {
    perm: RolePermissions;
}

// WASTEBIN //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `wastebin/[folder|page|file|image]/restore`
 */
export interface WastebinRestoreResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from `wastebin/[folder|page|file|image]/delete`
 */
export interface WastebinDeleteResponse extends Response {
    messages: ResponseMessage[];
}

// CUSTOM: LINKCHECKER //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `linkChecker/pages`
 */
export interface LinkCheckerPageList extends ListResponse<PageWithExternalLinks<Raw>> { }

/**
 * Response from `linkChecker/pages/:id`
 */
export interface LinkCheckerExternalLinkList extends ListResponse<ExternalLink> { }

/**
 * Response from `linkChecker/pages/:id`
 */
export interface LinkCheckerUpdateResponse extends Response {
    messages: ResponseMessage[];
}

/**
 * Response from `linkChecker/check`
 */
export interface LinkCheckerCheckResponse extends LinkCheckerUpdateResponse {
    valid: boolean;
    reason?: string;
}

/**
 * Response from `linkChecker/stats`
 */
export interface ExternalLinkStatistics extends LinkCheckerUpdateResponse {
    /** Timestamp of last successful run of the link checker */
    lastRun: number;

    /** Number of valid external links */
    valid: number;

    /** Number of invalid external links */
    invalid: number;

    /** Number of unchecked external links */
    unchecked: number;
}

// LINK CHECKER //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from `linkChecker/stats`
 * Response containing information about an external link
 * `linkChecker/pages/{pageId}/links/{id}`
 */
export interface LinkResponse extends Response {
    /* External link */
    link: ExternalLink;

    /* Number of occurrences of the URL in the page */
    page: number;

    /* Number of occurrences of the URL in the node */
    node: number;

    /* Number of global occurrences of the URL */
    global: number;
}

// USER SNAP //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response containing the Usersnap key.
 */
export interface UsersnapSettingsResponse extends Response {
    settings: UsersnapSettings;
}

// MAINTENANCE MODE /////////////////////////////////////////////////////////////////////////////////////////////////////

export interface MaintenanceModeResponse extends Response {
    maintenance: boolean;
    banner: boolean;
    message: string;
}

// SCHEDULER //////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Response from:
 * `GET /scheduler/status`
 * `PUT /scheduler/suspend`
 * `PUT /scheduler/resume`
 */
export interface SchedulerStatusResponse extends Response {
    /** The current status of the scheduler */
    status: SchedulerStatus;

    /** Ids of Jobs that are allowed to run, although the scheduler is suspended. */
    allowRun: number[]
}

/**
 * Response from `GET /scheduler/task`
 */
export interface ScheduleTaskListResponse extends PermissionListResponse<ScheduleTask> { }

/**
 * Response from:
 * * `POST /scheduler/task`
 * * `GET /scheduler/task/{id}`
 * * `PUT /scheduler/task/{id}`
 */
export interface ScheduleTaskResponse extends GenericItemResponse<ScheduleTask> { }

/**
 * Response from `GET /scheduler/schedule`
 */
export interface ScheduleListResponse extends PermissionListResponse<Schedule> { }

/**
 * Response from:
 * * `POST /scheduler/schedule`
 * * `GET /scheduler/schedule/{id}`
 * * `PUT /scheduler/schedule/{id}`
 */
export interface ScheduleResponse extends GenericItemResponse<Schedule> { }

/**
 * Response from `GET /scheduler/schedule/{id}/execution`
 */
export interface ScheduleExecutionListResponse extends ListResponse<ScheduleExecution> { }

/**
 * Response from `GET /scheduler/schedule/{id}/execution/{id}`
 */
export interface ScheduleExecutionResponse extends GenericItemResponse<ScheduleExecution> { }

// CONTENT STAGING //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface ContentPackageListResponse extends ListResponse<ContentPackage> { }

export interface ContentPackageResponse extends Response {
    contentPackage: ContentPackage;
}

export interface JobProgress {
    done: number;
    total: number;
    started: number;
    finished: number;
}

export interface JobStatus {
    running: boolean;
    progress?: JobProgress;
    messages?: ResponseMessage[];
}

export interface ContentPackageSyncResponse extends Response {
    running: boolean;
    progress?: ContentPackageSyncProgress;
}

export interface ContentPackageErrorResponse extends Response {
    errors: ContentPackageImportError[];
    mismatches: string[];
    timestamp: string;
}

export interface ContentPackageFolderResponse extends Response {
    import: JobStatus;
    nodes: ContentPackageNode[];
    channels: ContentPackageChannel[];
    folders: ContentPackageFolder[];
    files: ContentPackageFile[];
    images: ContentPackageImage[];
    pages: ContentPackagePage[];
    forms: ContentPackageForm[];
}

// CLUSTERING //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface ClusterInformationResponse extends Response {
    /** If clustering feature is enabled. */
    feature: boolean;
    /** If this is the master node. */
    master: boolean;
    /** If the cluster (hazelcast) is started. */
    started: boolean;
    /** UUIDs of all cluster memebers. */
    memebers: string[];
    /** Info of this cluster memeber. */
    localMember: {
        /** UUID of this cluster memeber. */
        uuid: string;
        /** Name of this cluster memeber. */
        name: string;
    };
}

// HASHING/CONTENT STAGING //////////////////////////////////////////////////////////////////////////////////////////////////////////////

export interface ImplementationHashResponse extends Response {
    hash: string;
    base: string;
}

// FILE UPLOAD MANIPULATOR (FUM) /////////////////////////////////////////////////////////////////////////////////////////////////////

export enum FUMStatus {
    OK = 'OK',
    ERROR = 'ERROR',
}

export interface FUMStatusResponse {
    status: FUMStatus;
    type: string;
    msg: string;
}


export interface TranslationResponse {
    text: string;
}

// LICENSE /////////////////////////////////////////////////////////////////////////////////////////////////////

export interface LicenseInfoResponse extends Response {
    checkResult: LicenseCheckResult;
}

export interface LicenseUpdateResponse extends LicenseInfoResponse {}

export type LicenseContentRepositoryInfoResponse = ListResponse<ContentRepositoryLicense>;
