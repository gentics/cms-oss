import {
    AccessControlledType,
    AssignEntityToContentPackageOptions,
    BaseListOptionsWithPaging,
    CancelPageEditOptions,
    ChannelSyncRequest,
    ClusterInformationResponse,
    Construct,
    ConstructCategoryCreateRequest,
    ConstructCategoryCreateResponse,
    ConstructCategoryListOptions,
    ConstructCategoryListResponse,
    ConstructCategoryLoadResponse,
    ConstructCategorySortRequest,
    ConstructCategoryUpdateRequest,
    ConstructCategoryUpdateResponse,
    ConstructCreateOptions,
    ConstructCreateRequest,
    ConstructCreateResponse,
    ConstructLinkedNodesResponse,
    ConstructListOptions,
    ConstructListResponse,
    ConstructLoadResponse,
    ConstructNodeLinkRequest,
    ConstructNodeLinkResponse,
    ConstructUpdateRequest,
    ConstructUpdateResponse,
    ContentMaintenanceActionRequest,
    ContentPackageCreateRequest,
    ContentPackageListOptions,
    ContentPackageListResponse,
    ContentPackageResponse,
    ContentPackageSaveRequest,
    ContentPackageSyncOptions,
    ContentPackageSyncResponse,
    ContentRepositoryCreateRequest,
    ContentRepositoryCreateResponse,
    ContentRepositoryFragmentCreateRequest,
    ContentRepositoryFragmentListOptions,
    ContentRepositoryFragmentListResponse,
    ContentRepositoryListOptions,
    ContentRepositoryListResponse,
    ContentRepositoryListRolesResponse,
    ContentRepositoryResponse,
    ContentRepositoryRolesUpdateRequest,
    ContentRepositoryUpdateRequest,
    ContentRepositoryUpdateResponse,
    ContentTagCreateRequest,
    CropResizeParameters,
    DataSourceConstructListOptions,
    DataSourceConstructListResponse,
    DataSourceCreateRequest,
    DataSourceCreateResponse,
    DataSourceEntryCreateRequest,
    DataSourceEntryCreateResponse,
    DataSourceEntryListResponse,
    DataSourceEntryListUpdateRequest,
    DataSourceEntryListUpdateResponse,
    DataSourceEntryLoadResponse,
    DataSourceListOptions,
    DataSourceListResponse,
    DataSourceLoadResponse,
    DataSourceUpdateRequest,
    DataSourceUpdateResponse,
    DirtQueueListOptions,
    DirtQueueListResponse,
    DirtQueueSummary,
    ElasticSearchIndexListOptions,
    ElasticSearchIndexListResponse,
    ElasticSearchIndexRebuildOptions,
    ElasticSearchQueryResponse,
    ElasticSearchTypeSearchOptions,
    EmbeddedToolsResponse,
    FUMResult,
    FUMStatusResponse,
    Feature,
    FeatureModelListResponse,
    FeatureResponse,
    FileCopyRequest,
    FileCreateRequest,
    FileDeleteOptions,
    FileListOptions,
    FileListResponse,
    FileReplaceOptions,
    FileResponse,
    FileSaveRequest,
    FileUploadOptions,
    FileUploadResponse,
    FileUsageResponse,
    FolderBreadcrumbOptions,
    FolderCreateRequest,
    FolderCreateResponse,
    FolderDeleteOptions,
    FolderExternalLinksOptions,
    FolderExternalLinksResponse,
    FolderListOptions,
    FolderListResponse,
    FolderPublishDirSanitizeRequest,
    FolderPublishDirSanitizeResponse,
    FolderRequestOptions,
    FolderResponse,
    FolderSaveRequest,
    FolderStartpageRequest,
    FolderUsageResponse,
    Form,
    FormCreateRequest,
    FormCreateResponse,
    FormDataListOptions,
    FormDownloadInfoResponse,
    FormListOptions,
    FormListResponse,
    FormLoadOptions,
    FormPublishRequest,
    FormResponse,
    FormSaveRequest,
    FormUnpublishRequest,
    GcmsPermission,
    Group,
    GroupCreateRequest,
    GroupCreateResponse,
    GroupListOptions,
    GroupListResponse,
    GroupPermissionBitsResponse,
    GroupPermissionsListOptions,
    GroupPermissionsListResponse,
    GroupResponse,
    GroupSetPermissionsRequest,
    GroupTreeOptions,
    GroupTreeResponse,
    GroupTypeOrInstancePermissionsResponse,
    GroupUpdateRequest,
    GroupUserCreateRequest,
    GroupUserCreateResponse,
    I18nLanguageListResponse,
    I18nLanguageResponse,
    I18nLanguageSetRequest,
    I18nTranslationOptions,
    IdSetRequest,
    ImageDeleteOptions,
    ImageListResponse,
    ImageResponse,
    ImageSaveRequest,
    ImplementationHashResponse,
    InheritableItem,
    InheritanceRequest,
    InheritanceResponse,
    InheritanceStatusOptions,
    InstancePermissionsOptions,
    ItemListOptions,
    ItemListResponse,
    ItemRequestOptions,
    Language,
    LanguageCreateRequest,
    LanguageListOptions,
    LanguageListResponse,
    LanguageResponse,
    LanguageUpdateRequest,
    LinkCheckerCheckRequest,
    LinkCheckerCheckResponse,
    LinkCheckerExternalLinkList,
    LinkCheckerOptions,
    LinkCheckerPageList,
    LinkCheckerPageStatusRequest,
    LinkCheckerReplaceRequest,
    LinkCheckerUpdateResponse,
    LinkResponse,
    ListResponse,
    LocalizationInfoOptions,
    LocalizationInfoResponse,
    LocalizationsResponse,
    LocalizeRequest,
    LogActionsRequest,
    LogTypeListItem,
    LogTypesRequest,
    LoginOptions,
    LoginRequest,
    LoginResponse,
    LogsListRequest,
    LogsListResponse,
    MaintenanceModeRequestOptions,
    MaintenanceModeResponse,
    MarkupLanguageListOptions,
    MarkupLanguageListResponse,
    MessageListOptions,
    MessageListResponse,
    MessageReadRequest,
    MultiInheritanceOptions,
    MultiInheritanceStatusOptions,
    MultiLocalizationInfoOptions,
    MultiObjectLoadRequest,
    MultiObjectMoveRequest,
    MultiPageLoadRequest,
    MultiPagePublishRequest,
    MultiPushToMasterRequest,
    MultiTagCreateRequest,
    MultiTagCreateResponse,
    MultiUnlocalizeRequest,
    MultipleInheritanceResponse,
    NodeCopyRequest,
    NodeCopyRequestOptions,
    NodeCreateRequest,
    NodeFeature,
    NodeFeatureListRequestOptions,
    NodeLanguageListRequest,
    NodeLanguageOrderRequest,
    NodeListOptions,
    NodeListRequestOptions,
    NodeListResponse,
    NodeMultiLinkRequest,
    NodeResponse,
    NodeSaveRequest,
    NodeSettingsResponse,
    ObjectMoveRequest,
    ObjectPropertyCategoryCreateRequest,
    ObjectPropertyCategoryCreateResponse,
    ObjectPropertyCategoryListOptions,
    ObjectPropertyCategoryListResponse,
    ObjectPropertyCategoryLoadResponse,
    ObjectPropertyCategoryUpdateRequest,
    ObjectPropertyCategoryUpdateResponse,
    ObjectPropertyCreateRequest,
    ObjectPropertyCreateResponse,
    ObjectPropertyListOptions,
    ObjectPropertyListResponse,
    ObjectPropertyLoadResponse,
    ObjectPropertyUpdateRequest,
    ObjectPropertyUpdateResponse,
    PackageCreateResponse,
    PackageListOptions,
    PackageListResponse,
    PackageLoadResponse,
    PackageSyncOptions,
    PackageSyncResponse,
    Page,
    PageAssignRequest,
    PageCopyRequest,
    PageCopyResponse,
    PageCreateRequest,
    PageCreateResponse,
    PageDeleteOptions,
    PageListOptions,
    PageListResponse,
    PageOfflineOptions,
    PageOfflineRequest,
    PagePreviewRequest,
    PagePreviewResponse,
    PagePublishOptions,
    PagePublishRequest,
    PageRenderOptions,
    PageRenderResponse,
    PageRequestOptions,
    PageResponse,
    PageRestoreOptions,
    PageSaveRequest,
    PageTagListOptions,
    PageTagListResponse,
    PageTagRenderOptions,
    PageTranslateOptions,
    PageUsageResponse,
    PageVariantCreateRequest,
    PagedConstructListRequestOptions,
    PagedConstructListResponse,
    PartType,
    PartTypeListOptions,
    PermissionListResponse,
    PermissionResponse,
    PermissionsOptions,
    PolicyGroupResponse,
    PolicyMapOptions,
    PolicyResponse,
    PublishInfo,
    PublishLogEntry,
    PublishLogListOption,
    PublishQueue,
    PublishType,
    PushToMasterRequest,
    Raw,
    Response,
    RoleCreateRequest,
    RoleCreateResponse,
    RoleListOptions,
    RoleListResponse,
    RoleLoadResponse,
    RolePermissionsLoadResponse,
    RolePermissionsUpdateRequest,
    RolePermissionsUpdateResponse,
    RoleUpdateRequest,
    RoleUpdateResponse,
    RotateParameters,
    ScheduleCreateReqeust,
    ScheduleExecutionListOptions,
    ScheduleExecutionListResponse,
    ScheduleExecutionResponse,
    ScheduleListOptions,
    ScheduleListResponse,
    ScheduleResponse,
    ScheduleSaveReqeust,
    ScheduleTaskCreateRequest,
    ScheduleTaskListOptions,
    ScheduleTaskListResponse,
    ScheduleTaskResponse,
    ScheduleTaskSaveRequest,
    SchedulerStatusResponse,
    SchedulerSuspendRequest,
    SearchPagesOptions,
    SendMessageRequest,
    SetPermissionsOptions,
    SetPermissionsRequest,
    SinglePermissionResponse,
    StagableEntityType,
    SuggestPageFileNameRequest,
    SuggestPageFileNameResponse,
    SyncObjectsResponse,
    TagCreateResponse,
    TagRestoreOptions,
    TagStatus,
    TagmapEntryCreateRequest,
    TagmapEntryCreateResponse,
    TagmapEntryListOptions,
    TagmapEntryListResponse,
    TagmapEntryResponse,
    TagmapEntryUpdateRequest,
    TagmapEntryUpdateResponse,
    Template,
    TemplateCopyRequest,
    TemplateCreateRequest,
    TemplateLinkRequest,
    TemplateListRequest,
    TemplateListResponse,
    TemplateLoadOptions,
    TemplateMultiLinkRequest,
    TemplateResponse,
    TemplateSaveRequest,
    TemplateTagStatusResponse,
    TemplateUsageResponse,
    TotalUsageResponse,
    UnlocalizeRequest,
    UpdatesInfo,
    UsageInFilesOptions,
    UsageInFoldersOptions,
    UsageInImagesOptions,
    UsageInPagesOptions,
    UsageInSyncableObjectsOptions,
    UsageInTemplatesOptions,
    UsageInTotalOptions,
    UserDataResponse,
    UserGroupNodeRestrictionsResponse,
    UserGroupsResponse,
    UserListOptions,
    UserListResponse,
    UserRequestOptions,
    UserResponse,
    UserUpdateRequest,
    UserUpdateResponse,
    UsersnapSettingsResponse,
    ValidateSidResponse,
    VersionResponse,
    WastebinDeleteOptions,
    WastebinRestoreOptions,
} from '@gentics/cms-models';
import { LoginResponse as MeshLoginResponse } from '@gentics/mesh-models';
import { BasicAPI } from './common';

