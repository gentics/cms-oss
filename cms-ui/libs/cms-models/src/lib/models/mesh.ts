// Auto-generated from the RAML for Version 1.4.2 of the Gentics Mesh REST API.

export type Integer = number;

export interface MeshBinaryFieldTransformRequest {
    /** Crop mode. To be used in conjunction with cropRect */
    cropMode?: string;
    /** Crop area. */
    cropRect?: MeshImageRect;
    /** Optional new focal point for the transformed image. */
    focalPoint?: MeshFocalPoint;
    /** New height of the image. */
    height?: Integer;
    /**
     * ISO 639-1 language tag of the node which provides the image which should be
     * transformed.
     */
    language: string;
    /** Resize mode. */
    resizeMode?: string;
    /**
     * Version number which must be provided in order to handle and detect concurrent
     * changes to the node content.
     */
    version: string;
    /** New width of the image. */
    width?: Integer;
}

export interface MeshBranchCreateRequest {
    /**
     * Optional reference to the base branch. If not set, the new branch will be based
     * on the current 'latest' branch.
     */
    baseBranch?: MeshBranchReference;
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /** Whether the new branch will be set as 'latest' branch. Defaults to 'true'. */
    latest?: boolean;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
}

export interface MeshBranchInfoMicroschemaList {
    /** List of microschema references. */
    microschemas?: MeshBranchMicroschemaInfo[];
}

export interface MeshBranchInfoSchemaList {
    /** List of schema references. */
    schemas?: MeshBranchSchemaInfo[];
}

export interface MeshBranchListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshBranchResponse[];
}

export interface MeshBranchMicroschemaInfo {
    /** Uuid of the migration job. */
    jobUuid?: string;
    /**
     * Status of the migration which was triggered when the schema/microschema was added
     * to the branch.
     */
    migrationStatus?: string;
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
    /** The version of the microschema. */
    version: string;
    versionUuid?: string;
}

/**
 * Optional reference to the base branch. If not set, the new branch will be based
 * on the current 'latest' branch.
 */
export interface MeshBranchReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshBranchResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /**
     * Flag which indicates whether this is the latest branch. Requests that do not
     * specify a specific branch will be performed in the scope of the latest branch.
     */
    latest: boolean;
    /**
     * Flag which indicates whether any active node migration for this branch is still
     * running or whether all nodes have been migrated to this branch.
     */
    migrated: boolean;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
    /** List of tags that were used to tag the branch. */
    tags: MeshTagReference[];
    /** Uuid of the element */
    uuid: string;
}

export interface MeshBranchSchemaInfo {
    /** Uuid of the migration job. */
    jobUuid?: string;
    /**
     * Status of the migration which was triggered when the schema/microschema was added
     * to the branch.
     */
    migrationStatus?: string;
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
    /** The version of the microschema. */
    version: string;
    versionUuid?: string;
}

export interface MeshBranchUpdateRequest {
    /**
     * The hostname of the branch which will be used to generate links across multiple
     * projects.
     */
    hostname?: string;
    /** Name of the branch. */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * SSL flag of the branch which will be used to generate links across multiple
     * projects.
     */
    ssl?: boolean;
}

export interface MeshClusterInstanceInfo {
    address?: string;
    name?: string;
    role?: string;
    startDate?: string;
    status?: string;
}

export interface MeshClusterStatusResponse {
    instances?: MeshClusterInstanceInfo[];
}

export interface MeshConsistencyCheckResponse {
    /** List of found inconsistencies. */
    inconsistencies: MeshInconsistencyInfo[];
    /**
     * Flag which indicates whether the output was truncated because more than 250 have
     * been found.
     */
    outputTruncated: boolean;
    /** Counter for repair operations */
    repairCount: { [key: string]: Integer };
    /** Result of the consistency check. */
    result: string;
}

export interface MeshEntityMetrics {
    delete?: MeshTypeMetrics;
    insert?: MeshTypeMetrics;
    update?: MeshTypeMetrics;
}

export interface MeshErrorLocation {
    /** Error column number. */
    column: Integer;
    /** Error line number. */
    line: Integer;
}

