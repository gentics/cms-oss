import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ScheduleBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, ScheduleOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Schedule, ScheduleListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ScheduleTableLoaderService extends BaseTableLoaderService<Schedule, ScheduleBO> {

    private store: { [id: string]: ScheduleBO } = {};

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ScheduleOperations,
    ) {
        super('schedule', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).toPromise();
    }

    public override getEntitiesByIds(entityIds: (string | number)[]): ScheduleBO[] {
        return entityIds.map(id => this.store[id]);
    }

    public override getEntityById(entityId: string | number): ScheduleBO {
        return this.store[entityId];
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ScheduleBO>> {
        const loadOptions: ScheduleListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
            embed: 'task',
        };

        return this.api.scheduler.listSchedules(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(schedule => this.mapToBusinessObject(schedule));
                this.applyPermissions(entities, response);
                // Save the items into the store
                entities.forEach(schedule => this.store[schedule[BO_ID]] = schedule);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        )
    }

    public mapToBusinessObject(schedule: Schedule): ScheduleBO {
        return {
            ...schedule,
            [BO_ID]: String(schedule.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: schedule.name,
        };
    }
}
