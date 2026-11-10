import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    EditableEntity,
    EditableEntityBusinessObjects,
    EditableEntityModels,
    EntityCreateRequestModel,
    EntityCreateRequestParams,
    EntityCreateResponseModel,
    EntityEditorHandler,
    EntityLoadRequestParams,
    EntityLoadResponseModel,
    EntityUpdateRequestModel,
    EntityUpdateRequestParams,
    EntityUpdateResponseModel,
} from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { Node, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { BaseEntityHandlerService } from '../base-entity-handler/base-entity-handler';
import { ErrorHandler } from '../error-handler';

@Injectable()
export class PortalImplementationHandlerService
    extends BaseEntityHandlerService
    implements EntityEditorHandler<EditableEntity.PORTAL_IMPLEMENTATION> {

    constructor(
        errorHandler: ErrorHandler,
        protected api: GcmsApi,
        protected notification: I18nNotificationService,
    ) {
        super(errorHandler);
    }

    displayName(entity: EditableEntityModels[EditableEntity.PORTAL_IMPLEMENTATION]): string {
        return entity.name;
    }

    public mapToBusinessObject(
        impl: EditableEntityModels[EditableEntity.PORTAL_IMPLEMENTATION],
        index?: number,
    ): EditableEntityBusinessObjects[EditableEntity.PORTAL_IMPLEMENTATION] {
        return {
            ...impl,
            [BO_ID]: String(impl.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.displayName(impl),
        };
    }

    create(
        data: EntityCreateRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        params?: EntityCreateRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EntityCreateResponseModel<EditableEntity.PORTAL_IMPLEMENTATION>> {
        return this.api.portalImplementations.createPortalImplementation(data).pipe(
            tap((res) => {
                const name = this.displayName(res.portalImplementation);
                this.nameMap[res.portalImplementation.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_created',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    createMapped(
        data: EntityCreateRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        options?: EntityCreateRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.PORTAL_IMPLEMENTATION]> {
        return this.create(data, options).pipe(
            map((res) => this.mapToBusinessObject(res.portalImplementation)),
        );
    }

    get(
        id: string | number,
        params?: EntityLoadRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EntityLoadResponseModel<EditableEntity.PORTAL_IMPLEMENTATION>> {
        return this.api.portalImplementations.getPortalImplementation(id).pipe(
            tap((res) => {
                this.nameMap[res.portalImplementation.id] = this.displayName(res.portalImplementation);
            }),
            this.catchAndRethrowError(),
        );
    }

    getMapped(id: string | number): Observable<EditableEntityBusinessObjects[EditableEntity.PORTAL_IMPLEMENTATION]> {
        return this.get(id).pipe(
            map((res) => this.mapToBusinessObject(res.portalImplementation)),
        );
    }

    update(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        params?: EntityUpdateRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EntityUpdateResponseModel<EditableEntity.PORTAL_IMPLEMENTATION>> {
        return this.api.portalImplementations.updatePortalImplementation(id, data).pipe(
            tap((res) => {
                const name = this.displayName(res.portalImplementation);
                this.nameMap[res.portalImplementation.id] = name;

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_updated',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    updateMapped(
        id: string | number,
        data: EntityUpdateRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        params?: EntityUpdateRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EditableEntityBusinessObjects[EditableEntity.PORTAL_IMPLEMENTATION]> {
        return this.update(id, data, params).pipe(
            map((res) => this.mapToBusinessObject(res.portalImplementation)),
        );
    }

    delete(id: string | number): Observable<void> {
        return this.api.portalImplementations.deletePortalImplementation(id).pipe(
            tap(() => {
                const name = this.nameMap[id];

                if (!name) {
                    return;
                }

                delete this.nameMap[id];
                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    translationParams: {
                        name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Get the nodes, the portal implementation is assigned to
     */
    getAssignedNodes(id: string | number): Observable<Node<Raw>[]> {
        return this.api.portalImplementations.getPortalImplementationNodes(id).pipe(
            map((res) => res.items),
            this.catchAndRethrowError(),
        );
    }
}
