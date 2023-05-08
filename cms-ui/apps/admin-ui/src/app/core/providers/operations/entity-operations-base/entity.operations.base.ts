import { Injector } from '@angular/core';
import {
    EntityIdType,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Raw,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { OperationsBase } from '../operations.base';

/**
 * Common superclass for operations on entities.
 *
 * `EntityOperations` services are used to fetch entities and execute actions on them
 * and then apply the results to the AppState.
 *
 * For data fetches triggered by a component, an operations service should not be used
 * directly, instead the corresponding `EntityDataService` should be used.
 * For edit actions, an operations service may be directly used by a component.
 *
 * We distinguish between two types of entities: *admin entities* and *auxiliary entities*.
 *
 * An **admin entity** is an entity that can be edited via the Admin UI and allows
 * fetching all its instances at once (e.g., nodes, groups).
 * For these entities, operations services need to extend `AdminEntityOperationsBase`.
 *
 * An **auxiliary entity**, on the other hand, is not primarily edited through
 * the Admin UI and does not allow fetching all its instances at once (e.g., folders, pages).
 * For these entities, operations services need to extend this class.
 *
 * @see /architecture/data-loading.png
 */
export abstract class EntityOperationsBase<
    T extends NormalizableEntityType,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
> extends OperationsBase {

    /** Name of the entity */
    readonly entityIdentifier: T;

    constructor(
        injector: Injector,
        entityIdentifier: T,
    ) {
        super(injector);
        this.entityIdentifier = entityIdentifier;
    }

    /**
     * Get a single (child) entity and add it to the AppState.
     */
    abstract get(entityId: EntityIdType, options?: any, parentId?: EntityIdType): Observable<T_RAW>;

}
