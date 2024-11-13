import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ContentRepositoryLicenseBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryLicense } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { catchError, map, Observable, of } from 'rxjs';
import { createMockLicenseInfo } from '../../mock';

@Injectable()
export class ContentRepositoryLicenseTableLoaderService extends BaseTableLoaderService<ContentRepositoryLicenseBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        private client: GCMSRestClientService,
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
            catchError(() => {
                const total = Math.trunc(Math.random() * 10);
                const items: ContentRepositoryLicenseBO[] = [];
                for (let i = 0; i < total; i++) {
                    items.push(this.mapToBusinessObject({
                        id: i,
                        name: `ContentRepository #${i}`,
                        url: 'example.com',
                        ...createMockLicenseInfo(),
                    }))
                }

                return of({
                    entities: items,
                    totalCount: total,
                });
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
