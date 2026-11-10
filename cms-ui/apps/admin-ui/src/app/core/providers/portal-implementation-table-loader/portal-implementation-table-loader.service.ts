import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, PortalImplementationBO, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PortalImplementation, PortalImplementationListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { PortalImplementationOperations } from '../operations';
import { PortalImplementationHandlerService } from '../portal-implementation-handler/portal-implementation-handler.service';

export interface PortalImplementationTableLoaderOptions {
    /** Load the portal implementations contained in the package */
    packageName?: string;
    /** Load the portal implementations assigned to the node */
    nodeId?: number;
}

@Injectable()
export class PortalImplementationTableLoaderService
    extends BaseTableLoaderService<PortalImplementation, PortalImplementationBO, PortalImplementationTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: PortalImplementationOperations,
        protected handler: PortalImplementationHandlerService,
    ) {
        super('portalImplementation', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: PortalImplementationTableLoaderOptions,
    ): Observable<EntityPageResponse<PortalImplementationBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<PortalImplementationListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getPortalImplementations(additionalOptions.packageName, loadOptions);
        } else if (additionalOptions?.nodeId) {
            loader = this.api.node.getNodePortalImplementations(additionalOptions.nodeId, loadOptions);
        } else {
            loader = this.api.portalImplementations.getPortalImplementations(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(item => this.mapToBusinessObject(item));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(portalImplementation: PortalImplementation): PortalImplementationBO {
        return {
            ...portalImplementation,
            [BO_ID]: String(portalImplementation.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: portalImplementation.name,
        };
    }

    addToDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.operations.addToDevTool(devToolPackage, entityId);
    }

    removeFromDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.operations.removeFromDevTool(devToolPackage, entityId);
    }

    addToNode(nodeId: number, entityId: string | number): Observable<void> {
        return this.operations.addToNode(nodeId, entityId);
    }

    removeFromNode(nodeId: number, entityId: string | number): Observable<void> {
        return this.operations.removeFromNode(nodeId, entityId);
    }
}
