import { detailLoading, LOAD_FROM_PACKAGE, masterLoading } from '@admin-ui/common';
import { ContentRepositoryOperations, EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryBO, ContentRepositoryListOptions, Raw } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ContentRepositoryDataService extends ExtendedEntityDataServiceBase<'contentRepository', ContentRepositoryOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ContentRepositoryOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'contentRepository',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ContentRepositoryBO<Raw>): string {
        return entity.id;
    }

    override getEntitiesFromApi(options?: ContentRepositoryListOptions): Observable<ContentRepositoryBO<Raw>[]> {
        if (options?.[LOAD_FROM_PACKAGE]) {
            return this.entityOperations.getAllFromPackage(options[LOAD_FROM_PACKAGE], options).pipe(
                detailLoading(this.state),
            );
        }

        return super.getEntitiesFromApi(options);
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}
