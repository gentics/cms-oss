import { detailLoading, LOAD_FROM_PACKAGE, masterLoading } from '@admin-ui/common';
import {
    EntityManagerService,
    I18nNotificationService,
    I18nService,
    NodeOperations,
    PermissionsService,
    TemplateOperations,
} from '@admin-ui/core';
import { AppStateService, SelectState, UIStateModel } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Raw, TemplateBO, TemplateCreateRequest, TemplateListRequest } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class TemplateDataService extends ExtendedEntityDataServiceBase<'template', TemplateOperations> {

    @SelectState(state => state.ui)
    stateUi$: Observable<UIStateModel>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: TemplateOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
        protected nodeOperations: NodeOperations,
    ) {
        super(
            'template',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: TemplateBO<Raw>): string {
        return entity.id;
    }

    getEntitiesFromApi(options?: TemplateListRequest): Observable<TemplateBO<Raw>[]> {
        if (options?.[LOAD_FROM_PACKAGE]) {
            return this.entityOperations.getAllFromPackage(options[LOAD_FROM_PACKAGE], options).pipe(
                detailLoading(this.state),
            );
        }

        let loader: Observable<TemplateBO<Raw>[]>;

        if (options.nodeId) {
            loader = this.entityOperations.getAllOfNode(options);
        } else {
            loader = this.entityOperations.getAllOfAllNodes(options);
        }

        return loader.pipe(
            // eslint-disable-next-line @typescript-eslint/no-misused-promises
            tap(templates => this.entityManager.addEntities(this.entityIdentifier, templates)),
        );
    }

    create(request: TemplateCreateRequest): Observable<TemplateBO<Raw>> {
        return this.entityOperations.create(request);
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}
