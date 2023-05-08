import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import { EntityManagerService, I18nNotificationService, I18nService, LanguageOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Language } from '@gentics/cms-models';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class LanguageDataService extends ExtendedEntityDataServiceBase<'language', LanguageOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: LanguageOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'language',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: Language): number {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

    watchSupportedLanguages(options?: any): Observable<Language[]> {
        return combineLatest([
            this.watchAllEntities(options),
            this.entityOperations.getBackendLanguages(),
        ]).pipe(
            map(([allLangs, avilableLangs]) => allLangs.filter(lang => avilableLangs.map(i => i.code).includes(lang.code))),
        );
    }
}
