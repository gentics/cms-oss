import {
    ConstructCategory,
    ConstructCategoryCreateRequest,
    ConstructCategoryCreateResponse,
    ConstructCategoryListOptions,
    ConstructCategoryListResponse,
    ConstructCategoryLoadResponse,
    ConstructCategoryUpdateRequest,
    ConstructCategoryUpdateResponse,
    ConstructCreateOptions,
    ConstructCreateRequest,
    ConstructCreateResponse,
    ConstructLoadResponse,
    ConstructUpdateRequest,
    ConstructUpdateResponse,
    ContentPackage,
    ContentPackageCreateRequest,
    ContentPackageListOptions,
    ContentPackageListResponse,
    ContentPackageResponse,
    ContentPackageSaveRequest,
    ContentRepository,
    ContentRepositoryCreateRequest,
    ContentRepositoryCreateResponse,
    ContentRepositoryFragment,
    ContentRepositoryFragmentCreateRequest,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentListResponse,
    ContentRepositoryFragmentResponse,
    ContentRepositoryFragmentUpdateRequest,
    ContentRepositoryListOptions,
    ContentRepositoryListResponse,
    ContentRepositoryResponse,
    ContentRepositoryUpdateRequest,
    ContentRepositoryUpdateResponse,
    DataSource,
    DataSourceCreateRequest,
    DataSourceCreateResponse,
    DataSourceListOptions,
    DataSourceListResponse,
    DataSourceLoadResponse,
    DataSourceUpdateRequest,
    DataSourceUpdateResponse,
    DevToolsConstructListResponse,
    Folder,
    FolderCreateRequest,
    FolderCreateResponse,
    FolderListOptions,
    FolderListRequest,
    FolderListResponse,
    FolderResponse,
    FolderSaveRequest,
    FolderSaveRequestOptions,
    Group,
    GroupCreateRequest,
    GroupCreateResponse,
    GroupListOptions,
    GroupListResponse,
    GroupResponse,
    GroupUpdateRequest,
    GroupUserCreateRequest,
    GroupUserCreateResponse,
    Language,
    LanguageCreateRequest,
    LanguageListOptions,
    LanguageListResponse,
    LanguageResponse,
    LanguageUpdateRequest,
    ListResponse,
    Node,
    NodeCreateRequest,
    NodeDeleteRequestOptions,
    NodeListRequestOptions,
    NodeListResponse,
    NodeResponse,
    NodeSaveRequest,
    NodeSaveRequestOptions,
    ObjectProperty,
    ObjectPropertyCategory,
    ObjectPropertyCategoryCreateRequest,
    ObjectPropertyCategoryCreateResponse,
    ObjectPropertyCategoryListOptions,
    ObjectPropertyCategoryListResponse,
    ObjectPropertyCategoryLoadResponse,
    ObjectPropertyCategoryUpdateRequest,
    ObjectPropertyCreateRequest,
    ObjectPropertyCreateResponse,
    ObjectPropertyListOptions,
    ObjectPropertyListResponse,
    ObjectPropertyLoadResponse,
    ObjectPropertyUpdateRequest,
    ObjectPropertyUpdateResponse,
    Package,
    PackageCreateRequest,
    PackageCreateResponse,
    PackageListOptions,
    PackageListResponse,
    PackageLoadResponse,
    PackageUpdateRequest,
    PagedConstructListRequestOptions,
    PermissionListResponse,
    Raw,
    Role,
    RoleCreateRequest,
    RoleCreateResponse,
    RoleListOptions,
    RoleListResponse,
    RoleLoadResponse,
    RoleUpdateRequest,
    RoleUpdateResponse,
    Schedule,
    ScheduleCreateReqeust,
    ScheduleListOptions,
    ScheduleListResponse,
    ScheduleResponse,
    ScheduleSaveReqeust,
    ScheduleTask,
    ScheduleTaskCreateRequest,
    ScheduleTaskListOptions,
    ScheduleTaskListResponse,
    ScheduleTaskResponse,
    ScheduleTaskSaveRequest,
    TagType,
    Template,
    TemplateCreateRequest,
    TemplateListRequest,
    TemplateListResponse,
    TemplateResponse,
    TemplateSaveOptions,
    TemplateSaveRequest,
    TemplateSaveResponse,
    User,
    UserListOptions,
    UserListResponse,
    UserResponse,
    UserUpdateRequest,
    UserUpdateResponse,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import {
    ConstructCategoryDetailTabs,
    ConstructDetailTabs,
    ContentPackageDetailTabs,
    ContentRepositoryDetailTabs,
    ContentRepositoryFragmentDetailTabs,
    DataSourceDetailTabs,
    DevToolPackageDetailTabs,
    FolderDetailTabs,
    GroupDetailTabs,
    LanguageDetailTabs,
    NodeDetailTabs,
    ObjectPropertyCategoryDetailTabs,
    ObjectPropertyDetailTabs,
    RoleDetailTabs,
    ScheduleDetailTabs,
    ScheduleTaskDetailTabs,
    TemplateDetailTabs,
    UserDetailTabs,
} from './detail-tabs';
import {
    ConstructBO,
    ConstructCategoryBO,
    ContentPackageBO,
    ContentRepositoryBO,
    ContentRepositoryFragmentBO,
    DataSourceBO,
    DevToolPackageBO,
    FolderBO,
    GroupBO,
    LanguageBO,
    NodeBO,
    ObjectPropertyBO,
    ObjectPropertyCategoryBO,
    RoleBO,
    ScheduleBO,
    ScheduleTaskBO,
    TemplateBO,
    UserBO,
} from './business-objects';

export enum EditableEntity {
    CONSTRUCT = 'construct',
    CONSTRUCT_CATEGORY = 'construct-category',
    CONTENT_PACKAGE = 'content-package',
    CONTENT_REPOSITORY = 'content-repository',
    CR_FRAGMENT = 'cr-fragment',
    DATA_SOURCE = 'data-source',
    DEV_TOOL_PACKAGE = 'devtool-package',
    FOLDER = 'folder',
    GROUP = 'group',
    LANGUAGE = 'language',
    NODE = 'node',
    OBJECT_PROPERTY = 'object-property',
    OBJECT_PROPERTY_CATEGORY = 'object-property-category',
    ROLE = 'role',
    SCHEDULE = 'schedule',
    SCHEDULE_TASK = 'task',
    TEMPLATE = 'template',
    USER = 'user',
}

export const ROUTE_ENTITY_RESOLVER_KEY = Symbol('route-entity');
export const ROUTE_ENTITY_TYPE_KEY = Symbol('route-entity-type');
export const ROUTE_PARAM_ENTITY_ID = 'id';
export const ROUTE_PARAM_NODE_ID = 'nodeId';
export const ROUTE_IS_EDITOR_ROUTE = Symbol('is-editor');
export const ROUTE_ENTITY_LOADED = Symbol('route-entity-loaded');
export const ROUTE_DETAIL_OUTLET = 'detail';
export const ROUTE_MESH_BROWSER_OUTLET = 'mesh-browser-list';

export enum EntityModelType {
    CREATE_REQUEST_MODEL = 'create-request',
    CREATE_REQUEST_PARAMS = 'create-params',
    CREATE_RESPONSE_MODEL = 'create-response',
    LOAD_REQUEST_PARAMS = 'load-params',
    LOAD_RESPONSE_MODEL = 'load-response',
    UPDATE_REQUEST_MODEL = 'update-request',
    UPDATE_REQUEST_PARAMS = 'update-params',
    UPDATE_RESPONSE_MODEL = 'update-response',
    DELETE_REQUEST_MODEL = 'delete-request',
    DELETE_REQUEST_PARAMS = 'delete-params',
    LIST_REQUEST_MODEL = 'list-request',
    LIST_REQUEST_PARAMS = 'list-params',
    LIST_RESPONSE_MODEL = 'list-response',
    DEV_TOOL_LIST_REQUEST_MODEL = 'dev-tool-list-request',
    DEV_TOOL_LIST_REQUEST_PARAMS = 'dev-tool-list-params',
    DEV_TOOL_LIST_RESPONSE_MODEL = 'dev-tool-list-response',
}

export type EditableEntityModels = {
    [EditableEntity.CONSTRUCT]: TagType<Raw>,
    [EditableEntity.CONSTRUCT_CATEGORY]: ConstructCategory<Raw>,
    [EditableEntity.CONTENT_PACKAGE]: ContentPackage<Raw>,
    [EditableEntity.CONTENT_REPOSITORY]: ContentRepository<Raw>,
    [EditableEntity.CR_FRAGMENT]: ContentRepositoryFragment<Raw>,
    [EditableEntity.DATA_SOURCE]: DataSource<Raw>,
    [EditableEntity.DEV_TOOL_PACKAGE]: Package<Raw>,
    [EditableEntity.FOLDER]: Folder<Raw>,
    [EditableEntity.GROUP]: Group<Raw>,
    [EditableEntity.LANGUAGE]: Language,
    [EditableEntity.NODE]: Node<Raw>,
    [EditableEntity.OBJECT_PROPERTY]: ObjectProperty<Raw>,
    [EditableEntity.OBJECT_PROPERTY_CATEGORY]: ObjectPropertyCategory<Raw>,
    [EditableEntity.ROLE]: Role<Raw>,
    [EditableEntity.SCHEDULE]: Schedule<Raw>,
    [EditableEntity.SCHEDULE_TASK]: ScheduleTask<Raw>,
    [EditableEntity.TEMPLATE]: Template<Raw>,
    [EditableEntity.USER]: User<Raw>,
}

export type EditableEntityBusinessObjects = {
    [EditableEntity.CONSTRUCT]: ConstructBO,
    [EditableEntity.CONSTRUCT_CATEGORY]: ConstructCategoryBO,
    [EditableEntity.CONTENT_PACKAGE]: ContentPackageBO,
    [EditableEntity.CONTENT_REPOSITORY]: ContentRepositoryBO,
    [EditableEntity.CR_FRAGMENT]: ContentRepositoryFragmentBO,
    [EditableEntity.DATA_SOURCE]: DataSourceBO,
    [EditableEntity.DEV_TOOL_PACKAGE]: DevToolPackageBO,
    [EditableEntity.FOLDER]: FolderBO,
    [EditableEntity.GROUP]: GroupBO,
    [EditableEntity.LANGUAGE]: LanguageBO,
    [EditableEntity.NODE]: NodeBO,
    [EditableEntity.OBJECT_PROPERTY]: ObjectPropertyBO,
    [EditableEntity.OBJECT_PROPERTY_CATEGORY]: ObjectPropertyCategoryBO,
    [EditableEntity.ROLE]: RoleBO,
    [EditableEntity.SCHEDULE]: ScheduleBO,
    [EditableEntity.SCHEDULE_TASK]: ScheduleTaskBO,
    [EditableEntity.TEMPLATE]: TemplateBO,
    [EditableEntity.USER]: UserBO,
}

export type EditableEntityAPIModels = {
    [EditableEntity.CONSTRUCT]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ConstructCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: ConstructCreateOptions,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ConstructCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ConstructLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ConstructUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ConstructUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: PagedConstructListRequestOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: PermissionListResponse<TagType>,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: PagedConstructListRequestOptions,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: DevToolsConstructListResponse,
    },
    [EditableEntity.CONSTRUCT_CATEGORY]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ConstructCategoryCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ConstructCategoryCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ConstructCategoryLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ConstructCategoryUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ConstructCategoryUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ConstructCategoryListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ConstructCategoryListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.CONTENT_PACKAGE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ContentPackageCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ContentPackageResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ContentPackageResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ContentPackageSaveRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ContentPackageResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ContentPackageListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ContentPackageListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.CONTENT_REPOSITORY]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ContentRepositoryCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ContentRepositoryCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ContentRepositoryResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ContentRepositoryUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ContentRepositoryUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ContentRepositoryListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ContentRepositoryListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: ContentRepositoryListOptions,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: ContentRepositoryListResponse,
    },
    [EditableEntity.CR_FRAGMENT]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ContentRepositoryFragmentCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ContentRepositoryFragmentResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ContentRepositoryFragmentResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ContentRepositoryUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ContentRepositoryFragmentResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ContentRepositoryFragmentListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ContentRepositoryFragmentListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: ContentRepositoryFragmentListOptions,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: ContentRepositoryFragmentListResponse,
    },
    [EditableEntity.DATA_SOURCE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: DataSourceCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: DataSourceCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: DataSourceLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: DataSourceUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: DataSourceUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: DataSourceListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: DataSourceListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: DataSourceListOptions,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: DataSourceListResponse,
    },
    [EditableEntity.DEV_TOOL_PACKAGE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: PackageCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: PackageCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: PackageLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: PackageUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: PackageListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: PackageListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.FOLDER]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: FolderCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: FolderCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: FolderResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: FolderSaveRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: FolderSaveRequestOptions,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: FolderResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: FolderListRequest,
        [EntityModelType.LIST_REQUEST_PARAMS]: FolderListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: FolderListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.GROUP]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: GroupCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: GroupCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: GroupResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: GroupUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: GroupResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: GroupListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: GroupListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.LANGUAGE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: LanguageCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: LanguageResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: LanguageResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: LanguageUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: LanguageResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: LanguageListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: LanguageListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.NODE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: NodeCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: NodeResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: NodeResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: NodeSaveRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: NodeSaveRequestOptions,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: NodeResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: NodeDeleteRequestOptions,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: NodeListRequestOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: NodeListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.OBJECT_PROPERTY]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ObjectPropertyCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ObjectPropertyCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ObjectPropertyLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ObjectPropertyUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ObjectPropertyUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ObjectPropertyListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ObjectPropertyListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: ObjectPropertyListOptions,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: ObjectPropertyListResponse,
    },
    [EditableEntity.OBJECT_PROPERTY_CATEGORY]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ObjectPropertyCategoryCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ObjectPropertyCategoryCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ObjectPropertyCategoryLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ObjectPropertyCategoryUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ObjectPropertyCategoryCreateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ObjectPropertyCategoryListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ObjectPropertyCategoryListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.ROLE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: RoleCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: RoleCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: RoleLoadResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: RoleUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: RoleUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: RoleListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: RoleListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.SCHEDULE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ScheduleCreateReqeust,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ScheduleResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ScheduleResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ScheduleSaveReqeust,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ScheduleResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ScheduleListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ScheduleListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.SCHEDULE_TASK]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: ScheduleTaskCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: ScheduleTaskResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: ScheduleTaskResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: ScheduleTaskSaveRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: ScheduleTaskResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: ScheduleTaskListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: ScheduleTaskListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
    [EditableEntity.TEMPLATE]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: TemplateCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: TemplateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: TemplateResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: TemplateSaveRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: TemplateSaveOptions,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: TemplateSaveResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: TemplateListRequest,
        [EntityModelType.LIST_RESPONSE_MODEL]: TemplateListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: TemplateListRequest,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: ListResponse<Template>,
    },
    [EditableEntity.USER]: {
        [EntityModelType.CREATE_REQUEST_MODEL]: GroupUserCreateRequest,
        [EntityModelType.CREATE_REQUEST_PARAMS]: never,
        [EntityModelType.CREATE_RESPONSE_MODEL]: GroupUserCreateResponse,
        [EntityModelType.LOAD_REQUEST_PARAMS]: never,
        [EntityModelType.LOAD_RESPONSE_MODEL]: UserResponse,
        [EntityModelType.UPDATE_REQUEST_MODEL]: UserUpdateRequest,
        [EntityModelType.UPDATE_REQUEST_PARAMS]: never,
        [EntityModelType.UPDATE_RESPONSE_MODEL]: UserUpdateResponse,
        [EntityModelType.DELETE_REQUEST_MODEL]: never,
        [EntityModelType.DELETE_REQUEST_PARAMS]: never,
        [EntityModelType.LIST_REQUEST_MODEL]: never,
        [EntityModelType.LIST_REQUEST_PARAMS]: UserListOptions,
        [EntityModelType.LIST_RESPONSE_MODEL]: UserListResponse,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL]: never,
        [EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS]: never,
        [EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL]: never,
    },
};

