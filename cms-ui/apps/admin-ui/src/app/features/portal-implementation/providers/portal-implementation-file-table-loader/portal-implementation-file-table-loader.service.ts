import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, PortalImplementationFileBO, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PortalImplementationFile } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../../../../core/providers/base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../../../../core/providers/entity-manager';

export interface PortalImplementationFileTableLoaderOptions {
    /** ID of the portal implementation */
    portalImplementationId: string | number;
}

/**
 * Loader for the (read-only) table of files contained in a portal implementation
 */
@Injectable()
export class PortalImplementationFileTableLoaderService
    extends BaseTableLoaderService<PortalImplementationFile, PortalImplementationFileBO, PortalImplementationFileTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        // files are not stored in the entity state
        super(null, entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: PortalImplementationFileTableLoaderOptions,
    ): Observable<EntityPageResponse<PortalImplementationFileBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.portalImplementations.getPortalImplementationFiles(additionalOptions.portalImplementationId, loadOptions).pipe(
            map(response => ({
                entities: response.items.map(file => this.mapToBusinessObject(file)),
                totalCount: response.numItems,
            })),
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        // files cannot be changed in the admin UI
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject(new Error('Files of portal implementations cannot be deleted in the admin UI'));
    }

    public mapToBusinessObject(file: PortalImplementationFile): PortalImplementationFileBO {
        return {
            ...file,
            [BO_ID]: String(file.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: file.path,
        };
    }
}
