import { CONTENT_TYPE_JSON, DELETE, GET, HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_CONTENT_TYPE, POST } from './internal';
import {
    MeshAPIVersion,
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClientDriver,
    MeshClusterAPI,
    MeshCoordinatorAPI,
    MeshGraphQLAPI,
    MeshGroupAPI,
    MeshMicroschemaAPI,
    MeshNodeAPI,
    MeshPermissionAPI,
    MeshPluginAPI,
    MeshProjectAPI,
    MeshRestClientConfig,
    MeshRestClientInterceptorData,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshServerAPI,
    MeshTagFamiliesAPI,
    MeshTagsAPI,
    MeshUserAPI,
    RequestMethod
} from './models';
import { toRelativePath, trimTrailingSlash } from './utils';

export class MeshRestClient {

    constructor(
        public driver: MeshClientDriver,
        public config: MeshRestClientConfig,
        public apiKey?: string,
    ) { }

    protected createUrl(
        path: string,
        queryParams: Record<string, any>,
    ): string {
        let buildPath = '';

        if (this.config.connection.basePath) {
            buildPath += trimTrailingSlash(toRelativePath(this.config.connection.basePath));
        } else {
            const version = this.config.connection.version ?? MeshAPIVersion.V2;
            buildPath += `/api/${version}`;
        }
        buildPath += toRelativePath(path);

        const { protocol, host, port, path: finalPath, params } = this.handleInterceptors({
            protocol: this.config.connection.ssl ? 'https' : 'http',
            host: this.config.connection.host,
            port: this.config.connection.port,
            path: buildPath,
            params: queryParams,
        });

        let url = `${protocol}://${host}`;
        if (port) {
            url += `:${port}`;
        }
        url += finalPath;

        const buildParams = new URLSearchParams(params || {}).toString();
        if (params) {
            url += `?${buildParams}`;
        }

        return url;
    }

    protected handleInterceptors(data: MeshRestClientInterceptorData): MeshRestClientInterceptorData {
        const interceptors = this.config.interceptors || [];
        for (const handler of interceptors) {
            data = handler(data);
        }
        return data;
    }

    protected performReqeust<T>(
        method: RequestMethod,
        path: string,
        body?: null | any,
        queryParams?: Record<string, any>,
    ): Promise<T> {
        const url = this.createUrl(path, queryParams);
        const headers: Record<string, string> = {
            [HTTP_HEADER_CONTENT_TYPE]: CONTENT_TYPE_JSON,
        };

        if (this.apiKey) {
            headers[HTTP_HEADER_AUTHORIZATION] = `Bearer ${this.apiKey}`;
        }

        return this.driver.performJsonRequest(method, url, headers, body) as any;
    }

    public auth: MeshAuthAPI = {
        login: (body) => this.performReqeust(POST, '/auth/login', body),
        me: () => this.performReqeust(GET, '/auth/me'),
        logout: () => this.performReqeust(GET, '/auth/logout'),
    } as const;

    public users: MeshUserAPI = {
        list: (params?) => this.performReqeust(GET, '/users', null, params),
        create: (body) => this.performReqeust(POST, '/users', body),
        get: (uuid, params?) => this.performReqeust(GET, `/users/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/users/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/users/${uuid}`),

        createAPIToken: (uuid) => this.performReqeust(POST, `/users/${uuid}/token`),
    } as const;

    public roles: MeshRoleAPI = {
        list: (params?) => this.performReqeust(GET, '/roles', null, params),
        create: (body) => this.performReqeust(POST, '/roles', body),
        get: (uuid, params?) => this.performReqeust(GET, `/roles/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/roles/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/roles/${uuid}`),
    } as const;