/**
 * New node reference of the user. This can also explicitly set to null in order to
 * remove the assigned node from the user
 */
export interface MeshExpandableNode {
    uuid?: string;
}

/** Dynamic map with fields of the node language specific content. */
export interface MeshFieldMap {
    empty?: boolean;
}

export interface MeshFieldSchema {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** Label of the field. */
    label?: string;
    /** Name of the field. */
    name: string;
    required?: boolean;
    /** Type of the field. */
    type: string;
}

/** Optional new focal point for the transformed image. */
export interface MeshFocalPoint {
    /**
     * The horizontal position of the focal point. The value is a factor of the image
     * width. The value 0.5 is the center of the image.
     */
    x?: number;
    /**
     * The vertical position of the focal point. The value is a factor of the image
     * height. The value 0.5 is the center of the image.
     */
    y?: number;
}

export interface MeshGenericMessageResponse {
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

export interface MeshGraphQLError {
    /** Mesh element id which is related to the error. */
    elementId?: string;
    /** Mesh element type which is related to the error. */
    elementType?: string;
    /** List of locations which are related to the error. */
    locations?: MeshErrorLocation[];
    /** The error message. */
    message: string;
    /** Type of the error. */
    type: string;
}

export interface MeshGraphQLRequest {
    /** GraphQL operation name. */
    operationName?: string;
    /** The actual GraphQL query. */
    query: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    variables?: MeshJsonObject;
}

export interface MeshGraphQLResponse {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    data?: MeshJsonObject;
    /** Array of errors which were encoutered when handling the query. */
    errors?: MeshGraphQLError[];
}

export interface MeshGroupCreateRequest {
    /** Name of the group. */
    name: string;
}

export interface MeshGroupListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshGroupResponse[];
}

export interface MeshGroupReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshGroupResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** Name of the group */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** List of role references */
    roles: MeshRoleReference[];
    /** Uuid of the element */
    uuid: string;
}

export interface MeshGroupUpdateRequest {
    /** New name of the group */
    name: string;
}

/** Crop area. */
export interface MeshImageRect {
    height?: Integer;
    startX?: Integer;
    startY?: Integer;
    width?: Integer;
}

export interface MeshInconsistencyInfo {
    /** Description of the inconsistency. */
    description: string;
    /** Uuid of the element which is related to the inconsistency. */
    elementUuid: string;
    /**
     * Repair action which will attept to fix the inconsistency. The action will only be
     * invoked when using invoking the rapair endpoint.
     */
    repairAction: string;
    /**
     * Status of the inconsistency. This will indicate whether the inconsistency could
     * be resolved via the repair action.
     */
    repaired: boolean;
    /** Level of severity of the inconsistency. */
    severity: string;
}

export interface MeshJobListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshJobResponse[];
}

export interface MeshJobResponse {
    /**
     * The completion count of the job. This indicates how many items the job has
     * processed.
     */
    completionCount: Integer;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** The detailed error information of the job. */
    errorDetail?: string;
    /** The error message of the job. */
    errorMessage?: string;
    /** Name of the Gentics Mesh instance on which the job was executed. */
    nodeName?: string;
    /** Properties of the job. */
    properties: { [key: string]: string };
    /** The start date of the job. */
    startDate: string;
    /** Migration status. */
    status: string;
    /** The stop date of the job. */
    stopDate: string;
    /** The type of the job. */
    type: string;
    /** Uuid of the element */
    uuid: string;
    /** List of warnings which were encoutered while executing the job. */
    warnings?: MeshJobWarning[];
}

export interface MeshJobWarning {
    message?: string;
    properties?: { [key: string]: string };
    type?: string;
}

/**
 * Additional search index configuration. This can be used to setup custom analyzers
 * and filters.
 */
export interface MeshJsonObject {
    empty?: boolean;
    map?: { [key: string]: any };
}

export interface MeshLocalConfigModel {
    /** If true, mutating requests to this instance are not allowed. */
    readOnly?: boolean;
}

export interface MeshLoginRequest {
    /** New password that will be set after successful login. */
    newPassword?: string;
    /** Password of the user which should be logged in. */
    password: string;
    /** Username of the user which should be logged in. */
    username: string;
}

