import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ScheduleTaskBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, ScheduleTaskOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ScheduleTask, ScheduleTaskListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ScheduleTaskTableLoaderService extends BaseTableLoaderService<ScheduleTask, ScheduleTaskBO> {

    private store: { [id: string]: ScheduleTaskBO } = {};

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

    public override getEntitiesByIds(entityIds: (string | number)[]): ScheduleTaskBO[] {
        return entityIds.map(id => this.store[id]);
    }

    public override getEntityById(entityId: string | number): ScheduleTaskBO {
        return this.store[entityId];
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ScheduleTaskBO>> {
        const loadOptions: ScheduleTaskListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
        };

        return this.api.scheduler.listTasks(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(task => this.mapToBusinessObject(task));
                this.applyPermissions(entities, response);
                entities.forEach(task => this.store[task[BO_ID]] = task);

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
