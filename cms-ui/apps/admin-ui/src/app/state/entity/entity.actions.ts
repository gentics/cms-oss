import { EntityIdType, NormalizableEntityType, RecursivePartial } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';
import { EntityStateModel } from './entity.state';


const ENTITY: keyof AppState = 'entity';

/**
 * Adds entities to the EntityState.
 *
 * If an entity already exists in the state, the properties of
 * the existing version will be merged with the new version.
 *
 * **Important:** This merging is only a shallow merge, so if
 * a property of the new entity is an object or an array, the
 * old property will be replaced completely. If you want to do a deep merge
 * instead (e.g., to update a single property of a nested object), use
 * the `UpdateEntities` action instead.
 *
 * If the new versions do not contain any changes with respect to the
 * existing EntityState, nothing will be udpated.
 */
@ActionDeclaration(ENTITY)
export class AddEntities {
    static readonly type = 'AddEntities';
    constructor(public entities: Partial<EntityStateModel>) {}
}

/**
 * Updates existing entities in the EntityState.
 *
 * Use this action to apply small updates to existing entities.
 * The supplied entity versions will be applied to the existing ones
 * using a deep merge.
 */
@ActionDeclaration(ENTITY)
export class UpdateEntities {
    static readonly type = 'UpdateEntities';
    constructor(public partialEntities: RecursivePartial<EntityStateModel>) {}
}

/**
 * Removes entities from the EntityState.
 */
@ActionDeclaration(ENTITY)
export class DeleteEntities<T extends NormalizableEntityType> {
    static readonly type = 'DeleteEntities';
    constructor(
        public type: T,
        public entityIds: EntityIdType[],
    ) {}
}

@ActionDeclaration(ENTITY)
export class DeleteAllEntitiesInBranch<T extends NormalizableEntityType> {
    constructor(
        public type: T,
    ) {}
}

/**
 * Clears all entities from the EntityState.
 */
@ActionDeclaration(ENTITY)
export class ClearAllEntities {
    static readonly type = 'ClearAllEntities';
}
