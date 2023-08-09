import { CONTENT_TYPE_JSON, DELETE, GET, HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_CONTENT_TYPE, POST } from './internal';
import {
    MeshGraphQLAPI,
    MeshAPIVersion,
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClientConnection,
    MeshClientDriver,
    MeshClusterAPI,
    MeshCoordinatorAPI,
    MeshGroupAPI,
    MeshMicroschemaAPI,
    MeshNodeAPI,
    MeshPluginAPI,
    MeshProjectAPI,
    MeshProjectMicroschemaAPI,
    MeshProjectSchemaAPI,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshServerAPI,
    MeshUserAPI,
    RequestMethod,
    MeshPermissionAPI,
} from './models';
import { toRelativePath, trimTrailingSlash } from './utils';

export class MeshRestClient {

    constructor(
        public driver: MeshClientDriver,
        public config: MeshClientConnection,
        public apiKey?: string,
    ) {}

    protected createUrl(
        path: string,
        queryParams: Record<string, any>,
    ): string {
        const params = new URLSearchParams(queryParams || {}).toString();
        const protocol = this.config.ssl ? 'https' : 'http';
        let url = `${protocol}://${this.config.host}`;

        if (this.config.port) {
            url += `:${this.config.port}`;
        }

        if (this.config.basePath) {
            url += trimTrailingSlash(toRelativePath(this.config.basePath));
        } else {
            const version = this.config.version ?? MeshAPIVersion.V2;
            url += `/api/${version}`;
        }

        url += toRelativePath(path);

        if (params) {
            url += `?${params}`;
        }

        return url;
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
        login: (username, password) => this.performReqeust(POST, '/auth/login', { username, password }),
        me: () => this.performReqeust(GET, '/auth/me'),
    } as const;

    public users: MeshUserAPI = {
        list: (params?) => this.performReqeust(GET, '/users', null, params),
        create: (body) => this.performReqeust(POST, '/users', body),
        get: (uuid, params) => this.performReqeust(GET, `/users/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/users/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/users/${uuid}`),

        createAPIToken: (uuid) => this.performReqeust(POST, `/users/${uuid}/token`),
    } as const;

    public roles: MeshRoleAPI = {
        list: (params?) => this.performReqeust(GET, '/roles', null, params),
        create: (body) => this.performReqeust(POST, '/roles', body),
        get: (uuid, params) => this.performReqeust(GET, `/roles/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/roles/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/roles/${uuid}`),
    } as const;

    public groups: MeshGroupAPI = {
        list: (params?) => this.performReqeust(GET, '/groups', null, params),
        create: (body) => this.performReqeust(POST, '/groups', body),
        get: (uuid, params) => this.performReqeust(GET, `/groups/${uuid}`, null, params),
        update: (uuid, body) => this.performReqeust(POST, `/groups/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/groups/${uuid}`),

        getRoles: (uuid, params) => this.performReqeust(GET, `/groups/${uuid}`, null, params),
        assignRole: (uuid, roleUuid) => this.performReqeust(POST, `/groups/${uuid}/roles/${roleUuid}`),
        unassignRole: (uuid, roleUuid) => this.performReqeust(DELETE, `/groups/${uuid}/roles/${roleUuid}`),

        getUsers: (uuid, params) => this.performReqeust(GET, `/groups/${uuid}/users`, null, params),
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
        get: (project) => this.performReqeust(GET, `/projects/${project}`),
        update: (project, body) => this.performReqeust(POST, `/projects/${project}`, body),
        delete: (project) => this.performReqeust(DELETE, `/projects/${project}`),

        listSchemas: (project) => this.performReqeust(GET, `/projects/${project}/schemas`),
        getSchema: (project, uuid) => this.performReqeust(GET, `/projects/${project}/schemas/${uuid}`),
        assignSchema: (project, uuid) => this.performReqeust(POST, `/projects/${project}/schemas/${uuid}`),
        unassignSchema: (project, uuid) => this.performReqeust(DELETE, `/projects/${project}/schemas/${uuid}`),

        listMicroschemas: (project) => this.performReqeust(GET, `/projects/${project}/microschemas`),
        getMicroschema: (project, uuid) => this.performReqeust(GET, `/projects/${project}/microschemas/${uuid}`),
        assignMicroschema: (project, uuid) => this.performReqeust(POST, `/projects/${project}/microschemas/${uuid}`),
        unassignMicroschema: (project, uuid) => this.performReqeust(DELETE, `/projects/${project}/microschemas/${uuid}`),
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
        get: (uuid) => this.performReqeust(GET, `/microschemas/${uuid}`),
        update: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}`, body),
        delete: (uuid) => this.performReqeust(DELETE, `/microschemas/${uuid}`),
        diff: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}/diff`, body),
        changes: (uuid, body) => this.performReqeust(POST, `/microschemas/${uuid}/changes`, body),
    } as const;

    public nodes: MeshNodeAPI = {
        list: (project, params?) => this.performReqeust(GET, `/${project}/nodes`, null, params),
        create: (project, body) => this.performReqeust(POST, `/${project}/nodes`, body),
        get: (project, uuid) => this.performReqeust(GET, `/${project}/nodes/${uuid}`),
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

    public projectSchemas: MeshProjectSchemaAPI = {
        list: (project) => this.performReqeust(GET, `/${project}/schemas`),
        get: (project, uuid) => this.performReqeust(GET, `/${project}/schemas/${uuid}`),
        assign: (project, uuid) => this.performReqeust(POST, `/${project}/schemas/${uuid}`),
        unassign: (project, uuid) => this.performReqeust(DELETE, `/${project}/schemas/${uuid}`),
    } as const;

    public projectMicroschemas: MeshProjectMicroschemaAPI = {
        list: (project) => this.performReqeust(GET, `/${project}/microschemas`),
        get: (project, uuid) => this.performReqeust(GET, `/${project}/microschemas/${uuid}`),
        assign: (project, uuid) => this.performReqeust(POST, `/${project}/microschemas/${uuid}`),
        unassign: (project, uuid) => this.performReqeust(DELETE, `/${project}/microschemas/${uuid}`),
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