    public groups: MeshGroupAPI = {
        list: (params?) => this.performReqeust(GET, '/groups', null, params),
        create: (body) => this.performReqeust(POST, '/groups', body),
        get: (uuid, params?) => this.performReqeust(GET, `/groups/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/groups/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/groups/${uuid}`),

        getRoles: (uuid, params?) => this.performReqeust(GET, `/groups/${uuid}`, null, params),
        assignRole: (uuid, roleUuid) => this.performReqeust(POST, `/groups/${uuid}/roles/${roleUuid}`),
        unassignRole: (uuid, roleUuid) => this.performReqeust(DELETE, `/groups/${uuid}/roles/${roleUuid}`),

        getUsers: (uuid, params?) => this.performReqeust(GET, `/groups/${uuid}/users`, null, params),
        assignUser: (uuid, userUuid) => this.performReqeust(POST, `/groups/${uuid}/users/${userUuid}`),
        unassignUser: (uuid, userUuid) => this.performReqeust(DELETE, `/groups/${uuid}/users/${userUuid}`),
    } as const;

    public permissions: MeshPermissionAPI = {
        get: (roleUuid, path) => this.performReqeust(GET, `/roles/${roleUuid}/permissions/${path}`),
        set: (roleUuid, path, body) => this.performReqeust(POST, `/roles/${roleUuid}/permissions/${path}`, body),

        check: (userUuid, path) => this.performReqeust(GET, `/user/${userUuid}/permissions/${path}`),
    } as const;

    public projects: MeshProjectAPI = {
        list: (params?) => this.performReqeust(GET, '/projects', null, params),
        create: (body) => this.performReqeust(POST, '/projects', body),
        get: (project, params?) => this.performReqeust(GET, `/projects/${project}`, null, params),
        update: (project, body) => this.performReqeust(POST, `/projects/${project}`, body),
        delete: (project) => this.performReqeust(DELETE, `/projects/${project}`),

        listSchemas: (project) => this.performReqeust(GET, `/${project}/schemas`),
        getSchema: (project, uuid) => this.performReqeust(GET, `/${project}/schemas/${uuid}`),
        assignSchema: (project, uuid) => this.performReqeust(POST, `/${project}/schemas/${uuid}`),
        unassignSchema: (project, uuid) => this.performReqeust(DELETE, `/${project}/schemas/${uuid}`),

        listMicroschemas: (project) => this.performReqeust(GET, `/${project}/microschemas`),
        getMicroschema: (project, uuid) => this.performReqeust(GET, `/${project}/microschemas/${uuid}`),
        assignMicroschema: (project, uuid) => this.performReqeust(POST, `/${project}/microschemas/${uuid}`),
        unassignMicroschema: (project, uuid) => this.performReqeust(DELETE, `/${project}/microschemas/${uuid}`),
    } as const;

    public schemas: MeshSchemaAPI = {
        list: (params?) => this.performReqeust(GET, '/schemas', null, params),
        create: (body) => this.performReqeust(POST, '/schemas', body),
        get: (uuid, params) => this.performReqeust(GET, `/schemas/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/schemas/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/schemas/${uuid}`),
        diff: (uuid, body) => this.performReqeust(POST, `/schemas/${uuid}/diff`, body),
        changes: (uuid, body) => this.performReqeust(POST, `/schemas/${uuid}/changes`, body),
    } as const;

