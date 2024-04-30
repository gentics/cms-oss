import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ActionLogEntryBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActionLogEntry } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ActionLogEntryLoaderService extends BaseTableLoaderService<ActionLogEntry, ActionLogEntryBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
    ) {
        super('logs', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject('Deletion of Logs is not possible!');
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ActionLogEntryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        console.log('OBJECT FILTER IN SERVICE', options.filters?.object);

        return this.api.logs.getLogs(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(log => this.mapToBusinessObject(log));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(log: ActionLogEntry): ActionLogEntryBO {
        let displayName: string;

        if (log.objId > 0) {
            displayName = `${log.action?.label} ${log.type?.label}: ${log.objId}`;
        } else {
            displayName = log.action.label;
        }

        return {
            ...log,
            [BO_ID]: String(log.id),
            [BO_PERMISSIONS]: [],
            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
            [BO_DISPLAY_NAME]: displayName,
        }
    }
}
