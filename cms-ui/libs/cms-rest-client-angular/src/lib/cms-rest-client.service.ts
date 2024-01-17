import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
    AbstractAdminAPI,
    AbstractAuthenticationAPI,
    AbstractClusterAPI,
    AbstractConstrctCategoryAPI,
    AbstractConstructAPI,
    AbstractContentRepositoryAPI,
    AbstractContentRepositoryFragmentAPI,
    AbstractContentStagingAPI,
    AbstractDataSourceAPI,
    AbstractDevToolsAPI,
    AbstractElasticSearchAPI,
    AbstractFileAPI,
    AbstractFileUploadManipulatorAPI,
    AbstractFolderAPI,
    AbstractFormAPI,
    AbstractRootAPI,
    AbstractGroupAPI,
    AbstractI18nAPI,
    AbstractImageAPI,
    AbstractInfoAPI,
    AbstractLanguageAPI,
    AbstractLinkCheckerAPI,
    AbstractMarkupLanguageAPI,
    AbstractMessagingAPI,
    AbstractNodeAPI,
    AbstractObjectPropertyAPI,
    AbstractObjectPropertyCategoryAPI,
    AbstractPageAPI,
    AbstractPermissionAPI,
    AbstractPolicyMapAPI,
    AbstractRoleAPI,
    AbstractScheduleTaskAPI,
    AbstractSchedulerAPI,
    AbstractSearchIndexAPI,
    AbstractTemplateAPI,
    AbstractUserAPI,
    AbstractUsersnapAPI,
    AbstractValidationAPI,
    AbstractPartTypeAPI,
} from '@gentics/cms-rest-client/abstracts';
import {
    GCMSClientDriver,
    GCMSRestClient,
    GCMSRestClientConfig,
    GCMSRestClientResponse,
} from '@gentics/cms-rest-client';
import { Observable } from 'rxjs';
import { AngularGCMSClientDriver } from './angular-cms-client-driver';
import { NGGCMSRestClientResponse } from './models';

type Callable<P extends Array<any>, R> = (...args: P) => R;
type BasicAPI = { [key: string]: (...args) => any };

type OriginalAPI<T extends BasicAPI> = {
    [K in Exclude<string, keyof T>]: never;
} & {
    [FN in keyof T]: Callable<Parameters<T[FN]>, GCMSRestClientResponse<ReturnType<T[FN]>>>;
};

type AngularAPI<T extends BasicAPI> = {
    [K in Exclude<string, keyof T>]: never;
} & {
    [FN in keyof T]: Callable<Parameters<T[FN]>, Observable<ReturnType<T[FN]>>>;
}

type FullAngularAPI = {
    [K in keyof AbstractRootAPI]: AngularAPI<AbstractRootAPI[K]>;
}

interface APIDefinition extends FullAngularAPI {

}

/**
 * The regular GCMS-API is promise based. However, it is more common to use rxjs based
 * Observables instead.
 * To achieve this, the driver has been replaced and returns an extension of the regular response.
 * This mapping helper then creates a delegate function and returns only the `rx` value from the response.
 * Therefore making the whole API rxjs/angular friendly without having to call the function youself.
 *
 * Example without mapping:
 * ```javascript
 * client.info.getMaintenanceMode().rx().subscribe(status => console.log(status));
 * ```
 *
 * Example with mapping:
 * ```javascript
 * client.info.getMaintenanceMode().subscribe(status => console.log(status));
 * ```
 *
 * @param api The API to map
 * @returns An "Angular" API, which simply delegates the call and returns the Observable.
 */
function asAngularAPI<T extends BasicAPI>(api: OriginalAPI<T>): AngularAPI<T> {
    return Object.entries(api).reduce((acc, [name, fn]: [string, Callable<any[], any>]) => {
        acc[name] = (...args) => (fn(...args) as NGGCMSRestClientResponse<any>).rx();
        return acc;
    }, {}) as any;
}

@Injectable()
export class GCMSRestClientService implements APIDefinition {

    protected driver: GCMSClientDriver;
    protected client: GCMSRestClient;
    /** Delegate map for each API, to not compute it everytime it's getting accessed. */
    protected apis: FullAngularAPI;

