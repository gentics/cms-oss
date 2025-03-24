import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ScheduleTaskBO, TableLoadOptions, applyPermissions } from '@admin-ui/common';
import { EntityManagerService, ScheduleTaskOperations } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ScheduleTask, ScheduleTaskListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ScheduleTaskTableLoaderService extends BaseTableLoaderService<ScheduleTask, ScheduleTaskBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ScheduleTaskOperations,
    ) {
        super('scheduleTask', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ScheduleTaskBO>> {
        const loadOptions: ScheduleTaskListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
        };

        return this.api.scheduler.listTasks(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(task => this.mapToBusinessObject(task));
                applyPermissions(entities, response);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(task: ScheduleTask): ScheduleTaskBO {
        return {
            ...task,
            [BO_ID]: String(task.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: task.name,
        };
    }
}
