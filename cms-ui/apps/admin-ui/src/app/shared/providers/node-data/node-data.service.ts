import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import { EntityManagerService, I18nNotificationService, I18nService, NodeOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Node, Raw } from '@gentics/cms-models';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class NodeDataService extends ExtendedEntityDataServiceBase<'node', NodeOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: NodeOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'node',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: Node<Raw>): number {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

}