    constructor(
        http: HttpClient,
    ) {
        this.driver = new AngularGCMSClientDriver(http);
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        this.client = new GCMSRestClient(this.driver);
        this.initializeAPIs();
    }

    private initializeAPIs(): void {
        this.apis = {
            admin: asAngularAPI<AbstractAdminAPI>(this.client.admin),
            auth: asAngularAPI<AbstractAuthenticationAPI>(this.client.auth),
            cluster: asAngularAPI<AbstractClusterAPI>(this.client.cluster),
            construct: asAngularAPI<AbstractConstructAPI>(this.client.construct),
            constructCategory: asAngularAPI<AbstractConstrctCategoryAPI>(this.client.constructCategory),
            contentRepository: asAngularAPI<AbstractContentRepositoryAPI>(this.client.contentRepository),
            contentRepositoryFragment: asAngularAPI<AbstractContentRepositoryFragmentAPI>(this.client.contentRepositoryFragment),
            contentStaging: asAngularAPI<AbstractContentStagingAPI>(this.client.contentStaging),
            dataSource: asAngularAPI<AbstractDataSourceAPI>(this.client.dataSource),
            devTools: asAngularAPI<AbstractDevToolsAPI>(this.client.devTools),
            elasticSearch: asAngularAPI<AbstractElasticSearchAPI>(this.client.elasticSearch),
            file: asAngularAPI<AbstractFileAPI>(this.client.file),
            folder: asAngularAPI<AbstractFolderAPI>(this.client.folder),
            form: asAngularAPI<AbstractFormAPI>(this.client.form),
            fum: asAngularAPI<AbstractFileUploadManipulatorAPI>(this.client.fum),
            group: asAngularAPI<AbstractGroupAPI>(this.client.group),
            i18n: asAngularAPI<AbstractI18nAPI>(this.client.i18n),
            image: asAngularAPI<AbstractImageAPI>(this.client.image),
            info: asAngularAPI<AbstractInfoAPI>(this.client.info),
            language: asAngularAPI<AbstractLanguageAPI>(this.client.language),
            linkChecker: asAngularAPI<AbstractLinkCheckerAPI>(this.client.linkChecker),
            markupLanguage: asAngularAPI<AbstractMarkupLanguageAPI>(this.client.markupLanguage),
            message: asAngularAPI<AbstractMessagingAPI>(this.client.message),
            node: asAngularAPI<AbstractNodeAPI>(this.client.node),
            objectProperty: asAngularAPI<AbstractObjectPropertyAPI>(this.client.objectProperty),
            objectPropertyCategory: asAngularAPI<AbstractObjectPropertyCategoryAPI>(this.client.objectPropertyCategory),
            page: asAngularAPI<AbstractPageAPI>(this.client.page),
            partType: asAngularAPI<AbstractPartTypeAPI>(this.client.partType),
            permission: asAngularAPI<AbstractPermissionAPI>(this.client.permission),
            policyMap: asAngularAPI<AbstractPolicyMapAPI>(this.client.policyMap),
            role: asAngularAPI<AbstractRoleAPI>(this.client.role),
            scheduler: asAngularAPI<AbstractSchedulerAPI>(this.client.scheduler),
            schedulerTask: asAngularAPI<AbstractScheduleTaskAPI>(this.client.schedulerTask),
            searchIndex: asAngularAPI<AbstractSearchIndexAPI>(this.client.searchIndex),
            template: asAngularAPI<AbstractTemplateAPI>(this.client.template),
            user: asAngularAPI<AbstractUserAPI>(this.client.user),
            usersnap: asAngularAPI<AbstractUsersnapAPI>(this.client.usersnap),
            validation: asAngularAPI<AbstractValidationAPI>(this.client.validation),
        };
    }

    init(config: GCMSRestClientConfig, sid?: string): void {
        this.client.config = config;
        this.client.sid = sid;
    }

    configure(config: GCMSRestClientConfig): void {
        this.client.config = config;
    }

    setSessionId(sid: number | string): void {
        this.client.sid = sid;
    }

    isInitialized(): boolean {
        return this.client != null;
    }

    getConfig(): GCMSRestClientConfig {
        return this.client.config;
    }

    getClient(): GCMSRestClient {
        return this.client;
    }

