import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ScheduleExecutionBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Raw, ScheduleExecution } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface ScheduleExecutionsTableLoaderOptions {
    scheduleId: string | number;
}

@Injectable()
export class ScheduleExecutionsTableLoaderService
    extends BaseTableLoaderService<ScheduleExecution, ScheduleExecutionBO, ScheduleExecutionsTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        super('scheduleExecution', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject(new Error('Deletion of Schedule-Executions is not allowed/supported!'));
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: ScheduleExecutionsTableLoaderOptions,
    ): Observable<EntityPageResponse<ScheduleExecutionBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.scheduler.listExecutions(additionalOptions.scheduleId, loadOptions).pipe(
            map(response => {
                const entities = response.items.map(exec => this.mapToBusinessObject(exec));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(exec: ScheduleExecution<Raw>): ScheduleExecutionBO {
        return {
            ...exec,
            [BO_ID]: String(exec.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: String(exec.id),
        };
    }
}