type SearchableType = 'page' | 'image' | 'file' | 'folder' | 'form';

export interface AbstractAdminAPI extends BasicAPI {
    getLogs: (options?: LogsListRequest) => LogsListResponse;
    getActionLogs: (options?: LogActionsRequest) => ListResponse<LogTypeListItem>;
    getTypeLogs: (options?: LogTypesRequest) => ListResponse<LogTypeListItem>;

    setMaintenanceMode: (body: MaintenanceModeRequestOptions) => MaintenanceModeResponse;

    getPublicKey: () => Record<string, string>;
    getPublishInfo: () => PublishInfo;
    getTools(): EmbeddedToolsResponse;
    getUpdates: () => UpdatesInfo;
    getVersion(): VersionResponse;
    getFeature(key: Feature): FeatureResponse;

    reloadConfiguration: () => Response;

    getDirtQueue: (options?: DirtQueueListOptions) => DirtQueueListResponse;
    getDirtQueueSummary: () => DirtQueueSummary;
    redoFailedDirtQueueEntry: (actionId: string | number) => void;
    deleteFailedDirtQueueEntry: (actionId: string | number) => void;

    getPublishQueue: () => PublishQueue;
    modifyPublishQueue: (body: ContentMaintenanceActionRequest) => Response;
}

export interface AbstractAuthenticationAPI extends BasicAPI {
    login: (data: LoginRequest, params?: LoginOptions) => LoginResponse;
    logout: (sid: string | number) => Response;
    validate: (sid: string | number) => ValidateSidResponse;
}

export interface AbstractClusterAPI extends BasicAPI {
    getInfo: () => ClusterInformationResponse;
    setMaster: () => ClusterInformationResponse;
}

