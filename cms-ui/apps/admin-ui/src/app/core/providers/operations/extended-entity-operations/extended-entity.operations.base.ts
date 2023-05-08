import { Injector } from '@angular/core';
import {
    EntityIdType,
    NormalizableEntityType,
    NormalizableEntityTypesMapBO,
    Raw,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { EntityOperationsBase } from '../entity-operations-base';

/**
 * Common superclass for operations on admin entities.
 *
 * @see /architecture/data-loading.png
 */
export abstract class ExtendedEntityOperationsBase<
    T extends NormalizableEntityType,
    T_RAW extends NormalizableEntityTypesMapBO<Raw>[T] = NormalizableEntityTypesMapBO<Raw>[T],
> extends EntityOperationsBase<T, T_RAW> {

    constructor(
        injector: Injector,
        entityIdentifier: T,
    ) {
        super(injector, entityIdentifier);
    }

    /**
     * Gets the list of all entities of this type that are visible to the current user.
     * @param options Search and filter parameters.
     * @param parentId If entity is a child entity, parent entity is identified via `parentId`.
     */
    abstract getAll(options?: any, parentId?: EntityIdType): Observable<T_RAW[]>;

}