    get admin(): AngularAPI<AbstractAdminAPI> {
        return this.apis.admin;
    }

    get auth(): AngularAPI<AbstractAuthenticationAPI> {
        return this.apis.auth
    }

    get cluster(): AngularAPI<AbstractClusterAPI> {
        return this.apis.cluster;
    }

    get construct(): AngularAPI<AbstractConstructAPI> {
        return this.apis.construct;
    }

    get constructCategory(): AngularAPI<AbstractConstrctCategoryAPI> {
        return this.apis.constructCategory;
    }

    get contentRepository(): AngularAPI<AbstractContentRepositoryAPI> {
        return this.apis.contentRepository;
    }

    get contentRepositoryFragment(): AngularAPI<AbstractContentRepositoryFragmentAPI> {
        return this.apis.contentRepositoryFragment;
    }

    get contentStaging(): AngularAPI<AbstractContentStagingAPI> {
        return this.apis.contentStaging;
    }

    get dataSource(): AngularAPI<AbstractDataSourceAPI> {
        return this.apis.dataSource;
    }

    get devTools(): AngularAPI<AbstractDevToolsAPI> {
        return this.apis.devTools;
    }

    get elasticSearch(): AngularAPI<AbstractElasticSearchAPI> {
        return this.apis.elasticSearch;
    }

    get file(): AngularAPI<AbstractFileAPI> {
        return this.apis.file;
    }

    get folder(): AngularAPI<AbstractFolderAPI> {
        return this.apis.folder;
    }

    get form(): AngularAPI<AbstractFormAPI> {
        return this.apis.form;
    }

    get fum(): AngularAPI<AbstractFileUploadManipulatorAPI> {
        return this.apis.fum;
    }

    get group(): AngularAPI<AbstractGroupAPI> {
        return this.apis.group;
    }

    get i18n(): AngularAPI<AbstractI18nAPI> {
        return this.apis.i18n;
    }

    get image(): AngularAPI<AbstractImageAPI> {
        return this.apis.image;
    }

    get info(): AngularAPI<AbstractInfoAPI> {
        return this.apis.info;
    }

    get language(): AngularAPI<AbstractLanguageAPI> {
        return this.apis.language;
    }

    get linkChecker(): AngularAPI<AbstractLinkCheckerAPI> {
        return this.apis.linkChecker;
    }

    get markupLanguage(): AngularAPI<AbstractMarkupLanguageAPI> {
        return this.apis.markupLanguage;
    }

    get message(): AngularAPI<AbstractMessagingAPI> {
        return this.apis.message;
    }

    get node(): AngularAPI<AbstractNodeAPI> {
        return this.apis.node;
    }

    get objectProperty(): AngularAPI<AbstractObjectPropertyAPI> {
        return this.apis.objectProperty;
    }

    get objectPropertyCategory(): AngularAPI<AbstractObjectPropertyCategoryAPI> {
        return this.apis.objectPropertyCategory;
    }

    get page(): AngularAPI<AbstractPageAPI> {
        return this.apis.page;
    }

    get partType(): AngularAPI<AbstractPartTypeAPI> {
        return this.apis.partType;
    }

    get permission(): AngularAPI<AbstractPermissionAPI> {
        return this.apis.permission;
    }

    get policyMap(): AngularAPI<AbstractPolicyMapAPI> {
        return this.apis.policyMap;
    }

    get role(): AngularAPI<AbstractRoleAPI> {
        return this.apis.role;
    }

    get scheduler(): AngularAPI<AbstractSchedulerAPI> {
        return this.apis.scheduler;
    }

    get schedulerTask(): AngularAPI<AbstractScheduleTaskAPI> {
        return this.apis.schedulerTask;
    }

    get searchIndex(): AngularAPI<AbstractSearchIndexAPI> {
        return this.apis.searchIndex;
    }

    get template(): AngularAPI<AbstractTemplateAPI> {
        return this.apis.template;
    }

    get user(): AngularAPI<AbstractUserAPI> {
        return this.apis.user;
    }

    get usersnap(): AngularAPI<AbstractUsersnapAPI> {
        return this.apis.usersnap;
    }

    get validation(): AngularAPI<AbstractValidationAPI> {
        return this.apis.validation;
    }
}
