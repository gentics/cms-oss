import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, DirtQueueItemBO, discard, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AdminOperations, BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DirtQueueItem, DirtQueueListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class DirtQueueItemTableLoaderService extends BaseTableLoaderService<DirtQueueItem, DirtQueueItemBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: AdminOperations,
    ) {
        super(null, entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.deleteFailedDirtQueueOfNode(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<DirtQueueItemBO>> {
        const loadOptions: DirtQueueListOptions = {
            ...this.createDefaultOptions(options),
            failed: true,
        };

        return this.api.adminInfo.getDirtQueue(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(item => this.mapToBusinessObject(item));

                return {
                    entities,
                    totalCount: entities.length,
                };
            }),
        );
    }

    public mapToBusinessObject(item: DirtQueueItem): DirtQueueItemBO {
        return {
            ...item,
            [BO_ID]: String(item.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: item.label,
        };
    }
}
