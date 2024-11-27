import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ContentRepositoryLicenseBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, ErrorHandler } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryLicense } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { catchError, map, Observable, throwError } from 'rxjs';

@Injectable()
export class ContentRepositoryLicenseTableLoaderService extends BaseTableLoaderService<ContentRepositoryLicenseBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        private client: GCMSRestClientService,
        private errorHandler: ErrorHandler,
    ) {
        super(null, entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number, additonalOptions?: never): Promise<void> {
        return Promise.reject('Deletion is not supported');
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: never): Observable<EntityPageResponse<ContentRepositoryLicenseBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.client.license.contentRepositories(loadOptions as any).pipe(
            map(res => {
                return {
                    entities: res.items.map(entity => this.mapToBusinessObject(entity)),
                    totalCount: res.numItems,
                };
            }),
            catchError(err => {
                this.errorHandler.catch(err);
                return throwError(() => err);
            }),
        );
    }

    public mapToBusinessObject(entity: ContentRepositoryLicense): ContentRepositoryLicenseBO {
        return {
            ...entity,
            [BO_ID]: String(entity.id),
            [BO_DISPLAY_NAME]: entity.name,
            [BO_PERMISSIONS]: [],
        };
    }
}