export type EditableEntityDetailTabs = {
    [EditableEntity.CONSTRUCT]: ConstructDetailTabs,
    [EditableEntity.CONSTRUCT_CATEGORY]: ConstructCategoryDetailTabs,
    [EditableEntity.CONTENT_PACKAGE]: ContentPackageDetailTabs,
    [EditableEntity.CONTENT_REPOSITORY]: ContentRepositoryDetailTabs,
    [EditableEntity.CR_FRAGMENT]: ContentRepositoryFragmentDetailTabs,
    [EditableEntity.DATA_SOURCE]: DataSourceDetailTabs,
    [EditableEntity.DEV_TOOL_PACKAGE]: DevToolPackageDetailTabs,
    [EditableEntity.FOLDER]: FolderDetailTabs,
    [EditableEntity.GROUP]: GroupDetailTabs,
    [EditableEntity.LANGUAGE]: LanguageDetailTabs,
    [EditableEntity.NODE]: NodeDetailTabs,
    [EditableEntity.OBJECT_PROPERTY]: ObjectPropertyDetailTabs,
    [EditableEntity.OBJECT_PROPERTY_CATEGORY]: ObjectPropertyCategoryDetailTabs,
    [EditableEntity.ROLE]: RoleDetailTabs,
    [EditableEntity.SCHEDULE]: ScheduleDetailTabs,
    [EditableEntity.SCHEDULE_TASK]: ScheduleTaskDetailTabs,
    [EditableEntity.TEMPLATE]: TemplateDetailTabs,
    [EditableEntity.USER]: UserDetailTabs,
}

