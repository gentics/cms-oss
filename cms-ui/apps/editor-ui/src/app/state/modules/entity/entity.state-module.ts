import { Injectable } from '@angular/core';
import { FolderPrivileges } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { iif, patch } from '@ngxs/store/operators';
import { NormalizedSchema } from 'normalizr';
import { EntityState } from '../../../common/models';
import { deepEqual } from '../../../common/utils/deep-equal';
import { ApplicationStateService } from '../../providers/application-state/application-state.service';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    AddEntitiesAction,
    ApplyImageDimensionsAction,
    ClearEntitiesAction,
    ENTITIES_STATE_KEY,
    LockItemAction,
    PartialEntityHash,
    ResetPageLockAction,
    SetMessageEntitiesAction,
    UpdateEntitiesAction,
} from './entity.actions';

const INITIAL_ENTITIES_STATE: EntityState = {
    contentPackage: { },
    contentRepository: { },
    file: { },
    folder: { },
    form: { },
    group: { },
    image: { },
    language: { },
    message: { },
    node: { },
    page: { },
    template: { },
    user: { },
};

@AppStateBranch<EntityState>({
    name: ENTITIES_STATE_KEY,
    defaults: INITIAL_ENTITIES_STATE,
})
@Injectable()
export class EntityStateModule {

    constructor(
        private appState: ApplicationStateService,
    ) {}

    @ActionDefinition(AddEntitiesAction)
    handleAddEntitiesAction(ctx: StateContext<EntityState>, action: AddEntitiesAction): void {
        const state = ctx.getState();
        const newState = addNormalizedEntities(state, action.entities);

        if (state !== newState) {
            ctx.setState(newState);
        }
    }

    @ActionDefinition(UpdateEntitiesAction)
    handleUpdateEntitiesAction(ctx: StateContext<EntityState>, action: UpdateEntitiesAction): void {
        const state = ctx.getState();
        const newState = updateEntities(state, action.entities);

        if (state !== newState) {
            ctx.setState(newState);
        }
    }

    @ActionDefinition(ClearEntitiesAction)
    handleClearEntitiesAction(ctx: StateContext<EntityState>, action: ClearEntitiesAction): void {
        ctx.patchState({
            [action.entityType]: {},
        });
    }

    @ActionDefinition(ApplyImageDimensionsAction)
    handleApplyImageDimensionsAction(ctx: StateContext<EntityState>, action: ApplyImageDimensionsAction): void {
        const state = ctx.getState();

        ctx.setState(patch({
            image: patch({
                [action.imageId]: iif(state?.image?.[action.imageId] != null,
                    patch({
                        sizeX: action.sizeX,
                        sizeY: action.sizeY,
                    }),
                ),
            }),
        }));
    }

    @ActionDefinition(ResetPageLockAction)
    handleResetPageLockAction(ctx: StateContext<EntityState>, action: ResetPageLockAction): void {
        ctx.setState(patch({
            page: patch({
                [action.pageId]: patch({
                    lockedBy: null,
                    locked: false,
                    lockedSince: -1,
                }),
            }),
        }));
    }

    @ActionDefinition(LockItemAction)
    handleLockItemAction(ctx: StateContext<EntityState>, action: LockItemAction): void {
        const state = ctx.getState();
        const currentAppState = this.appState.now;
        const activeLanguage = currentAppState.folder.activeLanguage;
        const currentUserId = currentAppState.auth.currentUserId;

        let privilegeKey: keyof FolderPrivileges;

        if (action.itemType === 'page' && (action.editMode === 'edit' || action.editMode === 'editProperties')) {
            privilegeKey = 'updatepage';
        } else if (action.itemType === 'form' && (action.editMode === 'edit' || action.editMode === 'editProperties')) {
            privilegeKey = 'updateform';
        } else {
            // Ignore locking of all other entities
            return;
        }

        const parentFolder = state.folder[state[action.itemType][action.itemId].folderId];
        const userCanEditPage = !parentFolder
            || (
                parentFolder
                && parentFolder.privilegeMap
                && parentFolder.privilegeMap.privileges
                && parentFolder.privilegeMap.privileges[privilegeKey]
            )
            || (
                parentFolder
                && parentFolder.privilegeMap
                && parentFolder.privilegeMap.languages
                && parentFolder.privilegeMap.languages[activeLanguage]
                && parentFolder.privilegeMap.languages[activeLanguage].updatepage
            );

        // To make the UI more responsive, mark page as locked by the current user instead of
        // locking on the server, waiting for the response, and then fetching the item again.
        if (!userCanEditPage) {
            return;
        }

        // Also weird typing quick, that's why it's copy pasted.
        if (action.itemType === 'page') {
            ctx.setState(patch({
                [action.itemType]: patch({
                    [action.itemId]: patch({
                        locked: true,
                        lockedSince: Math.round(Date.now() / 1000),
                        lockedBy: currentUserId,
                    }),
                }),
            }));
        } else {
            ctx.setState(patch({
                [action.itemType]: patch({
                    [action.itemId]: patch({
                        locked: true,
                        lockedSince: Math.round(Date.now() / 1000),
                        lockedBy: currentUserId,
                    }),
                }),
            }));
        }
    }

    @ActionDefinition(SetMessageEntitiesAction)
    handleSetMessageEntitiesAction(ctx: StateContext<EntityState>, action: SetMessageEntitiesAction): void {
        ctx.patchState({
            message: action.messages,
        });
    }
}

