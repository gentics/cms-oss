import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ScheduleBO, TableLoadOptions, applyPermissions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, ScheduleOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Schedule, ScheduleListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ScheduleTableLoaderService extends BaseTableLoaderService<Schedule, ScheduleBO> {

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

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ScheduleBO>> {
        const loadOptions: ScheduleListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
            embed: 'task',
        };

        return this.api.scheduler.listSchedules(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(schedule => this.mapToBusinessObject(schedule));
                applyPermissions(entities, response);

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