export const EDITABLE_ENTITY_DETAIL_TABS = {
    [EditableEntity.CONSTRUCT]: ConstructDetailTabs,
    [EditableEntity.CONSTRUCT_CATEGORY]: ConstructCategoryDetailTabs,
    [EditableEntity.CONTENT_PACKAGE]: ContentPackageDetailTabs,
    [EditableEntity.CONTENT_REPOSITORY]: ContentRepositoryDetailTabs,
    [EditableEntity.CR_FRAGMENT]: ContentRepositoryFragmentDetailTabs,
    [EditableEntity.DATA_SOURCE]: DataSourceDetailTabs,
    [EditableEntity.DEV_TOOL_PACKAGE]: DevToolPackageDetailTabs,
    [EditableEntity.FOLDER]: FolderDetailTabs,
    [EditableEntity.GROUP]: GroupDetailTabs,
    [EditableEntity.LANGUAGE]: LanguageDetailTabs,
    [EditableEntity.NODE]: NodeDetailTabs,
    [EditableEntity.OBJECT_PROPERTY]: ObjectPropertyDetailTabs,
    [EditableEntity.OBJECT_PROPERTY_CATEGORY]: ObjectPropertyCategoryDetailTabs,
    [EditableEntity.ROLE]: RoleDetailTabs,
    [EditableEntity.SCHEDULE]: ScheduleDetailTabs,
    [EditableEntity.SCHEDULE_TASK]: ScheduleTaskDetailTabs,
    [EditableEntity.TEMPLATE]: TemplateDetailTabs,
    [EditableEntity.USER]: UserDetailTabs,
};