export interface AbstractConstructAPI extends BasicAPI {
    list: (options?: PagedConstructListRequestOptions) => PagedConstructListResponse;
    create: (body: ConstructCreateRequest, params?: ConstructCreateOptions) => ConstructCreateResponse;
    get: (id: number | string) => ConstructLoadResponse;
    update: (id: number | string, body: ConstructUpdateRequest) => ConstructUpdateResponse;
    delete: (id: number | string) => void;

    hash: (id: number | string) => ImplementationHashResponse;
    getLinkedNodes: (id: number | string) => ConstructLinkedNodesResponse;
    linkToNode: (body: ConstructNodeLinkRequest) => ConstructNodeLinkResponse;
    unlinkFromNode: (body: ConstructNodeLinkRequest) => ConstructNodeLinkResponse;
}

export interface AbstractConstrctCategoryAPI extends BasicAPI {
    list: (options?: ConstructCategoryListOptions) => ConstructCategoryListResponse;
    create: (body: ConstructCategoryCreateRequest) => ConstructCategoryCreateResponse;
    get: (id: number | string) => ConstructCategoryLoadResponse;
    update: (id: number | string, body: ConstructCategoryUpdateRequest) => ConstructCategoryUpdateResponse;
    delete: (id: number | string) => void;

    sort: (body: ConstructCategorySortRequest) => ConstructCategoryListResponse;
}

export interface AbstractContentRepositoryFragmentAPI extends BasicAPI {
    list: (options?: ContentRepositoryFragmentListOptions) => ContentRepositoryFragmentListResponse;
    create: (body: ContentRepositoryFragmentCreateRequest) => ContentRepositoryCreateResponse;
    get: (id: number | string) => ConstructCategoryLoadResponse;
    update: (id: number | string, body: ContentRepositoryUpdateRequest) => ConstructCategoryUpdateResponse;
    delete: (id: number | string) => void;

    listEntries: (crFragmentId: number | string, options?: TagmapEntryListOptions) => TagmapEntryListResponse;
    createEntry: (crFragmentId: number | string, body: TagmapEntryCreateRequest) => TagmapEntryCreateResponse;
    getEntry: (crFragmentId: number | string, entryId: number | string) => TagmapEntryResponse;
    updateEntry: (crFragmentId: number | string, entryId: number | string, body: TagmapEntryUpdateRequest) => TagmapEntryUpdateResponse;
    deleteEntry: (crFragmentId: number | string, entryId: number | string) => void;
}

export interface AbstractContentRepositoryAPI extends BasicAPI {
    list: (options?: ContentRepositoryListOptions) => ContentRepositoryListResponse;
    create: (body: ContentRepositoryCreateRequest) => ContentRepositoryCreateResponse;
    get: (id: number | string) => ContentRepositoryResponse;
    update: (id: number | string, body: ContentRepositoryUpdateRequest) => ContentRepositoryUpdateResponse;
    delete: (id: number | string) => void;

    checkData: (id: number | string) => ContentRepositoryResponse;
    repairData: (id: number | string) => ContentRepositoryResponse;
    checkStructure: (id: number | string) => ContentRepositoryResponse;
    repairStructure: (id: number | string) => ContentRepositoryResponse;

    checkEntries: (crId: number | string) => TagmapEntryListResponse;
    listEntries: (crId: number | string, options?: TagmapEntryListOptions) => TagmapEntryListResponse;
    createEntry: (crId: number | string, body: TagmapEntryCreateRequest) => TagmapEntryCreateResponse;
    getEntry: (crId: number | string, entryId: number | string) => TagmapEntryResponse;
    updateEntry: (crId: number | string, entryId: number | string, body: TagmapEntryUpdateRequest) => TagmapEntryUpdateResponse;
    deleteEntry: (crId: number | string, entryId: number | string) => void;

    listFragments: (crId: number | string, options?: ContentRepositoryFragmentListOptions) => ContentRepositoryFragmentListResponse;
    linkFragment: (crId: number | string, fragmentId: number | string) => Response;
    unlinkFragment: (crId: number | string, fragmentId: number | string) => void;

    listAvailableRoles: (crId: number | string) => ContentRepositoryListRolesResponse;
    listAssignedRoles: (crId: number | string) => ContentRepositoryListRolesResponse;
    assignRoles: (crId: number | string, body: ContentRepositoryRolesUpdateRequest) => ContentRepositoryListRolesResponse;

    proxyLogin: (id: number | string) => MeshLoginResponse;
}

export interface AbstractContentStagingAPI extends BasicAPI {
    list: (options?: ContentPackageListOptions) => ContentPackageListResponse;
    create: (body: ContentPackageCreateRequest) => ContentPackageResponse;
    get: (name: string) => ContentPackageResponse;
    update: (name: string, body: ContentPackageSaveRequest) => ContentPackageResponse;
    delete: (name: string) => Response;

    export: (name: string, options?: ContentPackageSyncOptions) => ContentPackageSyncResponse;
    import: (name: string, options?: ContentPackageSyncOptions) => ContentPackageSyncResponse;
    upload: (name: string, file: File | Blob) => ContentPackageResponse;
    download: (name: string) => Blob;

    addEntity: (
        name: string,
        entityType: StagableEntityType,
        entityGlobalId: string,
        options?: AssignEntityToContentPackageOptions,
    ) => ContentPackageResponse;
    removeEntity: (
        name: string,
        entityType: StagableEntityType,
        entityGlobalId: string,
    ) => ContentPackageResponse;
}

export interface AbstractDataSourceAPI extends BasicAPI {
    list: (options?: DataSourceListOptions) => DataSourceListResponse;
    create: (body: DataSourceCreateRequest) => DataSourceCreateResponse;
    get: (id: number | string) => DataSourceLoadResponse;
    update: (id: number | string, body: DataSourceUpdateRequest) => DataSourceUpdateResponse;
    delete: (id: number | string) => void;

    hash: (id: number | string) => ImplementationHashResponse;
    listConstructs: (id: number | string, options?: DataSourceConstructListOptions) => DataSourceConstructListResponse;

    listEntries: (dsId: number | string) => DataSourceEntryListResponse;
    updateEntries: (dsId: number | string, body: DataSourceEntryListUpdateRequest) => DataSourceEntryListUpdateResponse;
    createEntry: (dsId: number | string, body: DataSourceEntryCreateRequest) => DataSourceEntryCreateResponse;
    getEntry: (dsId: number | string, entryId: number | string) => DataSourceEntryLoadResponse;
    deleteEntry: (dsId: number | string, entryId: number | string) => void;
}

