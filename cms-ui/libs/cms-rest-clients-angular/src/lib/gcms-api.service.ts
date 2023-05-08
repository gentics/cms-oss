import { Injectable } from '@angular/core';
import { AdminInfoApi } from './admin-info/admin-info-api';
import { AdminApi } from './admin/admin-api';
import { AuthApi } from './auth/auth-api';
import { ApiBase } from './base/api-base.service';
import { ConstructCategoryApi } from './construct-category/construct-category-api';
import { ContentStagingApi } from './content-staging/content-staging-api';
import { ContentrespositoryApi } from './content-respository/content-respository-api';
import { ContentrespositoryFragmentApi } from './cr-fragment/cr-fragment-api';
import { DataSourceApi } from './datasource/datasource-api';
import { DevToolsApi } from './dev-tools/dev-tools-api';
import { ElasticSearchIndexApi } from './elastic-search-index/elastic-search-index-api';
import { FileApi } from './file/file-api';
import { FolderApi } from './folder/folder-api';
import { FormApi } from './form/form-api';
import { GroupApi } from './group/group-api';
import { I18nApi } from './i18n/i18n-api';
import { ImageApi } from './image/image-api';
import { InfoApi } from './info/info-api';
import { LanguageApi } from './language/language-api';
import { LinkCheckerApi } from './link-checker/link-checker-api';
import { LogsApi } from './logs/logs-api';
import { MarkupLanguageApi } from './markup-language/markup-language-api';
import { MessagingApi } from './messaging/messaging-api';
import { NodeApi } from './node/node-api';
import { ObjectPropertyApi } from './object-property/object-property-api';
import { ObjectPropertyCategoryApi } from './object-property-category/object-property-category-api';
import { PageApi } from './page/page-api';
import { PermissionApi } from './permission/permission-api';
import { PublishQueueApi } from './publish-queue/publish-queue-api';
import { RoleApi } from './role/role-api';
import { SchedulerApi } from './scheduler/scheduler-api';
import { TagTypeApi } from './tag-type/tag-type-api';
import { TemplateApi } from './template/template-api';
import { UserApi } from './user/user-api';
import { UserDataApi } from './user-data/user-data-api';

/**
 * This is the GcmsApi service that should be used throughout an app making use of the GCMS REST API.
 * It simply collects together the various api classes into a single object.
 */
@Injectable({
    providedIn: 'root',
    })
export class GcmsApi {

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

    constructor(
        apiBase: ApiBase,
    ) {
        this.admin = new AdminApi(apiBase);
        this.auth = new AuthApi(apiBase);
        this.contentStaging = new ContentStagingApi(apiBase);
        this.constructCategory = new ConstructCategoryApi(apiBase);
        this.contentrepositories = new ContentrespositoryApi(apiBase);
        this.contentRepositoryFragments = new ContentrespositoryFragmentApi(apiBase);
        this.files = new FileApi(apiBase);
        this.folders = new FolderApi(apiBase);
        this.forms = new FormApi(apiBase);
        this.dataSource = new DataSourceApi(apiBase);
        this.folders = new FolderApi(apiBase);
        this.i18n = new I18nApi(apiBase);
        this.images = new ImageApi(apiBase);
        this.info = new InfoApi(apiBase);
        this.language = new LanguageApi(apiBase);
        this.linkChecker = new LinkCheckerApi(apiBase);
        this.markuplanguage = new MarkupLanguageApi(apiBase);
        this.messages = new MessagingApi(apiBase);
        this.objectproperties = new ObjectPropertyApi(apiBase);
        this.objectPropertycategories = new ObjectPropertyCategoryApi(apiBase);
        this.pages = new PageApi(apiBase);
        this.permissions = new PermissionApi(apiBase);
        this.publishQueue = new PublishQueueApi(apiBase);
        this.role = new RoleApi(apiBase);
        this.scheduler = new SchedulerApi(apiBase);
        this.tagType = new TagTypeApi(apiBase);
        this.template = new TemplateApi(apiBase);
        this.userData = new UserDataApi(apiBase);
        this.adminInfo = new AdminInfoApi(apiBase);
        this.user = new UserApi(apiBase);
        this.group = new GroupApi(apiBase);
        this.node = new NodeApi(apiBase);
        this.elasticSearchIndex = new ElasticSearchIndexApi(apiBase);
        this.devTools = new DevToolsApi(apiBase);
        this.logs = new LogsApi(apiBase);
    }
}

export const API_PROVIDERS = [ApiBase, GcmsApi];
