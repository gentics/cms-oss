import { BaseListOptionsWithPaging, PublishLogEntry } from '@gentics/cms-models';
import { CONTENT_TYPE_JSON, DELETE, GET, HTTP_HEADER_CONTENT_TYPE, POST, PUT, QUERY_PARAM_SID } from './internal';
import {
    GCMSAdminAPI,
    GCMSAuthenticationAPI,
    GCMSClientDriver,
    GCMSClusterAPI,
    GCMSConstrctCategoryAPI,
    GCMSConstructAPI,
    GCMSContentRepositoryAPI,
    GCMSContentRepositoryFragmentAPI,
    GCMSContentStagingAPI,
    GCMSDataSourceAPI,
    GCMSDevToolsAPI,
    GCMSElasticSearchAPI,
    GCMSFileAPI,
    GCMSFileUploadManipulatorAPI,
    GCMSFolderAPI,
    GCMSFormAPI,
    GCMSGroupAPI,
    GCMSI18nAPI,
    GCMSImageAPI,
    GCMSInfoAPI,
    GCMSLanguageAPI,
    GCMSLicenseAPI,
    GCMSLinkCheckerAPI,
    GCMSMarkupLanguageAPI,
    GCMSMessagingAPI,
    GCMSNodeAPI,
    GCMSObjectPropertyAPI,
    GCMSObjectPropertyCategoryAPI,
    GCMSPageAPI,
    GCMSPartTypeAPI,
    GCMSPermissionAPI,
    GCMSPolicyMapAPI,
    GCMSPublishProtocolAPI,
    GCMSRestClientConfig,
    GCMSRestClientInterceptorData,
    GCMSRestClientRequest,
    GCMSRestClientRequestData,
    GCMSRoleAPI,
    GCMSRootAPI,
    GCMSScheduleTaskAPI,
    GCMSSchedulerAPI,
    GCMSSearchIndexAPI,
    GCMSTemplateAPI,
    GCMSTranslationAPI,
    GCMSUserAPI,
    GCMSUsersnapAPI,
    GCMSValidationAPI,
    RequestMethod,
} from './models';
import { parseJSONSafe, stringifyEmbedOptions, stringifyPagingSortOptions, toRelativePath, trimTrailingSlash } from './utils';

const DEFAULT_CONFIG: GCMSRestClientConfig = {
    connection: {
        absolute: false,
        basePath: '/rest',
    },
};

export class GCMSRestClient implements GCMSRootAPI {

    constructor(
        public driver: GCMSClientDriver,
        public config: GCMSRestClientConfig = DEFAULT_CONFIG,
        public sid: null | number | string = null,
    ) { }

    protected prepareRequest(
        requestMethod: RequestMethod,
        path: string,
        queryParams: Record<string, any>,
        requestHeaders: Record<string, string>,
    ): GCMSRestClientRequestData {
        let buildPath = '';

        if (this.config.connection.basePath) {
            buildPath += trimTrailingSlash(toRelativePath(this.config.connection.basePath));
        } else {
            buildPath += '/rest';
        }
        buildPath += toRelativePath(path);

        // If a session-id is configured, append it to each request.
        if (queryParams == null || typeof queryParams !== 'object') {
            queryParams = {};
        }
        if (this.sid) {
            queryParams[QUERY_PARAM_SID] = `${this.sid}`;
        }

        stringifyPagingSortOptions(queryParams);
        stringifyEmbedOptions(queryParams);

        const data: GCMSRestClientInterceptorData = this.config.connection.absolute ? {
            method: requestMethod,
            protocol: typeof this.config.connection.ssl === 'boolean'
                ? (this.config.connection.ssl ? 'https' : 'http')
                : null,
            host: this.config.connection.host,
            port: this.config.connection.port,
            path: buildPath,
            params: queryParams,
            headers: requestHeaders,
        } : {
            method: requestMethod,
            protocol: null,
            host: null,
            port: null,
            path: buildPath,
            params: queryParams,
            headers: requestHeaders,
        };

        const { method, protocol, host, port, path: finalPath, params, headers } = this.handleInterceptors(data);

        let url: string;

        if (this.config.connection.absolute) {
            url = `${protocol ?? ''}://${host}`;
            if (port) {
                url += `:${port}`;
            }
            url += finalPath;
        } else {
            url = finalPath;
        }

        return {
            method,
            url,
            params,
            headers,
        };
    }

    protected handleInterceptors(data: GCMSRestClientInterceptorData): GCMSRestClientInterceptorData {
        const interceptors = this.config.interceptors || [];
        for (const handler of interceptors) {
            data = handler(data);
        }
        return data;
    }

    public executeMappedJsonRequest<T = any>(
        method: RequestMethod,
        path: string,
        body?: null | any,
        queryParams?: Record<string, any>,
    ): GCMSRestClientRequest<T> {
        const headers: Record<string, string> = {
            [HTTP_HEADER_CONTENT_TYPE]: CONTENT_TYPE_JSON,
        };

        const req = this.prepareRequest(method, path, queryParams, headers);

        if (body != null && typeof body !== 'string') {
            body = JSON.stringify(body);
        }

        return this.driver.performMappedRequest<T>(req, body);
    }

    public executeMappedFormRequest<T = any>(
        method: RequestMethod,
        path: string,
        body?: null | FormData,
        queryParams?: Record<string, any>,
    ): GCMSRestClientRequest<T> {
        const req = this.prepareRequest(method, path, queryParams, {});

        return this.driver.performMappedRequest<T>(req, body);
    }

    public executeRawRequest(
        method: RequestMethod,
        path: string,
        headers: Record<string, string> = {},
        body: null | any = null,
        queryParams: Record<string, any> = {},
    ): GCMSRestClientRequest<string> {
        const req = this.prepareRequest(method, path, queryParams, headers);

        return this.driver.performRawRequest(req, body);
    }

    public executeBlobRequest(
        method: RequestMethod,
        path: string,
        headers: Record<string, string> = {},
        body: null | any = null,
        queryParams: Record<string, any> = {},
    ): GCMSRestClientRequest<Blob> {
        const req = this.prepareRequest(method, path, queryParams, headers);

        return this.driver.performDownloadRequest(req, body);
    }