export interface AbstractDevToolsAPI extends BasicAPI {
    list: (options?: PackageListOptions) => PackageListResponse;
    create: (name: string) => PackageCreateResponse;
    get: (name: string) => PackageLoadResponse;
    delete: (name: string) => void;

    syncStatus: () => PackageSyncResponse;
    startSync: () => PackageSyncResponse;
    stopSync: () => PackageSyncResponse;
    syncToFileSystem: (name: string, options?: PackageSyncOptions) => Response;
    syncFromFileSystem: (name: string, options?: PackageSyncOptions) => Response;

    preview: (uuid: string) => string;
    livePreview: (uuid: string) => string;

    listFromNodes: (nodeId: number | string, options?: PackageListOptions) => PackageListResponse;
    assignToNode: (name: string, nodeId: number | string) => Response;
    unassignFromNode: (name: string, nodeId: number | string) => Response;

    listConstructs: (name: string, options?: ConstructListOptions) => ListResponse<Construct>;
    assignConstruct: (name: string, constructId: number | string) => Response;
    unassignConstruct: (name: string, constructId: number | string) => Response;

    listContentRepositories: (name: string, options?: ContentRepositoryListOptions) => ContentRepositoryListResponse;
    assignContentRepository: (name: string, crId: number | string) => Response;
    unassignContentRepository: (name: string, crId: number | string) => Response;

    listContentRepositoryFragments: (name: string, options?: ContentRepositoryFragmentListOptions) => ContentRepositoryFragmentListResponse;
    assignContentRepositoryFragment: (name: string, crfId: number | string) => Response;
    unassignContentRepositoryFragment: (name: string, crfId: number | string) => Response;

    listDataSources: (name: string, options?: DataSourceListOptions) => DataSourceListResponse;
    assignDataSource: (name: string, dataSourceId: number | string) => Response;
    unassignDataSource: (name: string, dataSourceId: number | string) => Response;

    listObjectProperties: (name: string, options?: ObjectPropertyListOptions) => ObjectPropertyListResponse;
    assignObjectProperty: (name: string, opId: number | string) => Response;
    unassignObjectProperty: (name: string, opId: number | string) => Response;

    listTemplates: (name: string, options?: TemplateListRequest) => ListResponse<Template>;
    assignTemplate: (name: string, templateId: number | string) => Response;
    unassignTemplate: (name: string, templateId: number | string) => Response;
}

export interface AbstractElasticSearchAPI extends BasicAPI {
    search: (type: SearchableType, body: any, options?: ElasticSearchTypeSearchOptions) => ElasticSearchQueryResponse<InheritableItem<Raw>>;
}

export interface AbstractFileUploadManipulatorAPI extends BasicAPI {
    contents: (fileName: string) => any;
    result: (fileName: string, body: FUMResult) => FUMStatusResponse;
}

export interface AbstractFileAPI extends BasicAPI {
    list: (options?: FileListOptions) => FileListResponse;
    create: (body: FileCreateRequest) => FileUploadResponse;
    upload: (file: File | Blob, options: FileUploadOptions, fileName?: string) => FileUploadResponse;
    get: (id: number | string, options?: ItemRequestOptions) => FileResponse;
    getMultiple: (body: MultiObjectLoadRequest) => FileListResponse;
    update: (id: number | string, body: FileSaveRequest) => Response;
    uploadTo: (id: number | string, file: File | Blob, fileName?: string, options?: FileReplaceOptions) => Response;
    delete: (id: number | string, options?: FileDeleteOptions) => void;

    copy: (body: FileCopyRequest) => FileUploadResponse;
    move: (id: number | string, body: ObjectMoveRequest) => Response;
    moveMultiple: (body: MultiObjectMoveRequest) => Response;

    inheritanceStatus: (id: number | string, options?: InheritanceStatusOptions) => InheritanceResponse;
    multipleInheritanceStatus: (options: MultiInheritanceStatusOptions) => MultipleInheritanceResponse;
    inherit: (id: number | string, body: InheritanceRequest) => InheritanceResponse;
    inheritMultiple: (body: InheritanceRequest, options: MultiInheritanceOptions) => Response;
    pushToMaster: (id: number | string, body: PushToMasterRequest) => Response;
    pushMultipleToMaster: (body: MultiPushToMasterRequest | ChannelSyncRequest) => Response;

    localizationInfo: (id: number | string, options?: LocalizationInfoOptions) => LocalizationInfoResponse;
    multipleLocalizationInfos: (options: MultiLocalizationInfoOptions) => LocalizationInfoResponse;
    listLocalizations: (id: number | string) => LocalizationsResponse;
    localize: (id: number | string, body: LocalizeRequest) => Response;
    unlocalize: (id: number | string, body: UnlocalizeRequest) => Response;
    unlocalizeMultiple: (body: MultiUnlocalizeRequest) => Response;

    restoreFromWastebin: (id: number | string, options?: WastebinRestoreOptions) => Response;
    restoreMultipleFromWastebin: (body: IdSetRequest, options?: WastebinRestoreOptions) => Response;
    deleteFromWastebin: (id: number | string, options?: WastebinDeleteOptions) => Response;
    deleteMultipleFromWastebin: (body: IdSetRequest, options?: WastebinDeleteOptions) => Response;

    usageInFiles: (options?: UsageInFilesOptions) => FileUsageResponse;
    usageInImages: (options?: UsageInImagesOptions) => FileUsageResponse;
    usageInFolders: (options?: UsageInFoldersOptions) => FolderUsageResponse;
    usageInPages: (options?: UsageInPagesOptions) => PageUsageResponse;
    usageInTemplates: (options?: UsageInTemplatesOptions) => TemplateUsageResponse;
    usageInSyncableObject: (options: UsageInSyncableObjectsOptions) => SyncObjectsResponse;
    usageInTotal: (options?: UsageInTotalOptions) => TotalUsageResponse;
}

export interface AbstractFolderAPI extends BasicAPI {
    list: (options?: FolderListOptions) => FolderListResponse;
    create: (body: FolderCreateRequest) => FolderCreateResponse;
    get: (id: number | string, options?: FolderRequestOptions) => FolderResponse;
    getMultiple: (body: MultiObjectLoadRequest) => FolderListResponse;
    update: (id: number | string, body: FolderSaveRequest) => FolderResponse;
    delete: (id: number | string, options?: FolderDeleteOptions) => Response;

    move: (id: number | string, body: ObjectMoveRequest) => Response;
    moveMultiple: (body: MultiObjectMoveRequest) => Response;

    externalLinks: (id: number | string, options?: FolderExternalLinksOptions) => FolderExternalLinksResponse;
    breadcrumbs: (id: number | string, options?: FolderBreadcrumbOptions) => FolderListResponse;
    files: (id: number | string, options?: FolderListOptions) => FileListResponse;
    folders: (id: number | string, options?: FolderListOptions) => FolderListResponse;
    images: (id: number | string, options?: FolderListOptions) => FileListResponse;
    items: (id: number | string, options?: ItemListOptions) => ItemListResponse;
    pages: (id: number | string, options?: FolderListOptions) => PageListResponse;
    templates: (id: number | string, options?: FolderListOptions) => TemplateListResponse;