export interface MeshMeshServerInfoModel {
    /** Database structure revision hash. */
    databaseRevision?: string;
    /** Used database implementation vendor name. */
    databaseVendor?: string;
    /** Used database implementation version. */
    databaseVersion?: string;
    /** Node name of the Gentics Mesh instance. */
    meshNodeName?: string;
    /** Gentics Mesh Version string. */
    meshVersion?: string;
    /** Used search implementation vendor name. */
    searchVendor?: string;
    /** Used search implementation version. */
    searchVersion?: string;
    /** Used Vert.x version. */
    vertxVersion?: string;
}

export interface MeshMeshStatusResponse {
    /** The current Gentics Mesh server status. */
    status: string;
}

export interface MeshMicroschemaCreateRequest {
    /** Description of the microschema */
    description?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of microschema fields */
    fields?: MeshFieldSchema[];
    /** Name of the microschema */
    name: string;
}

export interface MeshMicroschemaListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshMicroschemaResponse[];
}

export interface MeshMicroschemaResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** Description of the microschema */
    description?: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of microschema fields */
    fields: MeshFieldSchema[];
    /** Name of the microschema */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** Uuid of the element */
    uuid: string;
    /** Version of the microschema */
    version: string;
}

export interface MeshMicroschemaUpdateRequest {
    /** Description of the microschema */
    description?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of microschema fields */
    fields?: MeshFieldSchema[];
    /** Name of the microschema */
    name?: string;
    /** Version of the microschema */
    version?: string;
}

export interface MeshNavigationElement {
    /** List of further child elements of the node. */
    children?: MeshNavigationElement[];
    node?: MeshNodeResponse;
    /** Uuid of the node within this navigation element. */
    uuid?: string;
}

export interface MeshNavigationResponse {
    /** List of further child elements of the node. */
    children?: MeshNavigationElement[];
    node?: MeshNodeResponse;
    /** Uuid of the node within this navigation element. */
    uuid?: string;
}

export interface MeshNodeChildrenInfo {
    /** Count of children which utilize the schema. */
    count: Integer;
    /** Reference to the schema of the node child */
    schemaUuid: string;
}

export interface MeshNodeCreateRequest {
    /** Dynamic map with fields of the node language specific content. */
    fields: MeshFieldMap;
    /** ISO 639-1 language tag of the node content. */
    language: string;
    /** The project root node. All futher nodes are children of this node. */
    parentNode: MeshNodeReference;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: MeshSchemaReference;
    /** List of tags that should be used to tag the node. */
    tags?: MeshTagReference[];
}

export interface MeshNodeListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshNodeResponse[];
}

/** The project root node. All futher nodes are children of this node. */
export interface MeshNodeReference {
    /**
     * Optional display name of the node. A display field must be set in the schema in
     * order to populate this property.
     */
    displayName?: string;
    /**
     * Webroot path of the node. The path property will only be provided if the
     * resolveLinks query parameter has been set.
     */
    path?: string;
    /** Name of the project to which the node belongs */
    projectName: string;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: MeshSchemaReference;
    /** Uuid of the node */
    uuid: string;
}

export interface MeshNodeResponse {
    /** Map of languages for which content is available and their publish status. */
    availableLanguages: { [key: string]: MeshPublishStatusModel };
    /**
     * List of nodes which construct the breadcrumb. Note that the start node will not
     * be included in the list.
     */
    breadcrumb: MeshNodeReference[];
    /** Object which contains information about child elements. */
    childrenInfo?: { [key: string]: MeshNodeChildrenInfo };
    /**
     * Flag which indicates whether the node is a container and can contain nested
     * elements.
     */
    container: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /**
     * Display field name of the node. May not be retured if the node schema has no
     * display field.
     */
    displayField?: string;
    /**
     * Display field value of the node. May not be retured if the node schema has no
     * display field.
     */
    displayName?: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** Dynamic map with fields of the node language specific content. */
    fields: MeshFieldMap;
    /** ISO 639-1 language tag of the node content. */
    language?: string;
    /**
     * Map of webroot paths per language. This property will only be populated if the
     * resolveLinks query parameter has been set accordingly.
     */
    languagePaths?: { [key: string]: string };
    /** The project root node. All futher nodes are children of this node. */
    parentNode: MeshNodeReference;
    /**
     * Webroot path to the node content. Will only be provided if the resolveLinks query
     * parameter has been set accordingly.
     */
    path?: string;
    permissions: MeshPermissionInfo;
    /** Reference to the project of the node. */
    project: MeshProjectReference;
    rolePerms: MeshPermissionInfo;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: MeshSchemaReference;
    /** List of tags that were used to tag the node. */
    tags: MeshTagReference[];
    /** Uuid of the element */
    uuid: string;
    /** Version of the node content. */
    version: string;
}

