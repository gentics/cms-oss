import { Injectable, Injector } from '@angular/core';
import { Raw, ScheduleExecution, ScheduleExecutionListOptions } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityManagerService } from '../../entity-manager';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ScheduleExecutionOperations extends ExtendedEntityOperationsBase<'scheduleExecution'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private entities: EntityManagerService,
    ) {
        super(injector, 'scheduleExecution');
    }

    getAll(options?: ScheduleExecutionListOptions, parentId?: number): Observable<ScheduleExecution<Raw>[]> {
        return this.api.scheduler.listExecutions(parentId, options).pipe(
            map(res => res.items),
            tap(executions => {
                this.entities.addEntities(this.entityIdentifier, executions);
            }),
            this.catchAndRethrowError(),
        )
    }

    get(entityId: number, options?: any, parentId?: number): Observable<ScheduleExecution<Raw>> {
        return this.api.scheduler.getExecution(parentId, entityId).pipe(
            map(res => res.item),
            tap(executions => this.entities.addEntity(this.entityIdentifier, executions)),
            this.catchAndRethrowError(),
        );
    }
}
