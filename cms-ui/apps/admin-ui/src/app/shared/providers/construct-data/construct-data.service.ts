import { detailLoading, LOAD_FROM_PACKAGE, masterLoading } from '@admin-ui/common';
import { ConstructOperations, EntityManagerService, I18nNotificationService, I18nService, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Raw, TagTypeBO } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ConstructDataService extends ExtendedEntityDataServiceBase<'construct', ConstructOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ConstructOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'construct',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: TagTypeBO<Raw>): string {
        return entity.id;
    }

    override getEntitiesFromApi(options?: any): Observable<TagTypeBO<Raw>[]> {
        if (options?.[LOAD_FROM_PACKAGE]) {
            return this.entityOperations.getAllFromPackage(options[LOAD_FROM_PACKAGE], options).pipe(
                detailLoading(this.state),
            );
        }

        return super.getEntitiesFromApi(options);
    }

    protected getLoadingOperator(): any {
        return masterLoading(this.state);
    }
}
