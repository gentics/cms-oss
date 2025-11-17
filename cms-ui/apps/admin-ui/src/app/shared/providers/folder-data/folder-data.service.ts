import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import { EntityManagerService, FolderOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { Folder, Raw } from '@gentics/cms-models';
import { I18nService } from '@gentics/cms-components';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class FolderDataService extends ExtendedEntityDataServiceBase<'folder', FolderOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: FolderOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'folder',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: Folder<Raw>): number {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

}