    public admin: GCMSAdminAPI = {
        getLogs: (options) => this.executeMappedJsonRequest(GET, '/admin/actionlog', null, options),
        getActionLogs: (options) => this.executeMappedJsonRequest(GET, '/admin/actionlog/actions', null, options),
        getTypeLogs: (options) => this.executeMappedJsonRequest(GET, '/admin/actionlog/types', null, options),

        setMaintenanceMode: (body) => this.executeMappedJsonRequest(POST, '/admin/maintenance', body),

        getPublicKey: () => this.executeMappedJsonRequest(GET, '/admin/publicKey'),
        getPublishInfo: () => this.executeMappedJsonRequest(GET, '/admin/publishInfo'),
        getTools: () => this.executeMappedJsonRequest(GET, '/admin/tools'),
        getUpdates: () => this.executeMappedJsonRequest(GET, '/admin/updates'),
        getVersion: () => this.executeMappedJsonRequest(GET, '/admin/version'),
        getFeature: (key) => this.executeMappedJsonRequest(GET, `/admin/features/${key}`),

        reloadConfiguration: () => this.executeMappedJsonRequest(PUT, '/admin/config/reload'),

        getDirtQueue: (options) => this.executeMappedJsonRequest(GET, '/admin/content/dirtqueue', null, options),
        getDirtQueueSummary: () => this.executeMappedJsonRequest(GET, '/admin/content/dirtqueue/summary'),
        redoFailedDirtQueueEntry: (actionId) => this.executeMappedJsonRequest(PUT, `/admin/content/dirtqueue/${actionId}/redo`),
        deleteFailedDirtQueueEntry: (actionId) => this.executeMappedJsonRequest(DELETE, `/admin/content/dirtqueue/${actionId}`),

        getPublishQueue: () => this.executeMappedJsonRequest(GET, '/admin/content/publishqueue'),
        modifyPublishQueue: (body) => this.executeMappedJsonRequest(POST, '/admin/content/publishqueue', body),
    } as const;

    public auth: GCMSAuthenticationAPI = {
        login: (data, params) => this.executeMappedJsonRequest(POST, '/auth/login', data, params),
        logout: (sid) => this.executeMappedJsonRequest(POST, `/auth/logout/${sid}`),
        validate: (sid) => this.executeMappedJsonRequest(GET, `/auth/validate/${sid}`),
        ssoLogin: (bearerToken) => this.executeRawRequest(GET, '/auth/ssologin', {
            // eslint-disable-next-line @typescript-eslint/naming-convention
            Authorization: `Bearer ${bearerToken}`,
        }),
    } as const;

    public cluster: GCMSClusterAPI = {
        getInfo: () => this.executeMappedJsonRequest(GET, '/cluster/info'),
        setMaster: () => this.executeMappedJsonRequest(PUT, '/cluster/master'),
    } as const;

    public construct: GCMSConstructAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/construct', null, options),
        create: (body, options) => this.executeMappedJsonRequest(POST, '/construct', body, options),
        get: (id) => this.executeMappedJsonRequest(GET, `/construct/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/construct/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/construct/${id}`),

        listForEditor: (options) => this.executeMappedJsonRequest(GET, '/construct/list', null, options),

