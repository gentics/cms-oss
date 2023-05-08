import { EntityIdType } from '@gentics/cms-models';
import { Observable } from 'rxjs';

/**
 * Interface for a class that provides data to an `EntityDataGrid`.
 *
 * @param K Defines the type of the entities' keys (IDs).
 * @param E Defines the type of entity that is displayed.
 */
export interface EntityGridDataProvider<E>  {

    /** Observable for watching the total amount of entities. */
    amountTotal$: Observable<number>;

    /**
     * @returns The unique ID of the specified entity.
     */
    getEntityId(entity: E): EntityIdType;

    /**
     * @returns The entity with the specified `id` or `undefined` if the ID
     * is unknown.
     */
    getEntity(id: EntityIdType): E;

    /**
     * @returns An Observable that emits all entities to be displayed
     * and that emits again when the entities change.
     */
    watchAllEntities(options?: any): Observable<E[]>;

}
