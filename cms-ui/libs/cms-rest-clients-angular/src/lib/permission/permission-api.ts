import { AccessControlledType, EntityIdType, PermissionResponse, SinglePermissionResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the permissions resource.
 *
 * Docs for the endpoints used here can be found at:
 * http://www.gentics.com/Content.Node/guides/restapi/resource_PermResource.html
 */
export class PermissionApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Gets the permissions the current user has on a specific type.
     */
    getPermissionsForType(type: AccessControlledType): Observable<PermissionResponse> {
        return this.apiBase.get(`perm/${type}?map=true`);
    }

    /**
     * Gets the permissions the current user has on a specific instance of a type, optionally restricted to a node.
     */
    getPermissionsForInstance(type: AccessControlledType, instanceId: number | string, nodeId?: number): Observable<PermissionResponse> {
        const queryParams = typeof nodeId === 'number' ? { nodeId } : undefined;
        return this.apiBase.get(`perm/${type}/${instanceId}?map=true`, queryParams);
    }

    /**
     * Get the permissions the current user has for a folder in a node.
     */
    getFolderPermissions(folderId: number, nodeId: number): Observable<PermissionResponse> {
        return this.apiBase.get(`perm/10002/${folderId}?map=true`, { nodeId });
    }

    /**
     * Get the permissions the current user has for the message Inbox.
     */
    getInboxPermissions(): Observable<{ view: boolean, assignPermissions: boolean, instantMessages: boolean }> {
        return this.apiBase.get('perm/17/0').pipe(
            map((response: PermissionResponse) => {
                let permBits = response && response.perm || '000';
                return {
                    view: permBits[0] === '1',
                    assignPermissions: permBits[1] === '1',
                    instantMessages: permBits[2] === '1',
                };
            }),
        );
    }

    /**
     * Get the permissions the current user has in a node. Since derived nodes just inherit
     * their parent's permissions, this endpoint needs to be called with the parent node id.
     */
    getNodePermissions(nodeId: number): Observable<PermissionResponse> {
        return this.apiBase.get(`perm/10001/${nodeId}?map=true`);
    }

    /**
     * Get the permissions the current user has for the publish queue.
     */
    getPublishQueuePermissions(): Observable<{ view: boolean, assignPermissions: boolean }> {
        return this.apiBase.get('perm/10013/0').pipe(
            map((response: PermissionResponse) => {
                let permBits = response && response.perm || '000';
                return {
                    view: permBits[0] === '1',
                    assignPermissions: permBits[1] === '1',
                };
            }),
        );
    }

    getTemplateViewPermissions(templateId: EntityIdType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/view/template/${templateId}`);
    }

    getTemplateEditPermissions(templateId: EntityIdType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/edit/template/${templateId}`);
    }

    getTemplatePublishPermissions(templateId: EntityIdType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/publish/template/${templateId}`);
    }

    getTemplateDeletePermissions(templateId: EntityIdType): Observable<SinglePermissionResponse> {
        return this.apiBase.get(`perm/delete/template/${templateId}`);
    }
}