export type EntityCreateRequestModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.CREATE_REQUEST_MODEL];
export type EntityCreateRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.CREATE_REQUEST_PARAMS];
export type EntityCreateResponseModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.CREATE_RESPONSE_MODEL];

export type EntityLoadRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.LOAD_REQUEST_PARAMS];
export type EntityLoadResponseModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.LOAD_RESPONSE_MODEL];

export type EntityUpdateRequestModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.UPDATE_REQUEST_MODEL];
export type EntityUpdateRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.UPDATE_REQUEST_PARAMS];
export type EntityUpdateResponseModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.UPDATE_RESPONSE_MODEL];

export type EntityDeleteRequestModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.DELETE_REQUEST_MODEL];
export type EntityDeleteRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.DELETE_REQUEST_PARAMS];

export type EntityListRequestModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.LIST_REQUEST_MODEL];
export type EntityListRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.LIST_REQUEST_PARAMS];
export type EntityListResponseModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.LIST_RESPONSE_MODEL];

export type DevToolEntityListRequestModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.DEV_TOOL_LIST_REQUEST_MODEL];
export type DevToolEntityListRequestParams<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.DEV_TOOL_LIST_REQUEST_PARAMS];
export type DevToolEntityListResponseModel<T extends EditableEntity> = EditableEntityAPIModels[T][EntityModelType.DEV_TOOL_LIST_RESPONSE_MODEL];

