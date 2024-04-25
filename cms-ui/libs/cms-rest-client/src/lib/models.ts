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
    AbstractPartTypeAPI,
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
} from './abstracts';
import { BasicAPI, Callable } from './common';

export enum RequestMethod {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
    DELETE = 'DELETE',
}

export interface AbsoluteGCMSClientConnection {
    absolute: true;
    ssl?: boolean;
    host: string;
    port?: number;
    basePath?: string;
}

export interface RelativeGCMSClientConnection {
    absolute: false;
    basePath: string;
}

export type GCMSClientConnection = AbsoluteGCMSClientConnection | RelativeGCMSClientConnection;

export interface GCMSRestClientConfig {
    interceptors?: GCMSRestClientInterceptor[];
    connection: GCMSClientConnection;
}

export type GCMSRestClientInterceptor = (data: GCMSRestClientInterceptorData) => GCMSRestClientInterceptorData;

export interface GCMSRestClientInterceptorData {
    method: RequestMethod;
    protocol?: 'http' | 'https';
    host: string;
    port?: number;
    path: string;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export interface GCMSRestClientRequestData {
    method: RequestMethod;
    url: string;
    params: Record<string, string>;
    headers: Record<string, string>;
}

export interface GCMSRestClientRequest<T> {
    send: () => Promise<T>;
    cancel: () => void;
}

export interface GCMSClientDriver {
    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<T>;

    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<string>;

    performDownloadRequest(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<Blob>;
}

type MappedAPI<T extends BasicAPI> = {
    [K in Exclude<string, keyof T>]: never;
} & {
    [FN in keyof T]: Callable<Parameters<T[FN]>, GCMSRestClientRequest<ReturnType<T[FN]>>>;
};

export type GCMSAdminAPI = MappedAPI<AbstractAdminAPI>;
export type GCMSAuthenticationAPI = MappedAPI<AbstractAuthenticationAPI>;
export type GCMSClusterAPI = MappedAPI<AbstractClusterAPI>;
export type GCMSConstructAPI = MappedAPI<AbstractConstructAPI>;
export type GCMSConstrctCategoryAPI = MappedAPI<AbstractConstrctCategoryAPI>;
export type GCMSContentRepositoryFragmentAPI = MappedAPI<AbstractContentRepositoryFragmentAPI>;
export type GCMSContentRepositoryAPI = MappedAPI<AbstractContentRepositoryAPI>;
export type GCMSContentStagingAPI = MappedAPI<AbstractContentStagingAPI>;
export type GCMSDataSourceAPI = MappedAPI<AbstractDataSourceAPI>;
export type GCMSDevToolsAPI = MappedAPI<AbstractDevToolsAPI>;
export type GCMSElasticSearchAPI = MappedAPI<AbstractElasticSearchAPI>;
export type GCMSFileUploadManipulatorAPI = MappedAPI<AbstractFileUploadManipulatorAPI>;
export type GCMSFileAPI = MappedAPI<AbstractFileAPI>;
export type GCMSFolderAPI = MappedAPI<AbstractFolderAPI>;
export type GCMSFormAPI = MappedAPI<AbstractFormAPI>;
export type GCMSGroupAPI = MappedAPI<AbstractGroupAPI>;
export type GCMSI18nAPI = MappedAPI<AbstractI18nAPI>;
export type GCMSImageAPI = MappedAPI<AbstractImageAPI>;
export type GCMSInfoAPI = MappedAPI<AbstractInfoAPI>;
export type GCMSLanguageAPI = MappedAPI<AbstractLanguageAPI>;
export type GCMSLinkCheckerAPI = MappedAPI<AbstractLinkCheckerAPI>;
export type GCMSMarkupLanguageAPI = MappedAPI<AbstractMarkupLanguageAPI>;
export type GCMSMessagingAPI = MappedAPI<AbstractMessagingAPI>;
export type GCMSNodeAPI = MappedAPI<AbstractNodeAPI>;
export type GCMSObjectPropertyAPI = MappedAPI<AbstractObjectPropertyAPI>;
export type GCMSObjectPropertyCategoryAPI = MappedAPI<AbstractObjectPropertyCategoryAPI>;
export type GCMSPageAPI = MappedAPI<AbstractPageAPI>;
export type GCMSPartTypeAPI = MappedAPI<AbstractPartTypeAPI>;
export type GCMSPermissionAPI = MappedAPI<AbstractPermissionAPI>;
export type GCMSPolicyMapAPI = MappedAPI<AbstractPolicyMapAPI>;
export type GCMSRoleAPI = MappedAPI<AbstractRoleAPI>;
export type GCMSSchedulerAPI = MappedAPI<AbstractSchedulerAPI>;
export type GCMSScheduleTaskAPI = MappedAPI<AbstractScheduleTaskAPI>;
export type GCMSSearchIndexAPI = MappedAPI<AbstractSearchIndexAPI>;
export type GCMSTemplateAPI = MappedAPI<AbstractTemplateAPI>;
export type GCMSUserAPI = MappedAPI<AbstractUserAPI>;
export type GCMSUsersnapAPI = MappedAPI<AbstractUsersnapAPI>;
export type GCMSValidationAPI = MappedAPI<AbstractValidationAPI>;

export type GCMSRootAPI = {
    [K in keyof AbstractRootAPI]: MappedAPI<AbstractRootAPI[K]>;
}
