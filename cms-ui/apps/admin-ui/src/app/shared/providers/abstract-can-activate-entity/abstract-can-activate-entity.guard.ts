import { EntityOperationsBase } from '@admin-ui/core';
import { AppStateService, FocusEditor, OpenEditor, SetUIFocusEntity } from '@admin-ui/state';
import { ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot } from '@angular/router';
import { EntityIdType, NormalizableEntityType, NormalizableEntityTypesMapBO, Normalized, Raw } from '@gentics/cms-models';
import { EntityDataServiceBase } from '../entity-data-service-base/entity-data.service.base';

/**
 * A route guard preventing navigation if a user is not allowed to view an entity.
 */
export abstract class AbstractCanActivateEntityGuard<
    T extends NormalizableEntityType,
    O extends EntityOperationsBase<T, T_RAW>,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
    T_NORM extends NormalizableEntityTypesMapBO<Normalized>[T] = NormalizableEntityTypesMapBO<Normalized>[T],
> implements CanActivate {

    /** Name of the entity */
    readonly entityIdentifier: NormalizableEntityType;

    constructor(
        entityIdentifier: NormalizableEntityType,
        private entityData: EntityDataServiceBase<T, O, T_RAW, T_NORM>,
        private appState: AppStateService,
    ) {
        this.entityIdentifier = entityIdentifier;
    }

    protected loadEntityById(id: EntityIdType): Promise<T_RAW> {
        return this.entityData.getEntityFromApi(id).toPromise();
    }

    async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        const routeId = route.paramMap.has('id') && route.paramMap.get('id');
        let nodeId: null | number | string = route.paramMap.get('nodeId');
        if (nodeId != null) {
            nodeId = Number(nodeId);
            if (!Number.isInteger(nodeId)) {
                nodeId = null;
            }
        }

        const entityId = (Number.isInteger(Number(routeId)) || typeof routeId === 'string') && routeId;
        if (entityId == null) {
            return false;
        }

        try {
            const entity: T_RAW = await this.loadEntityById(entityId);
            this.setEntityInState(entity, nodeId as number | null);
            return true;
        } catch (err) {
            return false;
        }
    }

    private setEntityInState(entity: T_RAW, nodeId: null | number): void {
        this.appState.dispatch(new SetUIFocusEntity(this.entityIdentifier, entity.id, nodeId));
        this.appState.dispatch(new OpenEditor());
        this.appState.dispatch(new FocusEditor());
    }

}