export interface MeshNodeUpdateRequest {
    /** Dynamic map with fields of the node language specific content. */
    fields: MeshFieldMap;
    /** ISO 639-1 language tag of the node content. */
    language: string;
    /** List of tags that should be used to tag the node. */
    tags?: MeshTagReference[];
    /**
     * Version number which can be provided in order to handle and detect concurrent
     * changes to the node content.
     */
    version?: string;
}

export interface MeshNodeVersionsResponse {
    versions?: { [key: string]: MeshVersionInfo[] };
}

/** Paging information of the list result. */
export interface MeshPagingMetaInfo {
    /** Number of the current page. */
    currentPage: Integer;
    /** Number of the pages which can be found for the given per page count. */
    pageCount: Integer;
    /** Number of elements which can be included in a single page. */
    perPage: Integer;
    /** Number of all elements which could be found. */
    totalCount: Integer;
}

export interface MeshPermissionInfo {
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

export interface MeshPluginDeploymentRequest {
    /** Deployment path of the plugin which is relative to the plugin directory. */
    path: string;
}

export interface MeshPluginListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshPluginResponse[];
}

/** Manifest of the plugin */
export interface MeshPluginManifest {
    /**
     * API name of the plugin. This will be used to construct the REST API path to the
     * plugin.
     */
    apiName: string;
    /** Author of the plugin. */
    author: string;
    /** Description of the plugin. */
    description: string;
    /** Unique id of the plugin was defined by the plugin developer. */
    id: string;
    /** Inception date of the plugin. */
    inception: string;
    /** License of the plugin. */
    license: string;
    /** Human readable name of the plugin. */
    name: string;
    /** Version of the plugin. */
    version: string;
}

export interface MeshPluginResponse {
    /** Id of the plugin. */
    id: string;
    /** Manifest of the plugin */
    manifest: MeshPluginManifest;
    /** Name of the plugin. */
    name: string;
}

export interface MeshProjectCreateRequest {
    /**
     * The hostname of the project can be used to generate links across multiple
     * projects. The hostname will be stored along the initial branch of the project.
     */
    hostname?: string;
    /** Name of the project */
    name: string;
    /** Optional path prefix for webroot path and rendered links. */
    pathPrefix?: string;
    /**
     * Reference to the schema of the root node. Creating a project will also
     * automatically create the base node of the project and link the schema to the
     * initial branch  of the project.
     */
    schema: MeshSchemaReference;
    /**
     * SSL flag of the project which will be used to generate links across multiple
     * projects. The flag will be stored along the intial branch of the project.
     */
    ssl?: boolean;
}

export interface MeshProjectListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshProjectResponse[];
}

/** Reference to the project of the node. */
export interface MeshProjectReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshProjectResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** The name of the project. */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** The project root node. All futher nodes are children of this node. */
    rootNode: MeshNodeReference;
    /** Uuid of the element */
    uuid: string;
}

export interface MeshProjectUpdateRequest {
    /** New name of the project */
    name: string;
}

export interface MeshPublishStatusModel {
    /** ISO8601 formatted publish date string. */
    publishDate?: string;
    /** Flag which indicates whether the content is published. */
    published: boolean;
    /** User reference of the creator of the element. */
    publisher: MeshUserReference;
    /** Version number. */
    version: string;
}