    setStartpage: (id: number | string, body: FolderStartpageRequest) => Response;
    sanitizePublshDirectory: (body: FolderPublishDirSanitizeRequest) => FolderPublishDirSanitizeResponse;

    inheritanceStatus: (id: number | string, options?: InheritanceStatusOptions) => InheritanceResponse;
    multipleInheritanceStatus: (options: MultiInheritanceStatusOptions) => MultipleInheritanceResponse;
    inherit: (id: number | string, body: InheritanceRequest) => InheritanceResponse;
    inheritMultiple: (body: InheritanceRequest, options: MultiInheritanceOptions) => Response;
    pushToMaster: (id: number | string, body: PushToMasterRequest) => Response;
    pushMultipleToMaster: (body: MultiPushToMasterRequest | ChannelSyncRequest) => Response;

    localizationInfo: (id: number | string, options?: LocalizationInfoOptions) => LocalizationInfoResponse;
    multipleLocalizationInfos: (options: MultiLocalizationInfoOptions) => LocalizationInfoResponse;
    listLocalizations: (id: number | string) => LocalizationsResponse;
    localize: (id: number | string, body: LocalizeRequest) => Response;
    unlocalize: (id: number | string, body: UnlocalizeRequest) => Response;
    unlocalizeMultiple: (body: MultiUnlocalizeRequest) => Response;

    restoreFromWastebin: (id: number | string, options?: WastebinRestoreOptions) => Response;
    restoreMultipleFromWastebin: (body: IdSetRequest, options?: WastebinRestoreOptions) => Response;
    deleteFromWastebin: (id: number | string, options?: WastebinDeleteOptions) => Response;
    deleteMultipleFromWastebin: (body: IdSetRequest, options?: WastebinDeleteOptions) => Response;

    usageInSyncableObject: (options: UsageInSyncableObjectsOptions) => SyncObjectsResponse;
}

export interface AbstractFormAPI extends BasicAPI {
    list: (options?: FormListOptions) => FormListResponse;
    create: (body: FormCreateRequest) => FormCreateResponse;
    get: (id: number | string, options?: FormLoadOptions) => FormResponse;
    update: (id: number | string, body: FormSaveRequest) => FormResponse;
    delete: (id: number | string) => Response;

    move: (id: number | string, folderId: number | string) => FormResponse;

    lock: (id: number | string) => FormResponse;
    unlock: (id: number | string) => FormResponse;
    publish: (id: number | string, options?: FormPublishRequest) => FormResponse;
    unpublish: (id: number | string, options?: FormUnpublishRequest) => FormResponse;
    removeScheduledPublish: (id: number | string) => FormResponse;
    removeScheduledUnpublish: (id: number | string) => FormResponse;

    exportStatus: (id: number | string) => FormDownloadInfoResponse;
    createExport: (id: number | string) => FormDownloadInfoResponse;
    binariesStatus: (id: number | string) => FormDownloadInfoResponse;
    createBinaries: (id: number | string) => FormDownloadInfoResponse;
    downloadData: (id: number | string, downloadUuid: string) => Blob;

    previewSaved: (id: number | string, language: string) => string;
    previewModel: (id: number | string, language: string, body: Form<Raw>) => string;

    listVersions: (id: number | string) => FormListResponse;
    getVersion: (id: number | string, version: string) => FormResponse;

    listData: (id: number | string, options?: FormDataListOptions) => NodeListResponse;
    getData: (id: number | string, dataUuid: string) => NodeResponse;
    deleteData: (id: number | string, dataUuid: string) => void;
    getDataBinary: (id: number | string, dataUuid: string, binaryField: string) => Blob;

    restoreFromWastebin: (id: number | string, options?: WastebinRestoreOptions) => Response;
    deleteFromWastebin: (id: number | string, options?: WastebinDeleteOptions) => Response;
}

export interface AbstractGroupAPI extends BasicAPI {
    list: (options?: GroupListOptions) => GroupListResponse;
    create: (id: number | string, body: GroupCreateRequest) => GroupCreateResponse;
    get: (id: number | string) => GroupResponse;
    update: (id: number | string, body: GroupUpdateRequest) => GroupResponse;
    delete: (id: number | string) => void;

    tree: (options?: GroupTreeOptions) => GroupTreeResponse;
    move: (id: number | string, target: number | string) => GroupResponse;
    subGroups: (id: number | string, options?: BaseListOptionsWithPaging<Group>) => UserGroupsResponse;

    listUsers: (id: number | string, options?: UserListOptions) => UserListResponse;
    createUser: (id: number | string, body: GroupUserCreateRequest) => GroupUserCreateResponse;
    assignUser: (id: number | string, userId: number | string) => UserResponse;
    unassignUser: (id: number | string, userId: number | string) => void;

    listPermissions: (id: number | string, options?: GroupPermissionsListOptions) => GroupPermissionsListResponse;
    getPermission: (id: number | string, perm: AccessControlledType) => GroupTypeOrInstancePermissionsResponse;
    setPermission: (
        id: number | string,
        perm: AccessControlledType,
        body: GroupSetPermissionsRequest
    ) => Response;
    getInstancePermission: (
        id: number | string,
        perm: AccessControlledType,
        instanceId: number | string,
    ) => GroupTypeOrInstancePermissionsResponse;
    setInstancePermission: (
        id: number | string,
        perm: AccessControlledType,
        instanceId: number | string,
        body: GroupSetPermissionsRequest
    ) => Response;
}

export interface AbstractI18nAPI extends BasicAPI {
    listLanguages: () => I18nLanguageListResponse;
    getLanguage: () => I18nLanguageResponse;
    setLanguage: (body: I18nLanguageSetRequest) => Response;
    translate: (options: I18nTranslationOptions) => string;
}

export interface AbstractImageAPI extends BasicAPI {
    list: (options?: FileListOptions) => ImageListResponse;
    get: (id: number | string, options?: ItemRequestOptions) => ImageResponse;
    getMultiple: (body: MultiObjectLoadRequest) => ImageListResponse;
    update: (id: number | string, body: ImageSaveRequest) => Response;
    delete: (id: number | string, options?: ImageDeleteOptions) => void;

    move: (id: number | string, body: ObjectMoveRequest) => Response;
    moveMultiple: (body: MultiObjectMoveRequest) => Response;

    resize: (body: CropResizeParameters) => FileUploadResponse;
    rotate: (body: RotateParameters) => ImageResponse;

