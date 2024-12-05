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
    AbstractPublishProtocolAPI,
    AbstractTranslationAPI,
    AbstractLicenseAPI,
} from './abstracts';
import { BasicAPI, Callable } from './common';

export enum RequestMethod {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
    DELETE = 'DELETE',
}

// export const SESSION_REQUEST_HEADER = 'X-Session-Content';
// export const SESSION_RESPONSE_HEADER = SESSION_REQUEST_HEADER;

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

/**
 * A prepared request that can be send or canceled.
 * This request has to be *lazy*, i.E.  it should not perform the request
 * until the `send` method has been called.
 *
 * Additionally, the request should only be sent once for this instance.
 * When the `send` method is called again, the same response should be returned.
 *
 * Canceling the request should throw an `GCMSRestClientRequestError`,
 * to follow the proper handling of the Response data.
 */
export interface GCMSRestClientRequest<T> {
    /**
     * Sends the request to the API.
     * Repeated calls of this function may not trigger additional requests,
     * but may return the same promise/response.
     * For multiple requests, call the client functions accordingly.
     *
     * @returns A promise with the requested/parsed data from the response.
     */
    send: () => Promise<T>;
    /**
     * Cancels the request to the API.
     * If no request has yet been performed, attempts to do so, must not be performed
     * and should immediatly throw an `GCMSRestClientRequestError`.
     */
    cancel: () => void;
}

/**
 * Interface for a driver to be used in the GCMS Client.
 * The driver performs the actual request to the GCMS REST API.
 * To accomendate different use cases and platforms, this interface is
 * to be used to define the behaviour of a driver that can be used.
 */
export interface GCMSClientDriver {
    /**
     * Performs a request to the API and returns a prepared request.
     * The prepared request must return the body of the response, parsed, as JSON when sent.
     *
     * @param request The request that should be sent to the API.
     * @param body The body that should be sent to the API
     */
    performMappedRequest<T>(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<T>;

    /**
     * Performs a request to the API and returns a prepared request.
     * The prepared request must return the body of the response, raw, without any parsing.
     *
     * @param request The request that should be sent to the API.
     * @param body The body that should be sent to the API
     */
    performRawRequest(
        request: GCMSRestClientRequestData,
        body?: null | string | FormData,
    ): GCMSRestClientRequest<string>;

    /**
     * Performs a request to the API and returns a prepared request.
     * The prepared request must return the body of the response, as a Blob, to further process or download.
     *
     * @param request The request that should be sent to the API.
     * @param body The body that should be sent to the API
     */
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
export type GCMSPublishProtocolAPI = MappedAPI<AbstractPublishProtocolAPI>;
export type GCMSTranslationAPI = MappedAPI<AbstractTranslationAPI>;
export type GCMSLicenseAPI = MappedAPI<AbstractLicenseAPI>;

export type GCMSRootAPI = {
    [K in keyof AbstractRootAPI]: MappedAPI<AbstractRootAPI[K]>;
}