        hash: (id) => this.executeMappedJsonRequest(GET, `/constructs/${id}/hash`),
        getLinkedNodes: (id) => this.executeMappedJsonRequest(GET, `/construct/${id}/nodes`),
        linkToNode: (body) => this.executeMappedJsonRequest(POST, '/construct/link/nodes', body),
        unlinkFromNode: (body) => this.executeMappedJsonRequest(POST, '/construct/unlink/nodes', body),
    } as const;

    public constructCategory: GCMSConstrctCategoryAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/construct/category', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/construct/category', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/construct/category/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/construct/category/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/construct/category/${id}`),

        sort: (body) => this.executeMappedJsonRequest(POST, '/construct/category/sortorder', body),
    } as const;

    public contentRepository: GCMSContentRepositoryAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/contentrepositories', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/contentrepositories', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/contentrepositories/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/contentrepositories/${id}`),

        checkData: (id) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${id}/data/check`),
        repairData: (id) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${id}/data/repair`),
        checkStructure: (id) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${id}/structure/check`),
        repairStructure: (id) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${id}/structure/repair`),

        checkEntries: (crId) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/entries/check`),
        listEntries: (crId, options) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/entries`, null, options),
        createEntry: (crId, body) => this.executeMappedJsonRequest(POST, `/contentrepositories/${crId}/entries`, body),
        getEntry: (crId, entryId) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/entries/${entryId}`),
        updateEntry: (crId, entryId, body) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${crId}/entries/${entryId}`, body),
        deleteEntry: (crId, entryId) => this.executeMappedJsonRequest(DELETE, `/contentrepositories/${crId}/entries/${entryId}`),

        listFragments: (crId, options) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/cr_fragments`, null, options),
        linkFragment: (crId, fragmentId) => this.executeMappedJsonRequest(PUT, `/contentrepositories/${crId}/cr_fragments/${fragmentId}`),
        unlinkFragment: (crId, fragmentId) => this.executeMappedJsonRequest(DELETE, `/contentrepositories/${crId}/cr_fragments/${fragmentId}`),

        listAvailableRoles: (crId) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/availableroles`),
        listAssignedRoles: (crId) => this.executeMappedJsonRequest(GET, `/contentrepositories/${crId}/roles`),
        assignRoles: (crId, body) => this.executeMappedJsonRequest(POST, `/contentrepositories/${crId}/roles`, body),

        proxyLogin: (id) => this.executeMappedJsonRequest(POST, `/contentrepositories/${id}/proxylogin`),
    } as const;

    public contentRepositoryFragment: GCMSContentRepositoryFragmentAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/cr_fragments', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/cr_fragments', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/cr_fragments/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/cr_fragments/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/cr_fragments/${id}`),

        listEntries: (crFragmentId, options) => this.executeMappedJsonRequest(GET, `/cr_fragments/${crFragmentId}/entries`, null, options),
        createEntry: (crFragmentId, body) => this.executeMappedJsonRequest(POST, `/cr_fragments/${crFragmentId}/entries`, body),
        getEntry: (crFragmentId, entryId) => this.executeMappedJsonRequest(GET, `/cr_fragments/${crFragmentId}/entries/${entryId}`),
        updateEntry: (crFragmentId, entryId, body) => this.executeMappedJsonRequest(PUT, `/cr_fragments/${crFragmentId}/entries/${entryId}`, body),
        deleteEntry: (crFragmentId, entryId) => this.executeMappedJsonRequest(DELETE, `/cr_fragments/${crFragmentId}/entries/${entryId}`),
    } as const;

    public contentStaging: GCMSContentStagingAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/content/package', null, options),
        create: (body) => this.executeMappedJsonRequest(PUT, '/content/package', body),
        get: (name) => this.executeMappedJsonRequest(GET, `/content/package/${name}`),
        update: (name, body) => this.executeMappedJsonRequest(POST, `/content/package/${name}`, body),
        delete: (name) => this.executeMappedJsonRequest(DELETE, `/content/package/${name}`),

        export: (name) => this.executeMappedJsonRequest(POST, `/content/package/${name}/export`),
        import: (name, options) => this.executeMappedJsonRequest(POST, `/content/package/${name}/import`, null, options),
        upload: (name, file) => {
            const data = new FormData();
            data.append('fileBinaryData', file);
            return this.executeMappedFormRequest(POST, `/content/package/${name}/zip`, data);
        },
        download: (name) => this.executeBlobRequest(GET, `/content/package/${name}`),

        addEntity: (name, entityType, entityGlobalId, options) => this.executeMappedJsonRequest(PUT, `/content/package/${name}/${entityType}/${entityGlobalId}`, null, options),
        removeEntity: (name, entityType, entityGlobalId) => this.executeMappedJsonRequest(DELETE, `/content/package/${name}/${entityType}/${entityGlobalId}`),
    } as const;

    public dataSource: GCMSDataSourceAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/datasource', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/datasource', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/datasource/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/datasource/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/datasource/${id}`),

        hash: (id) => this.executeMappedJsonRequest(GET, `/datasource/${id}/hash`),
        listConstructs: (id, options) => this.executeMappedJsonRequest(GET, `/datasource/${id}/constructs`, null, options),

        listEntries: (dsId) => this.executeMappedJsonRequest(GET, `/datasource/${dsId}/entries`),
        updateEntries: (dsId, body) => this.executeMappedJsonRequest(PUT, `/datasource/${dsId}/entries`, body),
        createEntry: (dsId, body) => this.executeMappedJsonRequest(POST, `/datasource/${dsId}/entries`, body),
        getEntry: (dsId, entryId) => this.executeMappedJsonRequest(GET, `/datasource/${dsId}/entries/${entryId}`),
        deleteEntry: (dsId, entryId) => this.executeMappedJsonRequest(DELETE, `/datasource/${dsId}/entries/${entryId}`),
    } as const;

    public devTools: GCMSDevToolsAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/devtools/packages', null, options),
        create: (name) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}`),
        get: (name) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}`),
        delete: (name) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}`),

        syncStatus: () => this.executeMappedJsonRequest(GET, '/devtools/sync'),
        startSync: () => this.executeMappedJsonRequest(PUT, '/devtools/sync'),
        stopSync: () => this.executeMappedJsonRequest(DELETE, '/devtools/sync'),
        syncToFileSystem: (name, options) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/cms2fs`, null, options),
        syncFromFileSystem: (name, options) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/fs2cms`, null, options),

        preview: (uuid) => this.executeRawRequest(GET, `/devtools/preview/${uuid}`),
        livePreview: (uuid) => this.executeRawRequest(GET, `/devtools/preview/page/${uuid}`),

        listFromNodes: (nodeId, options) => this.executeMappedJsonRequest(GET, `/devtools/nodes/${nodeId}/packages`, null, options),
        assignToNode: (name, nodeId) => this.executeMappedJsonRequest(PUT, `/devtools/nodes/${nodeId}/packages/${name}`),
        unassignFromNode: (name, nodeId) => this.executeMappedJsonRequest(DELETE, `/devtools/nodes/${nodeId}/packages/${name}`),

        listConstructs: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/constructs`, null, options),
        assignConstruct: (name, constructId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/constructs/${constructId}`),
        unassignConstruct: (name, constructId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/constructs/${constructId}`),

        listContentRepositories: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/objectproperties`, null, options),
        assignContentRepository: (name, crId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/objectproperties/${crId}`),
        unassignContentRepository: (name, crId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/objectproperties/${crId}`),

        listContentRepositoryFragments: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/cr_fragments`, null, options),
        assignContentRepositoryFragment: (name, crfId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/cr_fragments/${crfId}`),
        unassignContentRepositoryFragment: (name, crfId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/cr_fragments/${crfId}`),

        listDataSources: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/datasources`, null, options),
        assignDataSource: (name, dsId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/datasources/${dsId}`),
        unassignDataSource: (name, dsId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/datasource/${dsId}`),

        listObjectProperties: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/objectproperties`, null, options),
        assignObjectProperty: (name, opId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/objectproperties/${opId}`),
        unassignObjectProperty: (name, opId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/objectproperties/${opId}`),

        listTemplates: (name, options) => this.executeMappedJsonRequest(GET, `/devtools/packages/${name}/templates`, null, options),
        assignTemplate: (name, templateId) => this.executeMappedJsonRequest(PUT, `/devtools/packages/${name}/templates/${templateId}`),
        unassignTemplate: (name, templateId) => this.executeMappedJsonRequest(DELETE, `/devtools/packages/${name}/templates/${templateId}`),
    } as const;

    public elasticSearch: GCMSElasticSearchAPI = {
        search: (type, body, options) => this.executeMappedJsonRequest(POST, `/elastic/${type}/_search`, body, options),
    } as const;

    public file: GCMSFileAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/file', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/file/create', body),
        upload: (file, options, fileName) => {
            const data = new FormData();
            data.append('fileBinaryData', file);
            data.append('fileName', fileName);
            if (options.folderId) {
                data.append('folderId', options.folderId.toString());
            }
            if (options.nodeId) {
                data.append('nodeId', options.nodeId.toString());
            }
            return this.executeMappedFormRequest(POST, '/file/create', data, options);
        },
        uploadFromURL: (body) => this.executeMappedJsonRequest(POST, '/file/create', body),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/file/load/${id}`, null, options),
        getMultiple: (body) => this.executeMappedJsonRequest(POST, '/file/load', body),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/file/save/${id}`, body),
        uploadTo: (id, file, fileName, options) => {
            const data = new FormData();
            data.append('fileBinaryData', file);
            if (fileName) {
                data.append('fileName', fileName);
            }
            if (options.folderId) {
                data.append('folderId', options.folderId.toString());
            }
            if (options.nodeId) {
                data.append('nodeId', options.nodeId.toString())
            }
            return this.executeMappedFormRequest(POST, `/file/save/${id}`, data, options);
        },
        delete: (id, options) => this.executeMappedJsonRequest(POST, `/file/delete/${id}`, null, options),

        copy: (body) => this.executeMappedJsonRequest(POST, '/file/copy', body),
        move: (id, body) => this.executeMappedJsonRequest(POST, `/file/move/${id}`, body),
        moveMultiple: (body) => this.executeMappedJsonRequest(POST, '/file/move', body),

        inheritanceStatus: (id, options) => this.executeMappedJsonRequest(GET, `/file/disinherit/${id}`, null, options),
        multipleInheritanceStatus: (options) => this.executeMappedJsonRequest(GET, '/file/disinherit', null, options),
        inherit: (id, body) => this.executeMappedJsonRequest(POST, `/file/disinherit/${id}`, body),
        inheritMultiple: (body) => this.executeMappedJsonRequest(POST, '/file/disinherit', body),
        pushToMaster: (id, body) => this.executeMappedJsonRequest(POST, `/file/push2master/${id}`, body),
        pushMultipleToMaster: (body) => this.executeMappedJsonRequest(POST, '/file/push2master', body),

        localizationInfo: (id, options) => this.executeMappedJsonRequest(GET, `/file/localizationinfo/${id}`, null, options),
        multipleLocalizationInfos: (options) => this.executeMappedJsonRequest(GET, '/file/localizationinfo', null, options),
        listLocalizations: (id) => this.executeMappedJsonRequest(GET, `/file/localizations/${id}`),
        localize: (id, body) => this.executeMappedJsonRequest(POST, `/file/localize/${id}`, body),
        unlocalize: (id, body) => this.executeMappedJsonRequest(POST, `/file/unlocalize/${id}`, body),
        unlocalizeMultiple: (body) => this.executeMappedJsonRequest(POST, '/file/unlocalize', body),

        restoreFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/file/wastebin/restore/${id}`, null, options),
        restoreMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/file/wastebin/restore', body),
        deleteFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/file/wastebin/delete/${id}`, null, options),
        deleteMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/file/wastebin/delete', body),

        usageInFiles: (options) => this.executeMappedJsonRequest(GET, '/file/usage/file', null, options),
        usageInImages: (options) => this.executeMappedJsonRequest(GET, '/file/usage/image', null, options),
        usageInFolders: (options) => this.executeMappedJsonRequest(GET, '/file/usage/folder', null, options),
        usageInPages: (options) => this.executeMappedJsonRequest(GET, '/file/usage/page', null, options),
        usageInTemplates: (options) => this.executeMappedJsonRequest(GET, '/file/usage/template', null, options),
        usageInSyncableObject: (options) => this.executeMappedJsonRequest(GET, '/file/usage/syncableObjects', null, options),
        usageInTotal: (options) => this.executeMappedJsonRequest(GET, '/file/usage/total', null, options),
    } as const;

    public folder: GCMSFolderAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/folder', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/folder/create', body),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/folder/load/${id}`, null, options),
        getMultiple: (body) => this.executeMappedJsonRequest(POST, '/folder/load', body),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/folder/save/${id}`, body),
        delete: (id, options) => this.executeMappedJsonRequest(POST, `/folder/delete/${id}`, options),

        move: (id, body) => this.executeMappedJsonRequest(POST, `/folder/move/${id}`, body),
        moveMultiple: (body) => this.executeMappedJsonRequest(POST, '/folder/move', body),

        externalLinks: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getExternalLinks/${id}`, null, options),
        breadcrumbs: (id, options) => this.executeMappedJsonRequest(GET, `/folder/breadcrumb/${id}`, null, options),
        files: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getFiles/${id}`, null, options),
        folders: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getFolders/${id}`, null, options),
        images: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getImages/${id}`, null, options),
        items: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getItems/${id}`, null, options),
        pages: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getPages/${id}`, null, options),
        templates: (id, options) => this.executeMappedJsonRequest(GET, `/folder/getTemplates/${id}`, null, options),

        setStartpage: (id, body) => this.executeMappedJsonRequest(POST, `/folder/startpage/${id}`, body),
        sanitizePublshDirectory: (body) => this.executeMappedJsonRequest(POST, '/folder/sanitize/publishDir', body),

        inheritanceStatus: (id, options) => this.executeMappedJsonRequest(GET, `/folder/disinherit/${id}`, null, options),
        multipleInheritanceStatus: (options) => this.executeMappedJsonRequest(GET, '/folder/disinherit', null, options),
        inherit: (id, body) => this.executeMappedJsonRequest(POST, `/folder/disinherit/${id}`, body),
        inheritMultiple: (body) => this.executeMappedJsonRequest(POST, '/folder/disinherit', body),
        pushToMaster: (id, body) => this.executeMappedJsonRequest(POST, `/folder/push2master/${id}`, body),
        pushMultipleToMaster: (body) => this.executeMappedJsonRequest(POST, '/folder/push2master', body),

        localizationInfo: (id, options) => this.executeMappedJsonRequest(GET, `/folder/localizationinfo/${id}`, null, options),
        multipleLocalizationInfos: (options) => this.executeMappedJsonRequest(GET, '/folder/localizationinfo', null, options),
        listLocalizations: (id) => this.executeMappedJsonRequest(GET, `/folder/localizations/${id}`),
        localize: (id, body) => this.executeMappedJsonRequest(POST, `/folder/localize/${id}`, body),
        unlocalize: (id, body) => this.executeMappedJsonRequest(POST, `/folder/unlocalize/${id}`, body),
        unlocalizeMultiple: (body) => this.executeMappedJsonRequest(POST, '/folder/unlocalize', body),

        restoreFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/folder/wastebin/restore/${id}`, null, options),
        restoreMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/folder/wastebin/restore', body),
        deleteFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/folder/wastebin/delete/${id}`, null, options),
        deleteMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/folder/wastebin/delete', body),

        usageInSyncableObject: (options) => this.executeMappedJsonRequest(GET, '/folder/usage/syncableObjects', null, options),
    } as const;

    public form: GCMSFormAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/form', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/form', body),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/form/${id}`, null, options),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/form/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/form/${id}`),

        move: (id, folderId) => this.executeMappedJsonRequest(PUT, `/form/${id}/folder/${folderId}`),

        lock: (id) => this.executeMappedJsonRequest(PUT, `/form/${id}/lock`),
        unlock: (id) => this.executeMappedJsonRequest(DELETE, `/form/${id}/lock`),
        publish: (id, options) => this.executeMappedJsonRequest(PUT, `/form/${id}/online`, null, options),
        unpublish: (id, options) => this.executeMappedJsonRequest(DELETE, `/form/${id}/online`, null, options),
        removeScheduledPublish: (id) => this.executeMappedJsonRequest(DELETE, `/form/${id}/online`),
        removeScheduledUnpublish: (id) => this.executeMappedJsonRequest(DELETE, `/form/${id}/offline`),

        exportStatus: (id) => this.executeMappedJsonRequest(GET, `/form/${id}/export/status`),
        createExport: (id) => this.executeMappedJsonRequest(POST, `/form/${id}/export`),
        binariesStatus: (id) => this.executeMappedJsonRequest(GET, `/form/${id}/binaries/status`),
        createBinaries: (id) => this.executeMappedJsonRequest(POST, `/form/${id}/binaries`),
        downloadData: (id, downloadUuid) => this.executeBlobRequest(GET, `/form/${id}/download/${downloadUuid}`),

        listVersions: (id) => this.executeMappedJsonRequest(GET, `/form/${id}/version`),
        getVersion: (id, version) => this.executeMappedJsonRequest(GET, `/form/${id}/version/${version}`),

        previewSaved: (id, language) => this.executeRawRequest(GET, `/form/${id}/preview/${language}`),
        previewModel: (id, language, body) => this.executeRawRequest(POST, `/form/${id}/preview/${language}`, {
            [HTTP_HEADER_CONTENT_TYPE]: CONTENT_TYPE_JSON,
        }, parseJSONSafe(body)),

        listData: (id, options) => this.executeMappedJsonRequest(GET, `/form/${id}/data`, null, options),
        getData: (id, dataUuid) => this.executeMappedJsonRequest(GET, `/form/${id}/data/${dataUuid}`),
        deleteData: (id, dataUuid) => this.executeMappedJsonRequest(DELETE, `/form/${id}/data/${dataUuid}`),
        getDataBinary: (id, dataUuid, binaryField) => this.executeMappedJsonRequest(GET, `/form/${id}/data/${dataUuid}/binary/${binaryField}`),

        restoreFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/form/wastebin/restore/${id}`, null, options),
        deleteFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/form/wastebin/delete/${id}`, null, options),
    } as const;

    public fum: GCMSFileUploadManipulatorAPI = {
        contents: (fileName) => this.executeMappedJsonRequest(GET, `/fum/${fileName}`),
        result: (fileName, body) => this.executeMappedJsonRequest(POST, `/fum/${fileName}`, body),
    } as const;

    public group: GCMSGroupAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/group', null, options),
        create: (id, body) => this.executeMappedJsonRequest(PUT, `/group/${id}/groups`, body),
        get: (id) => this.executeMappedJsonRequest(GET, `/group/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/group/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/group/${id}`),

        tree: (options) => this.executeMappedJsonRequest(GET, '/group/load', null, options),
        move: (id, target) => this.executeMappedJsonRequest(PUT, `/group/${id}/groups/${target}`),
        subGroups: (id, options) => this.executeMappedJsonRequest(GET, `/group/${id}/groups`, null, options),

        listUsers: (id, options) => this.executeMappedJsonRequest(GET, `/group/${id}/users`, null, options),
        createUser: (id, body) => this.executeMappedJsonRequest(PUT, `/group/${id}/users`, body),
        assignUser: (id, userId) => this.executeMappedJsonRequest(PUT, `/group/${id}/users/${userId}`),
        unassignUser: (id, userId) => this.executeMappedJsonRequest(DELETE, `/group/${id}/users/${userId}`),

        listPermissions: (id, options) => this.executeMappedJsonRequest(GET, `/group/${id}/perms`, null, options),
        getPermission: (id, perm) => this.executeMappedJsonRequest(GET, `/group/${id}/perms/${perm}`),
        setPermission: (id, perm, body) => this.executeMappedJsonRequest(POST, `/group/${id}/perms/${perm}`, body),
        getInstancePermission: (id, perm, instanceId) => this.executeMappedJsonRequest(GET, `/group/${id}/perms/${perm}/${instanceId}`),
        setInstancePermission: (id, perm, instanceId, body) => this.executeMappedJsonRequest(GET, `/group/${id}/perms/${perm}/${instanceId}`, body),
    } as const;

    public i18n: GCMSI18nAPI = {
        listLanguages: () => this.executeMappedJsonRequest(GET, '/i18n/list'),
        getLanguage: () => this.executeMappedJsonRequest(GET, '/i18n/get'),
        setLanguage: (body) => this.executeMappedJsonRequest(POST, '/i18n/set', body),
        translate: (options) => this.executeMappedJsonRequest(GET, '/i18n/t', null, options),
    } as const;

    public image: GCMSImageAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/image', null, options),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/image/load/${id}`, null, options),
        getMultiple: (body) => this.executeMappedJsonRequest(POST, '/image/load', body),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/image/save/${id}`, body),
        delete: (id, options) => this.executeMappedJsonRequest(DELETE, `/image/delete/${id}`, null, options),

        move: (id, body) => this.executeMappedJsonRequest(POST, `/image/move/${id}`, body),
        moveMultiple: (body) => this.executeMappedJsonRequest(POST, '/image/move', body),

        resize: (body) => this.executeMappedJsonRequest(POST, '/image/resize', body),
        rotate: (body) => this.executeMappedJsonRequest(POST, '/image/rotate', body),

        inheritanceStatus: (id, options) => this.executeMappedJsonRequest(GET, `/image/disinherit/${id}`, null, options),
        multipleInheritanceStatus: (options) => this.executeMappedJsonRequest(GET, '/image/disinherit', null, options),
        inherit: (id, body) => this.executeMappedJsonRequest(POST, `/image/disinherit/${id}`, body),
        inheritMultiple: (body) => this.executeMappedJsonRequest(POST, '/image/disinherit', body),
        pushToMaster: (id, body) => this.executeMappedJsonRequest(POST, `/image/push2master/${id}`, body),
        pushMultipleToMaster: (body) => this.executeMappedJsonRequest(POST, '/image/push2master', body),

        localizationInfo: (id, options) => this.executeMappedJsonRequest(GET, `/image/localizationinfo/${id}`, null, options),
        multipleLocalizationInfos: (options) => this.executeMappedJsonRequest(GET, '/image/localizationinfo', null, options),
        listLocalizations: (id) => this.executeMappedJsonRequest(GET, `/image/localizations/${id}`),
        localize: (id, body) => this.executeMappedJsonRequest(POST, `/image/localize/${id}`, body),
        unlocalize: (id, body) => this.executeMappedJsonRequest(POST, `/image/unlocalize/${id}`, body),
        unlocalizeMultiple: (body) => this.executeMappedJsonRequest(POST, '/image/unlocalize', body),

        restoreFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/image/wastebin/restore/${id}`, null, options),
        restoreMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/image/wastebin/restore', body),
        deleteFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/image/wastebin/delete/${id}`, null, options),
        deleteMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/image/wastebin/delete', body),

        usageInFiles: (options) => this.executeMappedJsonRequest(GET, '/image/usage/file', null, options),
        usageInImages: (options) => this.executeMappedJsonRequest(GET, '/image/usage/image', null, options),
        usageInFolders: (options) => this.executeMappedJsonRequest(GET, '/image/usage/folder', null, options),
        usageInPages: (options) => this.executeMappedJsonRequest(GET, '/image/usage/page', null, options),
        usageInTemplates: (options) => this.executeMappedJsonRequest(GET, '/image/usage/template', null, options),
        usageInSyncableObject: (options) => this.executeMappedJsonRequest(GET, '/image/usage/syncableObjects', null, options),
        usageInTotal: (options) => this.executeMappedJsonRequest(GET, '/image/usage/total', null, options),
    } as const;

    public info: GCMSInfoAPI = {
        getMaintenanceMode: () => this.executeMappedJsonRequest(GET, '/info/maintenance'),
    };

    public language: GCMSLanguageAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/language', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/language', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/language/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/language/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/language/${id}`),
    } as const;

    public linkChecker: GCMSLinkCheckerAPI = {
        check: (body) => this.executeMappedJsonRequest(POST, '/linkChecker/check', body),
        pages: (options) => this.executeMappedJsonRequest(GET, '/linkChecker/pages', null, options),
        pageLinks: (pageId) => this.executeMappedJsonRequest(GET, `/linkChecker/pages/${pageId}`),
        pageStatus: (pageId, body) => this.executeMappedJsonRequest(POST, `/linkChecker/pages/${pageId}`, body),
        link: (pageId, linkId) => this.executeMappedJsonRequest(GET, `/linkChecker/pages/${pageId}/links/${linkId}`),
        replace: (pageId, linkId, body) => this.executeMappedJsonRequest(POST, `/linkChecker/pages/${pageId}/links/${linkId}`, body),
    } as const;

    public markupLanguage: GCMSMarkupLanguageAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/markupLanguages', null, options),
    } as const;

    public message: GCMSMessagingAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/msg/list', null, options),
        markAsRead: (body) => this.executeMappedJsonRequest(POST, '/msg/read', body),
        send: (body) => this.executeMappedJsonRequest(POST, '/msg/send', body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/msg/${id}`),
    } as const;

    public node: GCMSNodeAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/node', null, options),
        create: (body) => this.executeMappedJsonRequest(PUT, '/node', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/node/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/node/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/node/${id}`),

        copy: (id, body, options) => this.executeMappedJsonRequest(POST, `/node/${id}/copy`, body, options),
        settings: (id) => this.executeMappedJsonRequest(GET, `/node/${id}/settings`),

        listFeatures: (options) => this.executeMappedJsonRequest(GET, '/node/features', null, options),
        instanceFeatures: (id, options) => this.executeMappedJsonRequest(GET, `/node/${id}/features`, null, options),
        activateFeature: (id, feature) => this.executeMappedJsonRequest(PUT, `/node/${id}/features/${feature}`),
        deactivateFeature: (id, feature) => this.executeMappedJsonRequest(DELETE, `/node/${id}/features/${feature}`),

        listLanguages: (id, options) => this.executeMappedJsonRequest(GET, `/node/${id}/languages`, null, options),
        assignLanguage: (id, languageId) => this.executeMappedJsonRequest(PUT, `/node/${id}/languages/${languageId}`),
        unassignLanguage: (id, languageId) => this.executeMappedJsonRequest(DELETE, `/node/${id}/languages/${languageId}`),
        orderLanguages: (id, body) => this.executeMappedJsonRequest(POST, `/node/${id}/languages`, body),

        listTemplates: (id, options) => this.executeMappedJsonRequest(GET, `/node/${id}/templates`, null, options),
        assignTemplate: (id, templateId) => this.executeMappedJsonRequest(PUT, `/node/${id}/templates/${templateId}`),
        unassignTemplate: (id, templateId) => this.executeMappedJsonRequest(DELETE, `/node/${id}/templates/${templateId}`),

        listConstructs: (id, options) => this.executeMappedJsonRequest(GET, `/node/${id}/constructs`, null, options),
        assignConstruct: (id, constructId) => this.executeMappedJsonRequest(PUT, `/node/${id}/constructs/${constructId}`),
        unassignConstruct: (id, constructId) => this.executeMappedJsonRequest(DELETE, `/node/${id}/constructs/${constructId}`),

        listObjectProperties: (id, options) => this.executeMappedJsonRequest(GET, `/node/${id}/objectproperties`, null, options),
        assignObjectProperty: (id, opId) => this.executeMappedJsonRequest(PUT, `/node/${id}/objectproperties/${opId}`),
        unassignObjectProperty: (id, opId) => this.executeMappedJsonRequest(DELETE, `/node/${id}/objectproperties/${opId}`),
    } as const;

    public objectProperty: GCMSObjectPropertyAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/objectproperty', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/objectproperty', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/objectproperty/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/objectproperty/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/objectproperty/${id}`),

        constructs: (id) => this.executeMappedJsonRequest(GET, `/objectproperty/${id}/constructs`),
        listNodes: (id) => this.executeMappedJsonRequest(GET, `/objectproperty/${id}/nodes`),
        linkToNode: (body) => this.executeMappedJsonRequest(POST, '/objectproperty/link/nodes', body),
        unlinkFromNode: (body) => this.executeMappedJsonRequest(POST, '/objectproperty/unlink/nodes', body),
        hash: (id) => this.executeMappedJsonRequest(GET, `/objectproperty/${id}/hash`),
    } as const;

    public objectPropertyCategory: GCMSObjectPropertyCategoryAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/objectproperty/category', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/objectproperty/category', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/objectproperty/category/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/objectproperty/category/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/objectproperty/category/${id}`),
    } as const;

    public page: GCMSPageAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/page', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/page/create', body),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/page/load/${id}`, null, options),
        getMultiple: (body) => this.executeMappedJsonRequest(POST, '/page/load', body),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/page/save/${id}`, body),
        delete: (id, options) => this.executeMappedJsonRequest(DELETE, `/page/delete/${id}`, null, options),

        copy: (body) => this.executeMappedJsonRequest(POST, '/page/copy', body),
        move: (id, body) => this.executeMappedJsonRequest(POST, `/page/move/${id}`, body),
        moveMultiple: (body) => this.executeMappedJsonRequest(POST, '/page/move', body),
        restoreVersion: (id, options) => this.executeMappedJsonRequest(POST, `/page/restore/${id}`, null, options),
        search: (options) => this.executeMappedJsonRequest(GET, '/page/search', null, options),

        preview: (body) => this.executeMappedJsonRequest(POST, '/page/preview', body),
        render: (body, options) => this.executeMappedJsonRequest(POST, '/page/render', body, options),
        renderTag: (id, keyword, body, options) => this.executeMappedJsonRequest(POST, `/page/renderTag/${id}/${keyword}`, body, options),

        suggestFileName: (body) => this.executeMappedJsonRequest(POST, '/page/suggest/filename', body),
        cancelEdit: (id, options) => this.executeMappedJsonRequest(POST, `/page/cancel/${id}`, null, options),
        assign: (body) => this.executeMappedJsonRequest(POST, '/page/assign', body),
        translate: (id, options) => this.executeMappedJsonRequest(POST, `/page/translate/${id}`, null, options),

        publish: (id, body, options) => this.executeMappedJsonRequest(POST, `/page/publish/${id}`, body, options),
        publishMultiple: (body, options) => this.executeMappedJsonRequest(POST, '/page/publish', body, options),
        takeOffline: (id, body, options) => this.executeMappedJsonRequest(POST, `/page/takeOffline/${id}`, body, options),
        publishQueueApprove: (body) => this.executeMappedJsonRequest(POST, '/page/pubqueue/approve', body),

        listTags: (id, options) => this.executeMappedJsonRequest(GET, `/page/getTags/${id}`, null, options),
        createTag: (id, body) => this.executeMappedJsonRequest(POST, `/page/newtag/${id}`, body),
        createMultipleTags: (id, body) => this.executeMappedJsonRequest(POST, `/page/newtags/${id}`, body),
        restoreTag: (id, keyword, options) => this.executeMappedJsonRequest(POST, `/page/restoreTag/${id}/${keyword}`, null, options),

        workflowDecline: (id) => this.executeMappedJsonRequest(POST, `/page/workflow/decline/${id}`),
        workflowRevoke: (id) => this.executeMappedJsonRequest(POST, `/page/workflow/revoke/${id}`),

        inheritanceStatus: (id, options) => this.executeMappedJsonRequest(GET, `/page/disinherit/${id}`, null, options),
        multipleInheritanceStatus: (options) => this.executeMappedJsonRequest(GET, '/page/disinherit', null, options),
        inherit: (id, body) => this.executeMappedJsonRequest(POST, `/page/disinherit/${id}`, body),
        inheritMultiple: (body) => this.executeMappedJsonRequest(POST, '/page/disinherit', body),
        pushToMaster: (id, body) => this.executeMappedJsonRequest(POST, `/page/push2master/${id}`, body),
        pushMultipleToMaster: (body) => this.executeMappedJsonRequest(POST, '/page/push2master', body),

        localizationInfo: (id, options) => this.executeMappedJsonRequest(GET, `/page/localizationinfo/${id}`, null, options),
        multipleLocalizationInfos: (options) => this.executeMappedJsonRequest(GET, '/page/localizationinfo', null, options),
        listLocalizations: (id) => this.executeMappedJsonRequest(GET, `/page/localizations/${id}`),
        localize: (id, body) => this.executeMappedJsonRequest(POST, `/page/localize/${id}`, body),
        unlocalize: (id, body) => this.executeMappedJsonRequest(POST, `/page/unlocalize/${id}`, body),
        unlocalizeMultiple: (body) => this.executeMappedJsonRequest(POST, '/page/unlocalize', body),

        restoreFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/page/wastebin/restore/${id}`, null, options),
        restoreMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/page/wastebin/restore', body),
        deleteFromWastebin: (id, options) => this.executeMappedJsonRequest(POST, `/page/wastebin/delete/${id}`, null, options),
        deleteMultipleFromWastebin: (body) => this.executeMappedJsonRequest(POST, '/page/wastebin/delete', body),

        usageInLinkedFiles: (options) => this.executeMappedJsonRequest(GET, '/page/usage/linkedFile', null, options),
        usageInLinkedImages: (options) => this.executeMappedJsonRequest(GET, '/page/usage/linkedImage', null, options),
        usageInLinkedPages: (options) => this.executeMappedJsonRequest(GET, '/page/usage/linkedPage', null, options),
        usageInPages: (options) => this.executeMappedJsonRequest(GET, '/page/usage/page', null, options),
        usageInTags: (options) => this.executeMappedJsonRequest(GET, '/page/usage/tag', null, options),
        usageInTemplates: (options) => this.executeMappedJsonRequest(GET, '/page/usage/template', null, options),
        usageInTotal: (options) => this.executeMappedJsonRequest(GET, '/page/usage/total', null, options),
        usageInVariants: (options) => this.executeMappedJsonRequest(GET, '/page/usage/variant', null, options),

        usageInSyncableObject: (options) => this.executeMappedJsonRequest(GET, '/page/usage/syncableObjects', null, options),
    } as const;

    public partType: GCMSPartTypeAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/parttype', null, options),
    } as const;

    public permission: GCMSPermissionAPI = {
        getType: (type, options) => this.executeMappedJsonRequest(GET, `/perm/${type}`, null, options),
        setType: (type, body, options) => this.executeMappedJsonRequest(POST, `/perm/${type}`, body, options),
        typeGroups: (type) => this.executeMappedJsonRequest(GET, `/perm/list/${type}`),

        getInstance: (type, instanceId, options) => this.executeMappedJsonRequest(GET, `/perm/${type}/${instanceId}`, null, options),
        setInstance: (type, instanceId, body, options) => this.executeMappedJsonRequest(POST, `/perm/${type}/${instanceId}`, body, options),
        instanceGroups: (type, instanceId) => this.executeMappedJsonRequest(GET, `/perm/list/${type}/${instanceId}`),

        check: (perm, type, instanceId, options) => this.executeMappedJsonRequest(GET, `/perm/${perm}/${type}/${instanceId}`, null, options),
    } as const;

    public policyMap: GCMSPolicyMapAPI = {
        policy: (options) => this.executeMappedJsonRequest(GET, '/policyMap/policy', null, options),
        policyGroup: (type) => this.executeMappedJsonRequest(GET, `/policyMap/partType/${type}/policyGroup`),
    } as const;

    public publishProtocol: GCMSPublishProtocolAPI = {
        get: (type, objId: number) => this.executeMappedJsonRequest(GET, `/publish/state/${type}/${objId}`, null, null),
        list: (options: BaseListOptionsWithPaging<PublishLogEntry>) => this.executeMappedJsonRequest(GET, '/publish/state/', null, options),
    } as const;

    public role: GCMSRoleAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/role', null, options),
        create: (body) => this.executeMappedJsonRequest(PUT, '/role', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/role/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/role/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/role/${id}`),

        getPermissions: (id) => this.executeMappedJsonRequest(GET, `/role/${id}/perm`),
        setPermissions: (id, body) => this.executeMappedJsonRequest(POST, `/role/${id}/perm`, body),
    } as const;

    public scheduler: GCMSSchedulerAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/scheduler/schedule', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/scheduler/schedule', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/scheduler/schedule/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/scheduler/schedule/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/scheduler/schedule/${id}`),

        status: () => this.executeMappedJsonRequest(GET, '/scheduler/status'),
        suspend: (body) => this.executeMappedJsonRequest(PUT, '/scheduler/suspend', body),
        resume: () => this.executeMappedJsonRequest(PUT, '/scheduler/resume'),

        execute: (id) => this.executeMappedJsonRequest(POST, `/scheduler/schedule/${id}/execute`, {}),
        listExecutions: (id, options) => this.executeMappedJsonRequest(GET, `/scheduler/schedule/${id}/execution`, null, options),
        getExecution: (id) => this.executeMappedJsonRequest(GET, `/scheduler/execution/${id}`),
    } as const;

    public schedulerTask: GCMSScheduleTaskAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/scheduler/task', null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/scheduler/task', body),
        get: (id) => this.executeMappedJsonRequest(GET, `/scheduler/task/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/scheduler/task/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/scheduler/task/${id}`),
    } as const;

    public searchIndex: GCMSSearchIndexAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/index', null, options),

        rebuild: (name, options) => this.executeMappedJsonRequest(PUT, `/index/${name}/rebuild`, {}, options),
    } as const;

    public template: GCMSTemplateAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/template', null, options),
        get: (id, options) => this.executeMappedJsonRequest(GET, `/template/${id}`, null, options),
        create: (body) => this.executeMappedJsonRequest(POST, '/template', body),
        update: (id, body) => this.executeMappedJsonRequest(POST, `/template/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/template/${id}`),

        copy: (id, body) => this.executeMappedJsonRequest(POST, `/template/${id}/copy`, body),
        unlock: (id) => this.executeMappedJsonRequest(POST, `/template/${id}/unlock`),
        hash: (id) => this.executeMappedJsonRequest(GET, `/template/${id}/hash`),

        link: (id, body) => this.executeMappedJsonRequest(POST, `/template/link/${id}`, body),
        linkMultiple: (body) => this.executeMappedJsonRequest(POST, '/template/link', body),
        unlink: (id, body) => this.executeMappedJsonRequest(POST, `/template/${id}/unlink`, body),
        unlinkMultiple: (body) => this.executeMappedJsonRequest(POST, '/template/unlink', body),

        listTagStatus: (id, options) => this.executeMappedJsonRequest(GET, `/template/${id}/tagstatus`, null, options),
        listFolders: (id, options) => this.executeMappedJsonRequest(GET, `/template/${id}/folders`, null, options),
        listNodes: (id, options) => this.executeMappedJsonRequest(GET, `/template/${id}/nodes`, null, options),

        pushToMaster: (id, body) => this.executeMappedJsonRequest(POST, `/template/push2master/${id}`, body),
        pushMultipleToMaster: (body) => this.executeMappedJsonRequest(POST, '/template/push2master', body),

        localizationInfo: (id, options) => this.executeMappedJsonRequest(GET, `/template/localizationinfo/${id}`, null, options),
        multipleLocalizationInfos: (options) => this.executeMappedJsonRequest(GET, '/template/localizationinfo', null, options),
        listLocalizations: (id) => this.executeMappedJsonRequest(GET, `/template/localizations/${id}`),
        localize: (id, body) => this.executeMappedJsonRequest(POST, `/template/localize/${id}`, body),
        unlocalize: (id, body) => this.executeMappedJsonRequest(POST, `/template/unlocalize/${id}`, body),
        unlocalizeMultiple: (body) => this.executeMappedJsonRequest(POST, '/template/unlocalize', body),

        usageInSyncableObject: (options) => this.executeMappedJsonRequest(GET, '/template/usage/syncableObjects', null, options),
    } as const;

    public user: GCMSUserAPI = {
        list: (options) => this.executeMappedJsonRequest(GET, '/user', null, options),
        get: (id) => this.executeMappedJsonRequest(GET, `/user/${id}`),
        update: (id, body) => this.executeMappedJsonRequest(PUT, `/user/${id}`, body),
        delete: (id) => this.executeMappedJsonRequest(DELETE, `/user/${id}`),

        me: (options) => this.executeMappedJsonRequest(GET, '/user/me', null, options),
        getFullUserData: () => this.executeMappedJsonRequest(GET, '/user/me/data'),
        getUserData: (key) => this.executeMappedJsonRequest(GET, `/user/me/data/${key}`),
        setUserData: (key, body) => this.executeMappedJsonRequest(POST, `/user/me/data/${key}`, body),
        deleteUserData: (key) => this.executeMappedJsonRequest(DELETE, `/user/me/data/${key}`),

        listGroups: (id, options) => this.executeMappedJsonRequest(GET, `/user/${id}/groups`, null, options),
        assignToGroup: (id, groupId) => this.executeMappedJsonRequest(PUT, `/user/${id}/groups/${groupId}`),
        unassignFromGroup: (id, groupId) => this.executeMappedJsonRequest(DELETE, `/user/${id}/groups/${groupId}`),

        listNodeRestrictions: (id, groupId) => this.executeMappedJsonRequest(GET, `/user/${id}/groups/${groupId}/nodes`),
        addNodeRestriction: (id, groupId, nodeId) => this.executeMappedJsonRequest(PUT, `/user/${id}/groups/${groupId}/nodes/${nodeId}`),
        removeNodeRestriction: (id, groupId, nodeId) => this.executeMappedJsonRequest(DELETE, `/user/${id}/groups/${groupId}/nodes/${nodeId}`),
    } as const;

    public usersnap: GCMSUsersnapAPI = {
        getUsersnapSettings: () => this.executeMappedJsonRequest(GET, '/usersnap'),
    } as const;

    public validation: GCMSValidationAPI = {

    } as const;

    public translation: GCMSTranslationAPI = {
        translateText: (body) => this.executeMappedJsonRequest(POST, 'translation/text', body),
        translatePage: (pageId, params) => this.executeMappedJsonRequest(POST, `translation/page/${pageId}/`, null, params),
    } as const;

    public license: GCMSLicenseAPI = {
        info: () => this.executeMappedJsonRequest(GET, 'license/info'),
        update: (body) => this.executeMappedJsonRequest(POST, 'license/update', body),
        contentRepositories: (options) => this.executeMappedJsonRequest(GET, 'license/contentRepositories', null, options),
        push: (body) => this.executeMappedJsonRequest(POST, 'license/push', body),
    }
}
