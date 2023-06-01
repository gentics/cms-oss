import {
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
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the content repository resource.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/guides/restapi/resource_RoleResource.html
 */
export class RoleApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get a list of roles.
     */
    getRoles(options?: RoleListOptions): Observable<RoleListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('role', options);
    }

    /**
     * Get a single role by id.
     */
    getRole(roleId: string): Observable<RoleLoadResponse> {
        return this.apiBase.get(`role/${roleId}`);
    }

    /**
     * Create a new role.
     */
    createRole(request: RoleCreateRequest): Observable<RoleCreateResponse> {
        return this.apiBase.put('role', request);
    }

    /**
     * Update a single role by id.
     */
    updateRole(roleId: number | string, request: RoleUpdateRequest): Observable<RoleUpdateResponse> {
        return this.apiBase.post(`role/${roleId}`, request);
    }

    /**
     * Delete a single role by id.
     */
    deleteRole(roleId: string | number): Observable<void> {
        return this.apiBase.delete(`role/${roleId}`);
    }

    /**
     * Get role permissions by id.
     */
    getRolePermissions(roleId: string): Observable<RolePermissionsLoadResponse> {
        return this.apiBase.get(`role/${roleId}/perm`);
    }

    /**
     * Update role permissions by id.
     */
    updateRolePermissions(roleId: string, request: RolePermissionsUpdateRequest): Observable<RolePermissionsUpdateResponse> {
        return this.apiBase.post(`role/${roleId}/perm`, request);
    }

}
