import { Injectable } from '@angular/core';
import {
    AdminApi,
    AdminInfoApi,
    AuthApi,
    ConstructCategoryApi,
    ContentStagingApi,
    ContentrespositoryApi,
    ContentrespositoryFragmentApi,
    DataSourceApi,
    DevToolsApi,
    ElasticSearchIndexApi,
    FileApi,
    FolderApi,
    FormApi,
    GcmsApi,
    GroupApi,
    I18nApi,
    ImageApi,
    InfoApi,
    LanguageApi,
    LinkCheckerApi,
    LogsApi,
    MarkupLanguageApi,
    MessagingApi,
    NodeApi,
    ObjectPropertyApi,
    ObjectPropertyCategoryApi,
    PageApi,
    PermissionApi,
    PublishQueueApi,
    RoleApi,
    SchedulerApi,
    TagTypeApi,
    TemplateApi,
    UserApi,
    UserDataApi,
} from '@gentics/cms-rest-clients-angular';

/** Helper type used to make sure that we add all GcmsApi's properties also to the Api service. */
type IGcmsApi = { [K in keyof GcmsApi]: GcmsApi[K] };

/**
 * This is the Api service that should be used throughout the app.
 * Due to compatibility reasons with existing code, it provides access to all properties
 * of the GcmsApi service from the @gentics/cms-rest-clients-angular library.
 */
@Injectable()
export class Api implements IGcmsApi {

    admin: AdminApi;
    adminInfo: AdminInfoApi;
    auth: AuthApi;
    constructCategory: ConstructCategoryApi;
    contentStaging: ContentStagingApi;
    contentrepositories: ContentrespositoryApi;
    contentRepositoryFragments: ContentrespositoryFragmentApi;
    dataSource: DataSourceApi;
    devTools: DevToolsApi;
    elasticSearchIndex: ElasticSearchIndexApi;
    files: FileApi;
    folders: FolderApi;
    forms: FormApi;
    group: GroupApi;
    i18n: I18nApi;
    images: ImageApi;
    info: InfoApi;
    language: LanguageApi;
    linkChecker: LinkCheckerApi;
    logs: LogsApi;
    markuplanguage: MarkupLanguageApi;
    messages: MessagingApi;
    node: NodeApi;
    objectproperties: ObjectPropertyApi;
    objectPropertycategories: ObjectPropertyCategoryApi;
    pages: PageApi;
    permissions: PermissionApi;
    publishQueue: PublishQueueApi;
    role: RoleApi;
    scheduler: SchedulerApi;
    tagType: TagTypeApi;
    template: TemplateApi;
    user: UserApi;
    userData: UserDataApi;

    constructor(gcmsApi: GcmsApi) {
        this.admin = gcmsApi.admin;
        this.adminInfo = gcmsApi.adminInfo;
        this.auth = gcmsApi.auth;
        this.constructCategory = gcmsApi.constructCategory;
        this.contentStaging = gcmsApi.contentStaging;
        this.contentrepositories = gcmsApi.contentrepositories;
        this.contentRepositoryFragments = gcmsApi.contentRepositoryFragments;
        this.dataSource = gcmsApi.dataSource;
        this.devTools = gcmsApi.devTools;
        this.elasticSearchIndex = gcmsApi.elasticSearchIndex;
        this.files = gcmsApi.files;
        this.folders = gcmsApi.folders;
        this.forms = gcmsApi.forms;
        this.group = gcmsApi.group;
        this.i18n = gcmsApi.i18n;
        this.images = gcmsApi.images;
        this.info = gcmsApi.info;
        this.language = gcmsApi.language;
        this.linkChecker = gcmsApi.linkChecker;
        this.logs = gcmsApi.logs;
        this.markuplanguage = gcmsApi.markuplanguage;
        this.messages = gcmsApi.messages;
        this.node = gcmsApi.node;
        this.objectproperties = gcmsApi.objectproperties;
        this.objectPropertycategories = gcmsApi.objectPropertycategories;
        this.pages = gcmsApi.pages;
        this.permissions = gcmsApi.permissions;
        this.publishQueue = gcmsApi.publishQueue;
        this.role = gcmsApi.role;
        this.scheduler = gcmsApi.scheduler;
        this.tagType = gcmsApi.tagType;
        this.template = gcmsApi.template;
        this.user = gcmsApi.user;
        this.userData = gcmsApi.userData;
    }

}
