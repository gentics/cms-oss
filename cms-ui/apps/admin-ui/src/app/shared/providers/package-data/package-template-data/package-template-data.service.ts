import { EntityManagerService, I18nNotificationService, I18nService, PackageTemplateOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { NormalizableEntityType, Raw, TemplateBO, TemplateFolderListRequest } from '@gentics/cms-models';
import { BehaviorSubject, combineLatest, Observable, Subject } from 'rxjs';
import { filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class PackageTemplateDataService extends ExtendedEntityDataServiceBase<'template', PackageTemplateOperations> {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: PackageTemplateOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
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

    getParentEntityId(): Observable<string> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'package' ? focusEntityId : undefined),
            filter((id: string | undefined) => id != null),
        );
    }

    getEntityId(entity: TemplateBO<Raw>): string {
        return entity.id;
    }

    getEntity(id: string): TemplateBO<Raw> {
        return { id } as TemplateBO<Raw>;
    }

    getEntitiesFromApi(options?: TemplateFolderListRequest): Observable<TemplateBO<Raw>[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.getParentEntityId().pipe(
            switchMap((parentId: string) => {
                return this.entityOperations.getAll(options, parentId).pipe(
                    tap(() => this.state.dispatch(new ResetDetailLoading())),
                );
            }),
        );
    }
}