    inheritanceStatus: (id: number | string, options?: InheritanceStatusOptions) => InheritanceResponse;
    multipleInheritanceStatus: (options: MultiInheritanceStatusOptions) => MultipleInheritanceResponse;
    inherit: (id: number | string, body: InheritanceRequest) => InheritanceResponse;
    inheritMultiple: (body: InheritanceRequest, options: MultiInheritanceOptions) => Response;
    pushToMaster: (id: number | string, body: PushToMasterRequest) => Response;
    pushMultipleToMaster: (body: MultiPushToMasterRequest | ChannelSyncRequest) => Response;

    localizationInfo: (id: number | string, options?: LocalizationInfoOptions) => LocalizationInfoResponse;
    multipleLocalizationInfos: (options: MultiLocalizationInfoOptions) => LocalizationInfoResponse;
    listLocalizations: (id: number | string) => LocalizationsResponse;
    localize: (id: number | string, body: LocalizeRequest) => Response;
    unlocalize: (id: number | string, body: UnlocalizeRequest) => Response;
    unlocalizeMultiple: (body: MultiUnlocalizeRequest) => Response;

    restoreFromWastebin: (id: number | string, options?: WastebinRestoreOptions) => Response;
    restoreMultipleFromWastebin: (body: IdSetRequest, options?: WastebinRestoreOptions) => Response;
    deleteFromWastebin: (id: number | string, options?: WastebinDeleteOptions) => Response;
    deleteMultipleFromWastebin: (body: IdSetRequest, options?: WastebinDeleteOptions) => Response;

    usageInFiles: (options?: UsageInFilesOptions) => FileUsageResponse;
    usageInImages: (options?: UsageInImagesOptions) => FileUsageResponse;
    usageInFolders: (options?: UsageInFoldersOptions) => FolderUsageResponse;
    usageInPages: (options?: UsageInPagesOptions) => PageUsageResponse;
    usageInTemplates: (options?: UsageInTemplatesOptions) => TemplateUsageResponse;
    usageInSyncableObject: (options: UsageInSyncableObjectsOptions) => SyncObjectsResponse;
    usageInTotal: (options?: UsageInTotalOptions) => TotalUsageResponse;
}

export interface AbstractInfoAPI extends BasicAPI {
    getMaintenanceMode: () => MaintenanceModeResponse;
}

export interface AbstractLanguageAPI extends BasicAPI {
    list: (options?: LanguageListOptions) => LanguageListResponse;
    create: (id: number | string, body: LanguageCreateRequest) => LanguageResponse;
    get: (id: number | string) => LanguageResponse;
    update: (id: number | string, body: LanguageUpdateRequest) => LanguageResponse;
    delete: (id: number | string) => void;
}

export interface AbstractLinkCheckerAPI extends BasicAPI {
    check: (body: LinkCheckerCheckRequest) => LinkCheckerCheckResponse;
    pages: (options: LinkCheckerOptions) => LinkCheckerPageList;
    pageLinks: (pageId: number | string) => LinkCheckerExternalLinkList;
    pageStatus: (pageId: number | string, body: LinkCheckerPageStatusRequest) => LinkCheckerUpdateResponse;
    link: (pageId: number | string, linkId: number) => LinkResponse;
    replace: (
        pageId: number | string,
        linkId: number,
        body: LinkCheckerReplaceRequest
    ) => Response;
}

export interface AbstractMarkupLanguageAPI extends BasicAPI {
    list: (options?: MarkupLanguageListOptions) => MarkupLanguageListResponse;
}

export interface AbstractMessagingAPI extends BasicAPI {
    list: (options?: MessageListOptions) => MessageListResponse;
    markAsRead: (body: MessageReadRequest) => Response;
    send: (body: SendMessageRequest) => Response;
    delete: (id: number) => Response;
}

export interface AbstractNodeAPI extends BasicAPI {
    list: (options?: NodeListRequestOptions) => NodeListResponse;
    create: (body: NodeCreateRequest) => NodeResponse;
    get: (id: number | string) => NodeResponse;
    update: (id: number | string, body: NodeSaveRequest) => NodeResponse;
    delete: (id: number | string) => void;

    copy: (id: number | string, body: NodeCopyRequest, options?: NodeCopyRequestOptions) => Response;
    settings: (id: number | string) => NodeSettingsResponse;

    listFeatures: (options?: NodeFeatureListRequestOptions) => FeatureModelListResponse;
    instanceFeatures: (id: number | string, options?: NodeFeatureListRequestOptions) => FeatureModelListResponse;
    activateFeature: (id: number | string, feature: NodeFeature) => Response;
    deactivateFeature: (id: number | string, feature: NodeFeature) => Response;

    listLanguages: (id: number | string, options?: NodeLanguageListRequest) => ListResponse<Language>;
    assignLanguage: (id: number | string, languageId: number | string) => Response;
    unassignLanguage: (id: number | string, languageId: number | string) => Response;
    orderLanguages: (id: number | string, body: NodeLanguageOrderRequest) => LanguageListResponse;

    listTemplates: (id: number | string, options?: TemplateListRequest) => PermissionListResponse<Template>;
    assignTemplate: (id: number | string, templateId: number | string) => TemplateResponse;
    unassignTemplate: (id: number | string, templateId: number | string) => Response;

    listConstructs: (id: number | string, options?: PagedConstructListRequestOptions) => PagedConstructListResponse;
    assignConstruct: (id: number | string, constructId: number | string) => Response;
    unassignConstruct: (id: number | string, constructId: number | string) => Response;

    listObjectProperties: (id: number | string, options?: ObjectPropertyListOptions) => ObjectPropertyListResponse;
    assignObjectProperty: (id: number | string, opId: number | string) => Response;
    unassignObjectProperty: (id: number | string, opId: number | string) => Response;
}

export interface AbstractObjectPropertyAPI extends BasicAPI {
    list: (options?: ObjectPropertyListOptions) => ObjectPropertyListResponse;
    create: (body: ObjectPropertyCreateRequest) => ObjectPropertyCreateResponse;
    get: (id: number | string) => ObjectPropertyLoadResponse;
    update: (id: number | string, body: ObjectPropertyUpdateRequest) => ObjectPropertyUpdateResponse;
    delete: (id: number | string) => Response;

    constructs: (id: number | string) => ConstructListResponse;
    listNodes: (id: number | string) => NodeListResponse;
    linkToNode: (body: NodeMultiLinkRequest) => Response;
    unlinkFromNode: (body: NodeMultiLinkRequest) => Response;
    hash: (id: number | string) => ImplementationHashResponse;
}

