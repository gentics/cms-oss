import {
    BranchCreateRequest,
    BranchListOptions,
    BranchListResponse,
    BranchResponse,
    BranchUpdateRequest,
    ClusterConfigResponse,
    ClusterStatusResponse,
    CoordinatorConfig,
    CoordinatorMasterResponse,
    GenericMessageResponse,
    GraphQLOptions,
    GraphQLRequest,
    GraphQLResponse,
    GroupCreateRequest,
    GroupListOptions,
    GroupListResponse,
    GroupResponse,
    GroupUpdateRequest,
    LocalConfigModel,
    LoginResponse,
    MicroschemaCreateRequest,
    MicroschemaListOptions,
    MicroschemaListResponse,
    MicroschemaResponse,
    MicroschemaUpdateRequest,
    NodeCreateRequest,
    NodeDeleteOptions,
    NodeListOptions,
    NodeListResponse,
    NodeResponse,
    NodeUpdateRequest,
    NodeVersionsResponse,
    PluginListResponse,
    ProjectCreateRequest,
    ProjectListOptions,
    ProjectListResponse,
    ProjectResponse,
    ProjectUpdateRequest,
    PublishOptions,
    PublishStatusResponse,
    RoleCreateRequest,
    RoleListOptions,
    RoleListResponse,
    RoleResponse,
    RoleUpdateRequest,
    SchemaChanges,
    SchemaCreateRequest,
    SchemaListOptions,
    SchemaListResponse,
    SchemaResponse,
    SchemaUpdateRequest,
    ServerInfoModel,
    StatusResponse,
    TagListResponse,
    TagListUpdateRequest,
    UserAPITokenResponse,
    UserCreateRequest,
    UserListOptions,
    UserListResponse,
    UserResponse,
    UserUpdateRequest,
} from '@gentics/mesh-models';

export interface MeshClientDriver {
    performJsonRequest(
        method: RequestMethod,
        url: string,
        headers?: Record<string, string>,
        body?: null | string,
    ): Promise<Record<string, any>>;
}

export interface MeshClientConnection {
    ssl?: boolean;
    host: string;
    port?: number;
    basePath?: string;
    version?: MeshAPIVersion;
}

export enum RequestMethod {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
    DELETE = 'DELETE',
}

export enum MeshAPIVersion {
    V1 = 'v1',
    V2 = 'v2',
}

export interface MeshAuthAPI {
    login(username: string, password: string): Promise<LoginResponse>;
    me(): Promise<UserResponse>;
}

export interface MeshUserAPI {
    list(params?: UserListOptions): Promise<UserListResponse>;
    create(body: UserCreateRequest): Promise<UserResponse>;
    get(uuid: string): Promise<UserResponse>;
    update(uuid: string, body: UserUpdateRequest): Promise<UserResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;

    createAPIToken(uuid: string): Promise<UserAPITokenResponse>;
}

export interface MeshRoleAPI {
    list(params?: RoleListOptions): Promise<RoleListResponse>;
    create(body: RoleCreateRequest): Promise<RoleResponse>;
    get(uuid: string): Promise<RoleResponse>;
    update(uuid: string, body: RoleUpdateRequest): Promise<RoleResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;
}

export interface MeshGroupAPI {
    list(params?: GroupListOptions): Promise<GroupListResponse>;
    create(body: GroupCreateRequest): Promise<GroupResponse>;
    get(uuid: string): Promise<GroupResponse>;
    update(uuid: string, body: GroupUpdateRequest): Promise<GroupResponse>;
    delete(uuid: string): Promise<GenericMessageResponse>;

    getRoles(uuid: string, params?: RoleListOptions): Promise<RoleListResponse>;
    assignRole(uuid: string, roleUuid: string): Promise<GroupResponse>;
    unassignRole(uuid: string, roleUuid: string): Promise<void>;

    getUsers(uuid: string, params?: UserListOptions): Promise<UserListResponse>;
    assignUser(uuid: string, userUuid: string): Promise<GroupResponse>;
    unassignUser(uuid: string, userUuid: string): Promise<void>;
}

export interface MeshProjectAPI {
    list(params?: ProjectListOptions): Promise<ProjectListResponse>;
    create(body: ProjectCreateRequest): Promise<ProjectResponse>;
    get(uuidOrName: string): Promise<ProjectResponse>;
    update(uuidOrName: string, body: ProjectUpdateRequest): Promise<ProjectResponse>;
    delete(uuidOrName: string): Promise<GenericMessageResponse>;
}

