import { detailLoading, discard } from '@admin-ui/common';
import { EntityManagerService, GroupOperations, I18nNotificationService, I18nService, PackageOperations, PermissionsService } from '@admin-ui/core';
import { PackageDataService } from '@admin-ui/shared/providers/package-data';
import { AppStateService, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { NormalizableEntityType, PackageBO, PackageCreateRequest, Raw } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';

@Injectable()
export class NodePackageDataService extends PackageDataService {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<number>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: PackageOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
        protected modalService: ModalService,
        protected groupOperations: GroupOperations,
    ) {
        super(
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
            permissionsService,
        );
    }

    getParentEntityId(): Observable<number> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'node' ? focusEntityId : undefined),
            filter((id: number | undefined) => id != null),
        );
    }

    getEntitiesFromApi(): Observable<PackageBO<Raw>[]> {
        // then get packages of node
        return this.getParentEntityId().pipe(
            filter((parentEntityId: number | undefined) => Number.isInteger(parentEntityId)),
            switchMap((parentEntityId: number) => {
                return this.entityOperations.getPackagesOfNode(parentEntityId).pipe(this.getLoadingOperator());
            }),
        );
    }

    getRawEntitiesFromState(): Observable<PackageBO<Raw>[]> {
        // Data model does not provide relationship indicator, so return from API.
        return this.getEntitiesFromApi();
    }

    createPackageInNode(nodeId: number, payload: PackageCreateRequest): Observable<PackageBO<Raw>> {
        return this.entityOperations.createPackageInNode(nodeId, payload);
    }

    changePackagesOfNode(nodeId: number, packageIds: string[], preselected: string[]): Observable<void> {
        return this.entityOperations.changePackagesOfNode(nodeId, packageIds, preselected).pipe(discard());
    }

    addPackageToNode(nodeId: number, packageName: string | string[]): Observable<void> {
        return this.entityOperations.addPackageToNode(nodeId, packageName);
    }

    removePackageFromNode(nodeId: number, packageName: string | string[]): Observable<void> {
        return this.entityOperations.removePackageFromNode(nodeId, packageName);
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