export interface EntityEditorHandler<K extends EditableEntity> {

    displayName(entity: EditableEntityModels[K]): string;
    mapToBusinessObject(entity: EditableEntityModels[K], index?: number, context?: any): EditableEntityBusinessObjects[K];

    create(data: EntityCreateRequestModel<K>, params?: EntityCreateRequestParams<K>): Observable<EntityCreateResponseModel<K>>;
    createMapped(data: EntityCreateRequestModel<K>, params?: EntityCreateRequestParams<K>): Observable<EditableEntityBusinessObjects[K]>;

    get(id: string | number, options?: EntityLoadRequestParams<K>): Observable<EntityLoadResponseModel<K>>;
    getMapped(id: string | number, options?: EntityLoadRequestParams<K>): Observable<EditableEntityBusinessObjects[K]>;

    update(id: string | number, data: EntityUpdateRequestModel<K>, options?: EntityUpdateRequestParams<K>): Observable<EntityUpdateResponseModel<K>>;
    updateMapped(id: string | number, data: EntityUpdateRequestModel<K>, options?: EntityUpdateRequestParams<K>): Observable<EditableEntityBusinessObjects[K]>;

    delete(id: string | number, data?: EntityDeleteRequestModel<K>, options?: EntityDeleteRequestModel<K>): Observable<void>;
}

export interface EntityList<T> {
    items: T[];
    totalItems?: number;
}

export interface EntityListHandler<K extends EditableEntity> {
    list(body?: EntityListRequestModel<K>, params?: EntityListRequestParams<K>): Observable<EntityListResponseModel<K>>;
    listMapped(body?: EntityListRequestModel<K>, params?: EntityListRequestParams<K>): Observable<EntityList<EditableEntityBusinessObjects[K]>>;
}

export interface DevToolEntityHandler<K extends EditableEntity> {
    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void>;
    removeFromDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void>;

    listFromDevTool(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<K>,
        params?: DevToolEntityListRequestParams<K>,
    ): Observable<DevToolEntityListResponseModel<K>>;
    listFromDevToolMapped(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<K>,
        params?: DevToolEntityListRequestParams<K>,
    ): Observable<EntityList<EditableEntityBusinessObjects[K]>>;
}