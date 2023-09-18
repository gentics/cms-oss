import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { Injectable } from '@angular/core';
import { GenericMessageResponse, RolePermissionRequest, RolePermissionResponse, RoleReference } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MeshRolePermissionHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public async get(roleUuid: string, entityPath: string): Promise<RolePermissionResponse> {
        try {
            const res = await this.mesh.permissions.get(roleUuid, entityPath);
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async set(role: RoleReference, entityPath: string, body: RolePermissionRequest): Promise<GenericMessageResponse> {
        try {
            const res = await this.mesh.permissions.set(role.uuid, entityPath, body);
            this.notification.show({
                message: 'mesh.role_permission_applied',
                type: 'success',
                translationParams: {
                    entityName: role.name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }
}