export interface AbstractObjectPropertyCategoryAPI extends BasicAPI {
    list: (options?: ObjectPropertyCategoryListOptions) => ObjectPropertyCategoryListResponse;
    create: (body: ObjectPropertyCategoryCreateRequest) => ObjectPropertyCategoryCreateResponse;
    get: (id: number | string) => ObjectPropertyCategoryLoadResponse;
    update: (id: number | string, body: ObjectPropertyCategoryUpdateRequest) => ObjectPropertyCategoryUpdateResponse;
    delete: (id: number | string) => Response;
}

export interface AbstractPageAPI extends BasicAPI {
    list: (options?: PageListOptions) => PageListResponse;
    create: (body: PageCreateRequest | PageVariantCreateRequest) => PageCreateResponse;
    get: (id: number | string, options?: PageRequestOptions) => PageResponse;
    getMultiple: (body: MultiPageLoadRequest) => PageListResponse;
    update: (id: number | string, body: PageSaveRequest) => Response;
    delete: (id: number | string, options?: PageDeleteOptions) => Response;

    copy: (body: PageCopyRequest) => PageCopyResponse;
    move: (id: number | string, body: ObjectMoveRequest) => Response;
    moveMultiple: (body: MultiObjectMoveRequest) => Response;
    restoreVersion: (id: number | string, options: PageRestoreOptions) => PageResponse;
    search: (options?: SearchPagesOptions) => PageResponse;

    preview: (body: PagePreviewRequest) => PagePreviewResponse;
    render: (body: Page<Raw>, options?: PageRenderOptions) => PageRenderResponse;
    renderTag: (id: number | string, tagKeyword: string, body: Page<Raw>, options?: PageTagRenderOptions) => PageRenderResponse;

    suggestFileName: (body: SuggestPageFileNameRequest) => SuggestPageFileNameResponse;
    cancelEdit: (id: number | string, options?: CancelPageEditOptions) => Response;
    assign: (body: PageAssignRequest) => Response;
    translate: (id: number | string, options: PageTranslateOptions) => PageResponse;

    publish: (id: number | string, body: PagePublishRequest, options?: PagePublishOptions) => Response;
    publishMultiple: (body: MultiPagePublishRequest, options?: PagePublishOptions) => Response;
    takeOffline: (id: number | string, body: PageOfflineRequest, options?: PageOfflineOptions) => Response;
    publishQueueApprove: (body: IdSetRequest) => Response,

    listTags: (id: number | string, options?: PageTagListOptions) => PageTagListResponse;
    createTag: (id: number | string, body: ContentTagCreateRequest) => TagCreateResponse;
    createMultipleTags: (id: number | string, body: MultiTagCreateRequest) => MultiTagCreateResponse;
    restoreTag: (id: number | string, tagKeyword: string, options: TagRestoreOptions) => PageTagListResponse;

    workflowDecline: (id: number | string) => Response,
    workflowRevoke: (id: number | string) => Response,

    inheritanceStatus: (id: number | string, options?: InheritanceStatusOptions) => InheritanceResponse;
    multipleInheritanceStatus: (options: MultiInheritanceStatusOptions) => MultipleInheritanceResponse;
    inherit: (id: number | string, body: InheritanceRequest) => InheritanceResponse;
    inheritMultiple: (body: InheritanceRequest, options: MultiInheritanceOptions) => Response;
    pushToMaster: (id: number | string, body: PushToMasterRequest) => Response;
    pushMultipleToMaster: (body: MultiPushToMasterRequest | ChannelSyncRequest) => Response;

    localizationInfo: (id: number | string, options?: LocalizationInfoOptions) => LocalizationInfoResponse;
    multipleLocalizationInfos: (options: MultiLocalizationInfoOptions) => LocalizationInfoResponse;
    listLocalizations: (id: number | string) => LocalizationsResponse;
    localize: (id: number | string, body: LocalizeRequest) => Response;
    unlocalize: (id: number | string, body: UnlocalizeRequest) => Response;
    unlocalizeMultiple: (body: MultiUnlocalizeRequest) => Response;

    restoreFromWastebin: (id: number | string, options?: WastebinRestoreOptions) => Response;
    restoreMultipleFromWastebin: (body: IdSetRequest, options?: WastebinRestoreOptions) => Response;
    deleteFromWastebin: (id: number | string, options?: WastebinDeleteOptions) => Response;
    deleteMultipleFromWastebin: (body: IdSetRequest, options?: WastebinDeleteOptions) => Response;

    usageInFiles: (options?: UsageInFilesOptions) => FileUsageResponse;
    usageInImages: (options?: UsageInImagesOptions) => FileUsageResponse;
    usageInPages: (options?: UsageInPagesOptions) => PageUsageResponse;
    usageInTemplates: (options?: UsageInTemplatesOptions) => TemplateUsageResponse;
    usageInSyncableObject: (options: UsageInSyncableObjectsOptions) => SyncObjectsResponse;
    usageInTotal: (options?: UsageInTotalOptions) => TotalUsageResponse;
}

export interface AbstractPartTypeAPI extends BasicAPI {
    list: (options?: PartTypeListOptions) => PartType[];
}

export interface AbstractPermissionAPI extends BasicAPI {
    getType: (type: AccessControlledType, options?: PermissionsOptions) => PermissionResponse;
    setType: (type: AccessControlledType, body: SetPermissionsRequest, options?: SetPermissionsOptions) => Response;
    typeGroups: (type: AccessControlledType) => GroupPermissionBitsResponse;

    getInstance: (type: AccessControlledType, instanceId: number | string, options?: InstancePermissionsOptions) => PermissionResponse;
    setInstance: (type: AccessControlledType, instanceId: number | string, body: SetPermissionsRequest, options?: SetPermissionsOptions) => Response;
    instanceGroups: (type: AccessControlledType, instanceId: number | string) => GroupPermissionBitsResponse;

    check: (perm: GcmsPermission, type: AccessControlledType, instanceId: number | string, options?: InstancePermissionsOptions) => SinglePermissionResponse;
}

export interface AbstractPolicyMapAPI extends BasicAPI {
    policy: (options: PolicyMapOptions) => PolicyResponse;
    policyGroup: (typeId: number) => PolicyGroupResponse;
}

export interface AbstractRoleAPI extends BasicAPI {
    list: (options?: RoleListOptions) => RoleListResponse;
    create: (body: RoleCreateRequest) => RoleCreateResponse;
    get: (id: number | string) => RoleLoadResponse;
    update: (id: number | string, body: RoleUpdateRequest) => RoleUpdateResponse;
    delete: (id: number | string) => void;

    getPermissions: (id: number | string) => RolePermissionsLoadResponse;
    setPermissions: (id: number | string, body: RolePermissionsUpdateRequest) => RolePermissionsUpdateResponse;
}

