import { Injectable } from '@angular/core';
import { ContentPackageImportError } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, ImportErrorBO, TableLoadOptions } from '../../../../common';
import { BaseTableLoaderService, ContentPackageOperations, EntityManagerService } from '../../../../core';
import { AppStateService } from '../../../../state/providers/app-state/app-state.service';

export interface ContentStagingImportErrorTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class ContentPackageImportErrorTableLoaderService extends BaseTableLoaderService<ContentPackageImportError, ImportErrorBO> {

    public lastCheckTimestamp$ = new BehaviorSubject<string>('');

    public checkResultAvailable$ = new BehaviorSubject<boolean>(false);

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected client: GCMSRestClientService,
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
        const packageName = additionalOptions?.packageName;

        return this.client.contentStaging.errors(packageName).pipe(
            map((response) => {
                const entities = response.errors.map((error) => this.mapToBusinessObject(error))
                    .sort((a, b) => -a?.path.localeCompare(b?.path));
                this.checkResultAvailable$.next(true);
                this.lastCheckTimestamp$.next(new Date(response.timestamp).toLocaleString());

                return {
                    entities,
                    totalCount: response.errors.length,
                };
            }),
            catchError((_error) => {
                this.checkResultAvailable$.next(false);
                return of({
                    entities: [],
                    totalCount: 0,
                    hasError: true,
                });
            }),
        );
    }

    public mapToBusinessObject(error: ContentPackageImportError): ImportErrorBO {
        return {
            ...error,
            [BO_ID]: error.globalId,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: error.error,
        };
    }
}