export interface MeshPublishStatusResponse {
    /** Map of publish status entries per language */
    availableLanguages?: { [key: string]: MeshPublishStatusModel };
}

export interface MeshRoleCreateRequest {
    /** New name of the role */
    name: string;
}

export interface MeshRoleListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshRoleResponse[];
}

export interface MeshRolePermissionRequest {
    permissions: MeshPermissionInfo;
    /** Flag which indicates whether the permission update should be applied recursively. */
    recursive?: boolean;
}

export interface MeshRolePermissionResponse {
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

export interface MeshRoleReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshRoleResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** List of groups which are assigned to the role. */
    groups: MeshGroupReference[];
    /** Name of the role. */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** Uuid of the element */
    uuid: string;
}

export interface MeshRoleUpdateRequest {
    /** New name of the role */
    name: string;
}

export interface MeshSchemaChangeModel {
    /** Type of operation for this change */
    operation?: string;
    properties?: { [key: string]: any };
    /** Uuid of the change entry */
    uuid?: string;
}

export interface MeshSchemaChangesListModel {
    changes?: MeshSchemaChangeModel[];
}

export interface MeshSchemaCreateRequest {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should
     * create new versions.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container?: boolean;
    /** Description of the schema */
    description?: string;
    /** Name of the display field. */
    displayField?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of schema fields */
    fields?: MeshFieldSchema[];
    /** Name of the schema */
    name: string;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
}

export interface MeshSchemaListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshSchemaResponse[];
}

/**
 * Reference to the schema of the root node. Creating a project will also
 * automatically create the base node of the project and link the schema to the
 * initial branch  of the project.
 */
export interface MeshSchemaReference {
    name?: string;
    set?: boolean;
    uuid?: string;
    version?: string;
    versionUuid?: string;
}

export interface MeshSchemaResponse {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should be
     * automatically purged on update.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** Description of the schema. */
    description?: string;
    /** Name of the display field. */
    displayField: string;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of schema fields */
    fields: MeshFieldSchema[];
    /** Name of the schema. */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
    /** Uuid of the element */
    uuid: string;
    /** Version of the schema. */
    version: string;
}

export interface MeshSchemaUpdateRequest {
    /**
     * Auto purge flag of the schema. Controls whether contents of this schema should
     * create new versions.
     */
    autoPurge?: boolean;
    /**
     * Flag which indicates whether nodes which use this schema store additional child
     * nodes.
     */
    container?: boolean;
    /** New description of the schema. */
    description?: string;
    /** Name of the display field. */
    displayField?: string;
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    /** List of schema fields */
    fields: MeshFieldSchema[];
    /** Name of the schema. */
    name: string;
    /**
     * Name of the segment field. This field is used to construct the webroot path to
     * the node.
     */
    segmentField?: string;
    /**
     * Names of the fields which provide a compete url to the node. This property can be
     * used to define custom urls for certain nodes. The webroot API will try to locate
     * the node via it's segment field and via the specified url fields.
     */
    urlFields?: string[];
    /** Version of the schema. */
    version?: string;
}

export interface MeshSchemaValidationResponse {
    /**
     * Additional search index configuration. This can be used to setup custom analyzers
     * and filters.
     */
    elasticsearch?: MeshJsonObject;
    message?: MeshGenericMessageResponse;
    /** Status of the validation. */
    status: string;
}

export interface MeshSearchStatusResponse {
    /**
     * Flag which indicates whether Elasticsearch is available and search queries can be
     * executed.
     */
    available: boolean;
    /** Map which contains various metric values. */
    metrics?: { [key: string]: MeshEntityMetrics };
}

export interface MeshTagCreateRequest {
    /** Name of the tag which will be created. */
    name: string;
}

export interface MeshTagFamilyCreateRequest {
    /** Name of the tag family which will be created. */
    name: string;
}

export interface MeshTagFamilyListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshTagFamilyResponse[];
}

/** Reference to the tag family to which the tag belongs. */
export interface MeshTagFamilyReference {
    /** Name of the referenced element */
    name?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshTagFamilyResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** Name of the tag family. */
    name?: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** Uuid of the element */
    uuid: string;
}