export interface MeshSchemaAPI {
    list(params?: SchemaListOptions): Promise<SchemaListResponse>;
    create(body: SchemaCreateRequest): Promise<SchemaResponse>;
    get(uuid: string): Promise<SchemaResponse>;
    update(uuid: string, body: SchemaUpdateRequest): Promise<SchemaResponse>;
    delete(uuid: string): Promise<void>;

    diff(uuid: string, body: SchemaUpdateRequest): Promise<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): Promise<GenericMessageResponse>;
}

export interface MeshMicroschemaAPI {
    list(params?: MicroschemaListOptions): Promise<MicroschemaListResponse>;
    create(body: MicroschemaCreateRequest): Promise<MicroschemaResponse>;
    get(uuid: string): Promise<MicroschemaResponse>;
    update(uuid: string, body: MicroschemaUpdateRequest): Promise<MicroschemaResponse>;
    delete(uuid: string): Promise<void>;

    diff(uuid: string, body: MicroschemaUpdateRequest): Promise<SchemaChanges>;
    changes(uuid: string, body: SchemaChanges): Promise<GenericMessageResponse>;
}

export interface MeshNodeAPI {
    list(project: string, params?: NodeListOptions): Promise<NodeListResponse>;
    create(project: string, body: NodeCreateRequest): Promise<NodeResponse>;
    get(project: string, uuid: string): Promise<NodeResponse>;
    update(project: string, uuid: string, body: NodeUpdateRequest): Promise<NodeResponse>;
    delete(project: string, uuid: string, params?: NodeDeleteOptions): Promise<GenericMessageResponse>;

    deleteLanguage(project: string, uuid: string, language: string): Promise<GenericMessageResponse>;
    children(project: string, uuid: string, params?: NodeListOptions): Promise<NodeListResponse>;
    versions(project: string, uuid: string): Promise<NodeVersionsResponse>;

    publishStatus(project: string, uuid: string, language?: string): Promise<PublishStatusResponse>;
    publish(project: string, uuid: string, language?: string, params?: PublishOptions): Promise<PublishStatusResponse>;
    unpublish(project: string, uuid: string, language?: string): Promise<GenericMessageResponse>;

    listTags(project: string, uuid: string): Promise<TagListResponse>;
    setTags(project: string, uuid: string, tags: TagListUpdateRequest): Promise<TagListResponse>;
    assignTag(project: string, uuid: string, tag: string): Promise<NodeResponse>;
    removeTag(project: string, uuid: string, tag: string): Promise<GenericMessageResponse>;
}

export interface MeshBranchAPI {
    list(project: string, params?: BranchListOptions): Promise<BranchListResponse>;
    create(project: string, body: BranchCreateRequest): Promise<BranchResponse>;
    get(project: string, uuid: string): Promise<BranchResponse>;
    update(project: string, uuid: string, body: BranchUpdateRequest): Promise<BranchResponse>;
    asLatest(project: string, uuid: string): Promise<BranchResponse>;
}

export interface MeshProjectSchemaAPI {
    list(project: string, params?: SchemaListOptions): Promise<SchemaListResponse>;
    get(project: string, uuid: string): Promise<SchemaResponse>;
    assign(project: string, uuid: string): Promise<SchemaResponse>;
    unassign(project: string, uuid: string): Promise<GenericMessageResponse>;
}

export interface MeshProjectMicroschemaAPI {
    list(project: string, params?: MicroschemaListOptions): Promise<MicroschemaListResponse>;
    get(project: string, uuid: string): Promise<MicroschemaResponse>;
    assign(project: string, uuid: string): Promise<MicroschemaResponse>;
    unassign(project: string, uuid: string): Promise<GenericMessageResponse>;
}

export interface MeshServerAPI {
    info(): Promise<ServerInfoModel>;
    config(): Promise<LocalConfigModel>;
    status(): Promise<StatusResponse>;
}

export interface MeshCoordinatorAPI {
    config(): Promise<CoordinatorConfig>;
    master(): Promise<CoordinatorMasterResponse>;
}

export interface MeshClusterAPI {
    config(): Promise<ClusterConfigResponse>;
    status(): Promise<ClusterStatusResponse>;
}

export interface MeshPluginAPI {
    list(): Promise<PluginListResponse>;
}

export type MeshGraphQLAPI = (project: string, body: GraphQLRequest, params?: GraphQLOptions) => Promise<GraphQLResponse>;