export interface AbstractSchedulerAPI extends BasicAPI {
    list: (options?: ScheduleListOptions) => ScheduleListResponse;
    create: (body: ScheduleCreateReqeust) => ScheduleResponse;
    get: (id: number | string) => ScheduleResponse;
    update: (id: number | string, body: ScheduleSaveReqeust) => ScheduleResponse;
    delete: (id: number | string) => Response;

    status: () => SchedulerStatusResponse;
    suspend: (body?: SchedulerSuspendRequest) => SchedulerStatusResponse;
    resume: () => SchedulerStatusResponse;

    execute: (id: number | string) => Response;
    listExecutions: (id: number | string, options?: ScheduleExecutionListOptions) => ScheduleExecutionListResponse;
    getExecution: (id: number | string) => ScheduleExecutionResponse;
}

export interface AbstractScheduleTaskAPI extends BasicAPI {
    list: (options?: ScheduleTaskListOptions) => ScheduleTaskListResponse;
    create: (body: ScheduleTaskCreateRequest) => ScheduleTaskResponse;
    get: (id: number | string) => ScheduleTaskResponse;
    update: (id: number | string, body: ScheduleTaskSaveRequest) => ScheduleTaskResponse;
    delete: (id: number | string) => Response;
}

export interface AbstractSearchIndexAPI extends BasicAPI {
    list: (options?: ElasticSearchIndexListOptions) => ElasticSearchIndexListResponse;

    rebuild: (name: string, options?: ElasticSearchIndexRebuildOptions) => Response;
}

export interface AbstractTemplateAPI extends BasicAPI {
    list: (options?: TemplateListRequest) => TemplateListResponse;
    create: (body: TemplateCreateRequest) => TemplateResponse;
    get: (id: number | string, options?: TemplateLoadOptions) => TemplateResponse;
    update: (id: number | string, body: TemplateSaveRequest) => TemplateResponse;
    delete: (id: number | string) => void;

    copy: (id: number | string, body: TemplateCopyRequest) => TemplateResponse;
    unlock: (id: number | string) => TemplateResponse;
    hash: (id: number | string) => ImplementationHashResponse;

    link: (id: number | string, body: TemplateLinkRequest) => Response;
    linkMultiple: (body: TemplateMultiLinkRequest) => Response;
    unlink: (id: number | string, body: TemplateLinkRequest) => Response;
    unlinkMultiple: (body: TemplateMultiLinkRequest) => Response;

    listTagStatus: (id: number | string, options?: BaseListOptionsWithPaging<TagStatus>) => TemplateTagStatusResponse;
    listFolders: (id: number | string, options?: FolderListOptions) => FolderListResponse;
    listNodes: (id: number | string, options?: NodeListOptions) => NodeListResponse;

    pushToMaster: (id: number | string, body: PushToMasterRequest) => Response;
    pushMultipleToMaster: (body: MultiPushToMasterRequest) => Response;

    localizationInfo: (id: number | string, options?: LocalizationInfoOptions) => LocalizationInfoResponse;
    multipleLocalizationInfos: (options: MultiLocalizationInfoOptions) => LocalizationInfoResponse;
    listLocalizations: (id: number | string) => LocalizationsResponse;
    localize: (id: number | string, body: LocalizeRequest) => Response;
    unlocalize: (id: number | string, body: UnlocalizeRequest) => Response;
    unlocalizeMultiple: (body: MultiUnlocalizeRequest) => Response;
}

export interface AbstractUserAPI extends BasicAPI {
    list: (options?: UserListOptions) => UserListResponse;
    get: (id: number | string) => UserResponse;
    update: (id: number | string, body: UserUpdateRequest) => UserUpdateResponse;
    delete: (id: number | string) => void;

    me: (options?: UserRequestOptions) => UserResponse;
    getFullUserData: () => UserDataResponse;
    getUserData: (key: string) => UserDataResponse;
    setUserData: (key: string, body: any) => Response;
    deleteUserData: (key: string) => Response;

    listGroups: (id: number | string, options?: GroupListOptions) => GroupListResponse;
    assignToGroup: (id: number | string, groupId: number | string) => GroupResponse;
    unassignFromGroup: (id: number | string, groupId: number | string) => void;

    listNodeRestrictions: (id: number | string, groupId: number | string) => UserGroupNodeRestrictionsResponse;
    addNodeRestriction: (id: number | string, groupId: number | string, nodeId: number | string) => UserGroupNodeRestrictionsResponse;
    removeNodeRestriction: (id: number | string, groupId: number | string, nodeId: number | string) => UserGroupNodeRestrictionsResponse;
}

export interface AbstractUsersnapAPI extends BasicAPI {
    getUsersnapSettings: () => UsersnapSettingsResponse;
}

export interface AbstractValidationAPI extends BasicAPI {

}

export interface AbstractPublishProtocolAPI extends BasicAPI {
    get: (type: PublishType, objId: number) =>  PublishLogEntry;
    list: (options?: PublishLogListOption) => ListResponse<PublishLogEntry>;
}


export interface AbstractRootAPI {
    admin: AbstractAdminAPI;
    auth: AbstractAuthenticationAPI;
    cluster: AbstractClusterAPI;
    construct: AbstractConstructAPI;
    constructCategory: AbstractConstrctCategoryAPI;
    contentRepository: AbstractContentRepositoryAPI;
    contentRepositoryFragment: AbstractContentRepositoryFragmentAPI;
    contentStaging: AbstractContentStagingAPI;
    dataSource: AbstractDataSourceAPI;
    devTools: AbstractDevToolsAPI;
    elasticSearch: AbstractElasticSearchAPI;
    file: AbstractFileAPI;
    folder: AbstractFolderAPI;
    form: AbstractFormAPI;
    fum: AbstractFileUploadManipulatorAPI;
    group: AbstractGroupAPI;
    i18n: AbstractI18nAPI;
    image: AbstractImageAPI;
    info: AbstractInfoAPI;
    language: AbstractLanguageAPI;
    linkChecker: AbstractLinkCheckerAPI;
    markupLanguage: AbstractMarkupLanguageAPI;
    message: AbstractMessagingAPI;
    node: AbstractNodeAPI;
    objectProperty: AbstractObjectPropertyAPI;
    objectPropertyCategory: AbstractObjectPropertyCategoryAPI;
    page: AbstractPageAPI;
    partType: AbstractPartTypeAPI;
    permission: AbstractPermissionAPI;
    policyMap: AbstractPolicyMapAPI;
    publishProtocol: AbstractPublishProtocolAPI;
    role: AbstractRoleAPI;
    scheduler: AbstractSchedulerAPI;
    schedulerTask: AbstractScheduleTaskAPI;
    searchIndex: AbstractSearchIndexAPI;
    template: AbstractTemplateAPI;
    user: AbstractUserAPI;
    usersnap: AbstractUsersnapAPI;
    validation: AbstractValidationAPI;
}
