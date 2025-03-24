import { ActionLogEntryBO, BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ActionLogEntry, BaseListOptionsWithPaging, LogTypeListItem, LogsListRequest } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export const COLUMN_TO_API_PARAM_MAP = new Map(Object.entries({
    object: 'type',
    timestamp: 'start',
}));

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
        const optionsWithFilter = this.createOptionsWithFilter(loadOptions, options.filters);

        return this.api.logs.getLogs(optionsWithFilter).pipe(
            map(response => {
                const entities = response.items.map(log => this.mapToBusinessObject(log));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
            catchError(() => of({
                entities: [],
                totalCount: 0,
            })),
        );
    }

    public async getActionLogTypes(): Promise<LogTypeListItem[]> {
        const options =  {
            pageSize: -1,
        };

        const list = await this.api.logs.getTypes(options).pipe(
            map(response => {
                return response.items.sort((a,b) => a.label?.localeCompare(b.label));
            }),
        ).toPromise()

        return list;
    }

    public async getActions(): Promise<LogTypeListItem[]> {
        const options =  {
            pageSize: -1,
        };

        const list = await this.api.logs.getActions(options).pipe(
            map(response => {
                return response.items.sort((a,b) => a.label?.localeCompare(b.label)).map(item => {
                    return {
                        name: item.name,
                        label: item.label,
                    }
                })
            }),
        ).toPromise()

        return list;
    }

    private createOptionsWithFilter(options: BaseListOptionsWithPaging<ActionLogEntry>, filters: Record<string, any>): LogsListRequest {
        const filterKeys = Object.keys(filters).filter(key => filters[key] != null);

        if (!filterKeys || !filterKeys.length) {
            return options;
        }

        const filterParameterMap = filterKeys.map(key => ({
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            [this.getMappedKey(key)]: (typeof filters[key] === 'string')  ? filters[key].toLowerCase(): filters[key],
        }),
        ).reduce((obj, item) => ({
            ...obj,
            ...item,
        }), {});

        const optionsWithFilter = {
            ...options,
            ...filterParameterMap,
        }
        delete optionsWithFilter.q;

        return optionsWithFilter;
    }


    private getMappedKey(key: string): string {
        const mappedKey = COLUMN_TO_API_PARAM_MAP.get(key);

        return mappedKey ?? key;
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
            [BO_DISPLAY_NAME]: displayName,
        }
    }
}