/**
 * Applies the result of normalizr to the current entity state.
 * If the entity currently stored in the entity state is equal to the normalizr result,
 * the current reference is reused and no change will be emitted.
 */
export function addNormalizedEntities(currentEntityState: EntityState, normalized: NormalizedSchema<any, any>): EntityState {
    const branches = normalized.entities && Object.keys(normalized.entities);
    if (!branches || !branches.length) {
        return currentEntityState;
    }

    let anyBranchChanged = false;

    const newEntityState: any = Object.assign({}, currentEntityState);
    for (let branch of branches) {
        if ((currentEntityState as any)[branch] === undefined || newEntityState[branch] === undefined) {
            throw new Error('addNormalizedEntities: key ' + branch + ' is undefined');
        }

        let changed = false;
        const changedBranch = Object.assign({}, newEntityState[branch]);

        // TODO: This workaround of state logic is very bad architecture and should be removed as soon a better REST API allows for it!
        const protectedProperties: string[] = [ 'privilegeMap', 'permissionsMap' ];
        const savedProperties = {};

        for (let id of Object.keys(normalized.entities[branch])) {
            const newEntity = normalized.entities[branch][id];
            const oldEntity = changedBranch[id];

            // retain protected data
            protectedProperties.forEach(k => {
                if (oldEntity && oldEntity[k] && !newEntity[k]) {
                    savedProperties[k] = oldEntity[k];
                }
            });

            let updatedEntity = {...oldEntity, ...newEntity};
            let missingProperties = [];

            if (!oldEntity) {
                // Entity was not in entity state before
                changedBranch[id] = newEntity;
                changed = true;
            } else {
                // Entity was already in entity state. Only update values that changed
                let entityChanged = false;
                for (let prop of Object.keys(updatedEntity)) {
                    if (!newEntity.hasOwnProperty(prop) && !savedProperties[prop]) {
                        missingProperties.push(prop);
                    }
                    if (newEntity[prop] !== oldEntity[prop] && (typeof newEntity[prop] !== 'object' ||
                        newEntity[prop] === null || !deepEqual(oldEntity[prop], newEntity[prop]))) {
                        // Always create a new entity reference if a property changed
                        if (!entityChanged) {
                            changedBranch[id] = Object.assign({}, oldEntity);
                            changed = entityChanged = true;
                        }
                        if (missingProperties.length > 0) {
                            missingProperties.forEach(property => {
                                delete changedBranch[id][property];
                            });
                        }
                        // reassign properties required to be present if possible
                        if (savedProperties[prop]) {
                            newEntity[prop] = savedProperties[prop];
                        }
                        changedBranch[id][prop] = newEntity[prop];
                    }
                }
            }
        }

        if (changed) {
            newEntityState[branch] = changedBranch;
            anyBranchChanged = true;
        }
    }

    return anyBranchChanged ? newEntityState : currentEntityState;
}

/**
 * Updates entries in an entity hash with passed properties.
 * If no properties of the stored entity are changed, the current reference is reused and returned.
 */
export function updateEntities(currentEntityState: EntityState, updates: PartialEntityHash): EntityState {
    const branches = updates && Object.keys(updates) as Array<keyof PartialEntityHash>;
    if (!branches || !branches.length) {
        return currentEntityState;
    }

    // TODO: This workaround of state logic is very bad architecture and should be removed as soon a better REST API allows for it!
    const protectedProperties: string[] = [ 'privilegeMap', 'permissionsMap' ];
    const savedProperties = {};
    let anyBranchChanged = false;

    const newEntityState: any = Object.assign({}, currentEntityState);
    for (let branch of branches) {
        if (currentEntityState[branch] === undefined || newEntityState[branch] === undefined) {
            throw new Error('addNormalizedEntities: key ' + branch + ' is undefined');
        }

        let anyEntityChanged = false;
        const changedBranch = Object.assign({}, newEntityState[branch]);

        for (let id of Object.keys(updates[branch]).map(key => Number(key))) {
            if (!changedBranch[id]) {
                // No entity to apply updates to. For example:
                //     entities = { pages: {1: {...} } }
                //     updates = { pages: {3: {...} } }
                // TODO: throw?
                //
                // console.error(`updateEntities(): Trying to update ${branch}[${id}], which is not in the entity state.`);
                continue;
            }

            const currentEntity = { ...changedBranch[id] };
            const propsToChange: any = updates[branch][id];
            let currentEntityChanged = false;

            for (let key of Object.keys(propsToChange)) {
                const oldValue = currentEntity[key];
                const newValue = propsToChange[key];

                // retain protected data
                const originalValue = currentEntityState[branch][id][key];
                if (originalValue && !newValue && protectedProperties.includes(key)) {
                    savedProperties[key] = originalValue;
                }
                if (oldValue !== newValue && (typeof newValue !== 'object' || !newValue || !oldValue || !deepEqual(oldValue, newValue))) {
                    currentEntity[key] = newValue;
                    currentEntityChanged = true;
                }
                // reassign properties required to be present if possible
                if (savedProperties[key]) {
                    currentEntity[key] = savedProperties[key];
                }
            }

            if (currentEntityChanged) {
                anyEntityChanged = true;
                changedBranch[id] = currentEntity;
            }
        }

        if (anyEntityChanged) {
            newEntityState[branch] = changedBranch;
            anyBranchChanged = true;
        }
    }

    return anyBranchChanged ? newEntityState : currentEntityState;
}