export interface MeshTagFamilyUpdateRequest {
    /** New name of the tag family */
    name: string;
}

export interface MeshTagListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshTagResponse[];
}

export interface MeshTagListUpdateRequest {
    /**
     * List of tags which should be assigned to the node. Tags which are not included
     * will be removed from the node.
     */
    tags: MeshTagReference[];
}

export interface MeshTagReference {
    /** Name of the referenced element */
    name?: string;
    tagFamily?: string;
    /** Uuid of the referenced element */
    uuid: string;
}

export interface MeshTagResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** Name of the tag. */
    name: string;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** Reference to the tag family to which the tag belongs. */
    tagFamily: MeshTagFamilyReference;
    /** Uuid of the element */
    uuid: string;
}

export interface MeshTagUpdateRequest {
    /** New name of the tag. */
    name: string;
}

export interface MeshTypeMetrics {
    pending?: Integer;
    synced?: Integer;
}

export interface MeshUserAPITokenResponse {
    /** Date of the last time the API token was issued. */
    previousIssueDate: string;
    /** Issued client API token. */
    token: string;
}

export interface MeshUserCreateRequest {
    /** Email address of the user. */
    emailAddress?: string;
    /** Firstname of the user. */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange?: boolean;
    /**
     * Optional group id for the user. If provided the user will automatically be
     * assigned to the identified group.
     */
    groupUuid?: string;
    /** Lastname of the user. */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: MeshExpandableNode;
    /** Password of the new user. */
    password: string;
    /** Username of the user. */
    username: string;
}

export interface MeshUserListResponse {
    /** Paging information of the list result. */
    _metainfo: MeshPagingMetaInfo;
    /** Array which contains the found elements. */
    data: MeshUserResponse[];
}

export interface MeshUserPermissionResponse {
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

/** User reference of the creator of the element. */
export interface MeshUserReference {
    /** Firstname of the user */
    firstName?: string;
    /** Lastname of the user */
    lastName?: string;
    /** Uuid of the user */
    uuid: string;
}

export interface MeshUserResetTokenResponse {
    /** ISO8601 date of the creation date for the provided token */
    created: string;
    /** JSON Web Token which was issued by the API. */
    token: string;
}

export interface MeshUserResponse {
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** ISO8601 formatted edited date string. */
    edited: string;
    /** User reference of the creator of the element. */
    editor: MeshUserReference;
    /** Email address of the user */
    emailAddress?: string;
    /**
     * Flag which indicates whether the user is enabled or disabled. Disabled users can
     * no longer log into Gentics Mesh. Deleting a user user will not remove it. Instead
     * the user will just be disabled.
     */
    enabled: boolean;
    /** Firstname of the user. */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange: boolean;
    /** List of group references to which the user belongs. */
    groups: MeshGroupReference[];
    /** Lastname of the user. */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: MeshExpandableNode;
    permissions: MeshPermissionInfo;
    rolePerms: MeshPermissionInfo;
    /** Hashsum of user roles which can be used for user permission caching. */
    rolesHash: string;
    /** Username of the user. */
    username: string;
    /** Uuid of the element */
    uuid: string;
}

export interface MeshUserUpdateRequest {
    /** New email address of the user */
    emailAddress?: string;
    /** New firstname of the user */
    firstname?: string;
    /** When true, the user needs to change their password on the next login. */
    forcedPasswordChange?: boolean;
    /** New lastname of the user */
    lastname?: string;
    /**
     * New node reference of the user. This can also explicitly set to null in order to
     * remove the assigned node from the user
     */
    nodeReference?: MeshExpandableNode;
    oldPassword?: string;
    /** New password of the user */
    password?: string;
    /** New username of the user */
    username?: string;
}

export interface MeshVersionInfo {
    /** Is the version used as a root version in another branch? */
    branchRoot: boolean;
    /** ISO8601 formatted created date string. */
    created: string;
    /** User reference of the creator of the element. */
    creator: MeshUserReference;
    /** Is the content a draft version? */
    draft: boolean;
    /** Is the content published version? */
    published: boolean;
    /** Version of the content. */
    version: string;
}
