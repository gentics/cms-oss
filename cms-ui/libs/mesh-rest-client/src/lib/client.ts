import { CONTENT_TYPE_JSON, DELETE, GET, HTTP_HEADER_AUTHORIZATION, HTTP_HEADER_CONTENT_TYPE, POST } from './internal';
import {
    MeshAPIVersion,
    MeshAuthAPI,
    MeshBranchAPI,
    MeshClientConnection,
    MeshClientDriver,
    MeshGroupAPI,
    MeshMicroschemaAPI,
    MeshNodeAPI,
    MeshProjectAPI,
    MeshProjectMicroschemaAPI,
    MeshProjectSchemaAPI,
    MeshRoleAPI,
    MeshSchemaAPI,
    MeshUserAPI,
    RequestMethod,
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
        login: (username, password) => {
            return this.performReqeust(POST, '/auth/login', { username, password });
        },
        me: () => {
            return this.performReqeust(GET, '/auth/me');
        },
    } as const;

    public users: MeshUserAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/users', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/users', body);
        },
        get: (uuid) => {
            return this.performReqeust(GET, `/users/${uuid}`);
        },
        update: (uuid, body) => {
            return this.performReqeust(POST, `/users/${uuid}`, body);
        },
        delete: (uuid) => {
            return this.performReqeust(DELETE, `/users/${uuid}`);
        },
    } as const;

    public roles: MeshRoleAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/roles', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/roles', body);
        },
        get: (uuid) => {
            return this.performReqeust(GET, `/roles/${uuid}`);
        },
        update: (uuid, body) => {
            return this.performReqeust(POST, `/roles/${uuid}`, body);
        },
        delete: (uuid) => {
            return this.performReqeust(DELETE, `/roles/${uuid}`);
        },
    } as const;

    public groups: MeshGroupAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/groups', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/groups', body);
        },
        get: (uuid) => {
            return this.performReqeust(GET, `/groups/${uuid}`);
        },
        update: (uuid, body) => {
            return this.performReqeust(POST, `/groups/${uuid}`, body);
        },
        delete: (uuid) => {
            return this.performReqeust(DELETE, `/groups/${uuid}`);
        },
    } as const;

    public projects: MeshProjectAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/projects', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/projects', body);
        },
        get: (uuidOrName) => {
            return this.performReqeust(GET, `/projects/${uuidOrName}`);
        },
        update: (uuidOrName, body) => {
            return this.performReqeust(POST, `/projects/${uuidOrName}`, body);
        },
        delete: (uuidOrName) => {
            return this.performReqeust(DELETE, `/projects/${uuidOrName}`);
        },
    } as const;

    public schemas: MeshSchemaAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/schemas', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/schemas', body);
        },
        get: (uuid) => {
            return this.performReqeust(GET, `/schemas/${uuid}`);
        },
        update: (uuid, body) => {
            return this.performReqeust(POST, `/schemas/${uuid}`, body);
        },
        delete: (uuid) => {
            return this.performReqeust(DELETE, `/schemas/${uuid}`);
        },
        diff: (uuid, body) => {
            return this.performReqeust(POST, `/schemas/${uuid}/diff`, body);
        },
        changes: (uuid, body) => {
            return this.performReqeust(POST, `/schemas/${uuid}/changes`, body);
        },
    } as const;

    public microschemas: MeshMicroschemaAPI = {
        list: (params?) => {
            return this.performReqeust(GET, '/microschemas', null, params);
        },
        create: (body) => {
            return this.performReqeust(POST, '/microschemas', body);
        },
        get: (uuid) => {
            return this.performReqeust(GET, `/microschemas/${uuid}`);
        },
        update: (uuid, body) => {
            return this.performReqeust(POST, `/microschemas/${uuid}`, body);
        },
        delete: (uuid) => {
            return this.performReqeust(DELETE, `/microschemas/${uuid}`);
        },
        diff: (uuid, body) => {
            return this.performReqeust(POST, `/microschemas/${uuid}/diff`, body);
        },
        changes: (uuid, body) => {
            return this.performReqeust(POST, `/microschemas/${uuid}/changes`, body);
        },
    } as const;

    public nodes: MeshNodeAPI = {
        list: (project, params?) => {
            return this.performReqeust(GET, `/${project}/nodes`, null, params);
        },
        create: (project, body) => {
            return this.performReqeust(POST, `/${project}/nodes`, body);
        },
        get: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/nodes/${uuid}`);
        },
        update: (project, uuid, body) => {
            return this.performReqeust(POST, `/${project}/nodes/${uuid}`, body);
        },
        delete: (project, uuid, params?) => {
            return this.performReqeust(DELETE, `/${project}/nodes/${uuid}`, null, params);
        },

        deleteLanguage: (project, uuid, language) => {
            return this.performReqeust(DELETE, `/${project}/nodes/${uuid}/languages/${language}`);
        },
        children: (project, uuid, params?) => {
            return this.performReqeust(GET, `/${project}/nodes/${uuid}/children`, null, params);
        },
        versions: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/nodes/${uuid}/versions`);
        },

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

        listTags: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/nodes/${uuid}/tags`);
        },
        setTags: (project, uuid, body) => {
            return this.performReqeust(POST, `/${project}/nodes/${uuid}/tags`, body);
        },
        assignTag: (project, uuid, tag) => {
            return this.performReqeust(POST, `/${project}/nodes/${uuid}/tags/${tag}`);
        },
        removeTag: (project, uuid, tag) => {
            return this.performReqeust(DELETE, `/${project}/nodes/${uuid}/tags/${tag}`);
        },
    } as const;

    public branches: MeshBranchAPI = {
        list: (project) => {
            return this.performReqeust(GET, `/${project}/branches`);
        },
        create: (project, body) => {
            return this.performReqeust(POST, `/${project}/branches`, body);
        },
        get: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/branches/${uuid}`);
        },
        update: (project, uuid, body) => {
            return this.performReqeust(POST, `/${project}/branches/${uuid}`, body);
        },
        asLatest: (project, uuid) => {
            return this.performReqeust(POST, `/${project}/branches/${uuid}/latest`);
        },
    } as const;

    public projectSchemas: MeshProjectSchemaAPI = {
        list: (project) => {
            return this.performReqeust(GET, `/${project}/schemas`);
        },
        get: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/schemas/${uuid}`);
        },
        assign: (project, uuid) => {
            return this.performReqeust(POST, `/${project}/schemas/${uuid}`);
        },
        unassign: (project, uuid) => {
            return this.performReqeust(DELETE, `/${project}/schemas/${uuid}`);
        },
    } as const;

    public projectMicroschemas: MeshProjectMicroschemaAPI = {
        list: (project) => {
            return this.performReqeust(GET, `/${project}/microschemas`);
        },
        get: (project, uuid) => {
            return this.performReqeust(GET, `/${project}/microschemas/${uuid}`);
        },
        assign: (project, uuid) => {
            return this.performReqeust(POST, `/${project}/microschemas/${uuid}`);
        },
        unassign: (project, uuid) => {
            return this.performReqeust(DELETE, `/${project}/microschemas/${uuid}`);
        },
    } as const;
}
