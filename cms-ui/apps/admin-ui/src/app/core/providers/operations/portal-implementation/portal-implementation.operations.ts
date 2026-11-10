import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_PERMISSIONS,
    DevToolEntityHandler,
    DevToolEntityListRequestModel,
    DevToolEntityListRequestParams,
    DevToolEntityListResponseModel,
    EditableEntity,
    EditableEntityBusinessObjects,
    EntityList,
    LOAD_FOR_PACKAGE_LIST,
    PackageEntityOperations,
    applyPermissions,
} from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import {
    EntityIdType,
    PortalImplementation,
    PortalImplementationBO,
    PortalImplementationListOptions,
    Raw,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class PortalImplementationOperations
    extends ExtendedEntityOperationsBase<'portalImplementation'>
    implements PackageEntityOperations<PortalImplementationBO<Raw>>,
        DevToolEntityHandler<EditableEntity.PORTAL_IMPLEMENTATION> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entityManager: EntityManagerService,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector, 'portalImplementation');
    }

    /**
     * Loads all portal implementations from the CMS and adds them to the EntityState.
     */
    getAll(options?: PortalImplementationListOptions): Observable<PortalImplementationBO<Raw>[]> {
        return this.api.portalImplementations.getPortalImplementations(options).pipe(
            map((res) => res.items.map((item) => this.mapToBusinessObject(item, !!(options?.[LOAD_FOR_PACKAGE_LIST])))),
            tap((items) => this.entityManager.addEntities(this.entityIdentifier, items)),
            this.catchAndRethrowError(),
        );
    }

    /**
     * Loads a single portal implementation from the CMS and adds it to the EntityState.
     */
    get(portalImplementationId: EntityIdType): Observable<PortalImplementationBO<Raw>> {
        return this.api.portalImplementations.getPortalImplementation(portalImplementationId).pipe(
            map((res) => this.mapToBusinessObject(res.portalImplementation)),
            tap((item) => this.entityManager.addEntity(this.entityIdentifier, item)),
            this.catchAndRethrowError(),
        );
    }

    getAllFromPackage(packageId: string, options?: any): Observable<PortalImplementationBO<Raw>[]> {
        return this.api.devTools.getPortalImplementations(packageId, options).pipe(
            map((res) => res.items.map((item) => this.mapToBusinessObject(item, true))),
            this.catchAndRethrowError(),
        );
    }

    getFromPackage(packageId: string, entityId: string): Observable<PortalImplementationBO<Raw>> {
        return this.api.devTools.getPortalImplementation(packageId, entityId).pipe(
            map((res) => this.mapToBusinessObject(res.portalImplementation, true)),
            this.catchAndRethrowError(),
        );
    }

    addToDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        const entity = this.appState.now.entity.portalImplementation[entityId];

        return this.api.devTools.addPortalImplementationToPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'portal_implementation.portalImplementation_successfully_added_to_package',
                    type: 'success',
                    translationParams: {
                        name: entity?.name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    removeFromDevTool(
        devtoolPackage: string,
        entityId: string | number,
    ): Observable<void> {
        const entity = this.appState.now.entity.portalImplementation[entityId];

        return this.api.devTools.removePortalImplementationFromPackage(devtoolPackage, entityId).pipe(
            tap(() => {
                this.notification.show({
                    message: 'portal_implementation.portalImplementation_successfully_removed_from_package',
                    type: 'success',
                    translationParams: {
                        name: entity?.name,
                    },
                });
            }),
            this.catchAndRethrowError(),
        );
    }

    listFromDevTool(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        params?: DevToolEntityListRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<DevToolEntityListResponseModel<EditableEntity.PORTAL_IMPLEMENTATION>> {
        return this.api.devTools.getPortalImplementations(devtoolPackage, params).pipe(
            this.catchAndRethrowError(),
        );
    }

    listFromDevToolMapped(
        devtoolPackage: string,
        body?: DevToolEntityListRequestModel<EditableEntity.PORTAL_IMPLEMENTATION>,
        params?: DevToolEntityListRequestParams<EditableEntity.PORTAL_IMPLEMENTATION>,
    ): Observable<EntityList<EditableEntityBusinessObjects[EditableEntity.PORTAL_IMPLEMENTATION]>> {
        return this.listFromDevTool(devtoolPackage, body, params).pipe(
            map((res) => {
                const items = res.items.map((item) => ({
                    ...item,
                    [BO_ID]: String(item.id),
                    [BO_PERMISSIONS]: [],
                    [BO_DISPLAY_NAME]: item.name,
                }));
                applyPermissions(items, res);

                return {
                    items,
                    totalItems: res.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(portalImplementation: PortalImplementation<Raw>, forPackage: boolean = false): PortalImplementationBO<Raw> {
        return {
            ...portalImplementation,
            id: forPackage ? portalImplementation.globalId?.toString?.() : portalImplementation.id?.toString?.(),
        };
    }
}