    public microschemas: MeshMicroschemaAPI = {
        list: (params?) => this.performReqeust(GET, '/microschemas', null, params),
        create: (body) => this.performReqeust(POST, '/microschemas', body),
        get: (uuid, params?) => this.performReqeust(GET, `/microschemas/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/microschemas/${uuid}`),
        diff: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}/diff`, body),
        changes: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}/changes`, body),
    } as const;

    public nodes: MeshNodeAPI = {
        list: (project, params?) => this.performReqeust(GET, `/${project}/nodes`, null, params),
        create: (project, body) => this.performReqeust(POST, `/${project}/nodes`, body),
        get: (project, uuid, params?) => this.performReqeust(GET, `/${project}/nodes/${uuid}`, null, params),
        update: (project, uuid, body) => this.performReqeust(POST, `/${project}/nodes/${uuid}`, body),
        delete: (project, uuid, params?) => this.performReqeust(DELETE, `/${project}/nodes/${uuid}`, null, params),

        deleteLanguage: (project, uuid, language) => this.performReqeust(DELETE, `/${project}/nodes/${uuid}/languages/${language}`),
        children: (project, uuid, params?) => this.performReqeust(GET, `/${project}/nodes/${uuid}/children`, null, params),
        versions: (project, uuid) => this.performReqeust(GET, `/${project}/nodes/${uuid}/versions`),

        publishStatus: (project, uuid, language?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.performReqeust(GET, path);
        },
        publish: (project, uuid, language?, params?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.performReqeust(POST, path, null, params);
        },
        unpublish: (project, uuid, language?) => {
            const path = language
                ? `/${project}/nodes/${uuid}/languages/${language}/published`
                : `/${project}/nodes/${uuid}/published`;
            return this.performReqeust(DELETE, path);
        },

        listTags: (project, uuid) => this.performReqeust(GET, `/${project}/nodes/${uuid}/tags`),
        setTags: (project, uuid, body) => this.performReqeust(POST, `/${project}/nodes/${uuid}/tags`, body),
        assignTag: (project, uuid, tag) => this.performReqeust(POST, `/${project}/nodes/${uuid}/tags/${tag}`),
        removeTag: (project, uuid, tag) => this.performReqeust(DELETE, `/${project}/nodes/${uuid}/tags/${tag}`),
    } as const;

    public branches: MeshBranchAPI = {
        list: (project) => this.performReqeust(GET, `/${project}/branches`),
        create: (project, body) => this.performReqeust(POST, `/${project}/branches`, body),
        get: (project, uuid) => this.performReqeust(GET, `/${project}/branches/${uuid}`),
        update: (project, uuid, body) => this.performReqeust(POST, `/${project}/branches/${uuid}`, body),
        asLatest: (project, uuid) => this.performReqeust(POST, `/${project}/branches/${uuid}/latest`),
    } as const;

    public tagFamilies: MeshTagFamiliesAPI = {
        list: (project, params?) => this.performReqeust(GET, `/${project}/tagFamilies`, null, params),
        create: (project, body) => this.performReqeust(POST, `/${project}/tagFamilies`, body),
        get: (project, uuid, params?) => this.performReqeust(GET, `/${project}/tagFamilies/${uuid}`, null, params),
        update: (project, uuid, body) => this.performReqeust(POST, `/${project}/tagFamilies/${uuid}`, body),
        delete: (project, uuid) => this.performReqeust(DELETE, `/${project}/tagFamilies/${uuid}`),
    } as const;

    public tags: MeshTagsAPI = {
        list: (project, familyUuid, params?) => this.performReqeust(GET, `/${project}/tagFamilies/${familyUuid}/tags`, null, params),
        create: (project, familyUuid, body) => this.performReqeust(POST, `/${project}/tagFamilies/${familyUuid}/tags`, body),
        get: (project, familyUuid, uuid, params?) => this.performReqeust(GET, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`, null, params),
        update: (project, familyUuid, uuid, body) => this.performReqeust(POST, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`, body),
        delete: (project, familyUuid, uuid) => this.performReqeust(DELETE, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}`),

        nodes: (project, familyUuid, uuid, params?) => this.performReqeust(GET, `/${project}/tagFamilies/${familyUuid}/tags/${uuid}/nodes`, null, params),
    } as const;

    public server: MeshServerAPI = {
        info: () => this.performReqeust(GET, '/'),
        config: () => this.performReqeust(GET, '/admin/config'),
        status: () => this.performReqeust(GET, '/admin/status'),
    } as const;

    public coordinator: MeshCoordinatorAPI = {
        config: () => this.performReqeust(GET, '/admin/coordinator/config'),
        master: () => this.performReqeust(GET, '/admin/coordinator/master'),
    } as const;

    public cluster: MeshClusterAPI = {
        config: () => this.performReqeust(GET, '/admin/cluster/config'),
        status: () => this.performReqeust(GET, '/admin/cluster/status'),
    } as const;

    public plugins: MeshPluginAPI = {
        list: () => this.performReqeust(GET, '/admin/plugins'),
    } as const;

    public graphql: MeshGraphQLAPI = (project, body, params) => this.performReqeust(POST, `/${project}/graphql`, body, params);
}