import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ImportErrorBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, ContentPackageOperations, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ImportError } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface ContentStagingImportErrorTableLoaderOptions {
    packageName?: string;
}


@Injectable()
export class ContentPackageImportErrorTableLoaderService extends BaseTableLoaderService<ImportError, ImportErrorBO> {

    public lastCheckTimestamp$ = new BehaviorSubject<string>('');

    public checkResultAvailable$ = new BehaviorSubject<boolean>(false);

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ContentPackageOperations,
    ) {
        super(null, entityManager, appState);
    }

    public canDelete(_entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(_entityId: string): Promise<void> {
        return Promise.reject();
    }

    public loadEntities(
        _options: TableLoadOptions,
        additionalOptions: ContentStagingImportErrorTableLoaderOptions,
    ): Observable<EntityPageResponse<ImportErrorBO>> {
        const packageName = additionalOptions?.packageName

        return this.api.contentStaging.getImportErrors(packageName).pipe(
            map(response => {
                const entities = response.errors.map(error => this.mapToBusinessObject(error));
                this.checkResultAvailable$.next(true);
                this.lastCheckTimestamp$.next(new Date(response.timestamp).toLocaleString())

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
            catchError(_error => {
                this.checkResultAvailable$.next(false);
                return of({
                    entities: [],
                    totalCount: 0,
                    hasError: true,
                })
            }),
        );
    }

    public mapToBusinessObject(error: ImportError): ImportErrorBO {
        return {
            ...error,
            [BO_ID]: error.globalId,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: error.error,
        };
    }
}
