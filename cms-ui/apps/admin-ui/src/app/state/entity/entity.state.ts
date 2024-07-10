import { Injectable } from '@angular/core';
import { EntityIdType, NormalizableEntityType, NormalizedEntityStore, NormalizedEntityStoreBO, RecursivePartial } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import {
    cloneDeep as _cloneDeep,
    isEqual as _isEqual,
    isMatchWith as _isMatchWith,
    mergeWith as _mergeWith,
} from'lodash-es'
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import { AddEntities, ClearAllEntities, DeleteAllEntitiesInBranch, DeleteEntities, UpdateEntities } from './entity.actions';

export interface EntityStateModel extends NormalizedEntityStore {}
export interface EntityStateModelBO extends NormalizedEntityStoreBO {}

export const INITIAL_ENTITY_STATE = defineInitialState<EntityStateModel>({
    construct: {},
    constructCategory: {},
    contentPackage: {},
    contentRepository: {},
    contentRepositoryFragment: {},
    dataSource: {},
    dataSourceEntry: {},
    elasticSearchIndex: {},
    file: {},
    folder: {},
    form: {},
    group: {},
    image: {},
    language: {},
    logs: {},
    markupLanguage: {},
    message: {},
    node: {},
    objectProperty: {},
    objectPropertyCategory: {},
    package: {},
    page: {},
    role: {},
    schedule: {},
    scheduleExecution: {},
    scheduleTask: {},
    tagmapEntry: {},
    template: {},
    templateTag: {},
    templateTagStatus: {},
    user: {},
});

/**
 * Used for functions that given an old (existing) version of an entity
 * and a new version of it, return an updated version (a new object) that
 * contains the merged changes or `null` if there were no changes.
 */
type AssembleUpdatedEntityFn<T> = (oldEntity: T, newEntity: T) => T | null;

@AppStateBranch<EntityStateModel>({
    name: 'entity',
    defaults: INITIAL_ENTITY_STATE,
})
@Injectable()
export class EntityStateModule {

// ToDo: Refactor addEntities() and assembleStateUpdates() to use `batchedReduce()`.
// Note: This requires the implementation of a queue for incoming EntityState actions to make sure
// that all state operations are executed sequentially. Otherwise a long running action could overwrite
// changes made by a short action that dispatched after the long one completes.

    @ActionDefinition(AddEntities)
    addEntities(ctx: StateContext<EntityStateModel>, action: AddEntities): void {
        const currState = ctx.getState();

        const updates = this.assembleStateUpdates(currState, action.entities, (oldEntity, newEntity) => {
            // If there is no oldEntity, we are adding a new entity to the state,
            // otherwise we update an existing entity using a shallow merge.
            if (!oldEntity) {
                return newEntity;
            } else {
                return this.shallowMergeEntity(oldEntity, newEntity);
            }
        });

        if (updates) {
            ctx.patchState(updates);
        }
    }

    @ActionDefinition(UpdateEntities)
    updateEntities(ctx: StateContext<EntityStateModel>, action: UpdateEntities): void {
        const currState = ctx.getState();

        const updates = this.assembleStateUpdates(currState, action.partialEntities, (oldEntity, changes) => {
            // If there is no existing entity to be updated, do nothing.
            if (!oldEntity) {
                return null;
            }

            const anyChanges = !_isMatchWith(oldEntity, changes, (oldValue, newValue) => {
                // isMatch() would normally treat two arrays [ a, b ] and [ ] as a match,
                // but we need them to be different, because we want to be able to replace
                // an existing array with an empty one.
                if (Array.isArray(oldValue) || Array.isArray(newValue)) {
                    return _isEqual(oldValue, newValue);
                }
                return undefined;
            });

            if (anyChanges) {
                const modifiedEntity = _cloneDeep(oldEntity);
                return _mergeWith(modifiedEntity, changes, (oldValue, newValue) => {
                    // We don't want to merge arrays, but overwrite them.
                    if (Array.isArray(oldValue) || Array.isArray(newValue)) {
                        return newValue;
                    }
                    return undefined;
                });
            }
        });

        if (updates) {
            ctx.patchState(updates);
        }
    }

    @ActionDefinition(DeleteEntities)
    deleteEntities<T extends NormalizableEntityType>(
        ctx: StateContext<EntityStateModel>,
        action: DeleteEntities<T>,
    ): void {
        const currState = ctx.getState();
        const currEntitiesBranch = currState[action.type];
        const entitiesLatest = Object.keys(currEntitiesBranch)
            .map((entityId: EntityIdType) => entityId.toString())
            .filter((entityId: string) => !action.entityIds.map((id: number | string) => id.toString()).includes(entityId))
            .reduce((result, current) => {
                result[current] = currEntitiesBranch[current];
                return result;
            }, {});
        const updates = { ...currState };
        updates[action.type] = entitiesLatest;

        if (updates) {
            ctx.patchState(updates);
        }
    }

    @ActionDefinition(DeleteAllEntitiesInBranch)
    deleteAllEntitiesInBranch<T extends NormalizableEntityType>(
        ctx: StateContext<EntityStateModel>,
        action: DeleteEntities<T>,
    ): void {
        const currState = ctx.getState();
        const entitiesLatest = {};
        const updates = { ...currState };
        updates[action.type] = entitiesLatest;

        if (updates) {
            ctx.patchState(updates);
        }
    }

    @ActionDefinition(ClearAllEntities)
    clearEntities(ctx: StateContext<EntityStateModel>): void {
        ctx.setState(INITIAL_ENTITY_STATE);
    }

    /**
     * Iterates over all new entities and calls `assembleUpdatedEntity()` for
     * each [old, new] entity pair. If this callback returns a modified entity,
     * it is placed in the respective EntityState branch.
     *
     * @returns A partial EntityStateModel with all the branches that have been
     * updated (the complete branches containing both the existing and the modified entities)
     * or `null` if there were no changes.
     */
    private assembleStateUpdates(
        currState: EntityStateModel,
        newEntities: EntityStateModel | RecursivePartial<EntityStateModel>,
        assembleUpdatedEntity: AssembleUpdatedEntityFn<any>,
    ): Partial<EntityStateModel> {
        const updates: Partial<EntityStateModel> = {};
        let stateModified = false;

        Object.keys(newEntities).forEach(branchName => {
            let branchModified = false;
            const changesBranch = newEntities[branchName];
            const updatedExistingBranch = { ...currState[branchName] };

            Object.keys(changesBranch).forEach(id => {
                const oldEntity = updatedExistingBranch[id];
                const newEntity = changesBranch[id];
                const modifiedEntity = assembleUpdatedEntity(oldEntity, newEntity);
                if (modifiedEntity) {
                    updatedExistingBranch[id] = modifiedEntity;
                    branchModified = true;
                }
            });

            if (branchModified) {
                updates[branchName] = updatedExistingBranch;
                stateModified = true;
            }
        });

        if (stateModified) {
            return updates;
        } else {
            return null;
        }
    }

    /**
     * Performs a shallow merge of the two entity objects into a new object.
     * @returns The new, merged version of the entity or `null` if there were no changes.
     */
    private shallowMergeEntity<T>(oldEntity: T, newEntity: T): T {
        let modifiedEntity: T;
        Object.keys(newEntity).forEach(key => {
            const oldProp = oldEntity[key];
            const newProp = newEntity[key];
            if (oldProp !== newProp && (typeof newProp !== 'object' || !_isEqual(oldProp, newProp))) {
                if (!modifiedEntity) {
                    modifiedEntity = { ...oldEntity };
                }
                modifiedEntity[key] = newProp;
            }
        });
        return modifiedEntity;
    }

}
