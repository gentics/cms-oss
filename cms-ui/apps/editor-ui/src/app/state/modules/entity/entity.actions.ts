import { EditMode, Message, Normalized } from '@gentics/cms-models';
import { NormalizedSchema } from 'normalizr';
import { AppState, EntityState, EntityTypesMap } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export interface PartialNumberHash<T> { [id: number]: Partial<T>; }
export type PartialEntityHash = { [K in keyof EntityTypesMap]?: PartialNumberHash<EntityTypesMap[K]> };

export const ENTITIES_STATE_KEY: keyof AppState = 'entities';

@ActionDeclaration(ENTITIES_STATE_KEY)
export class AddEntitiesAction {
    constructor(
        public entities: NormalizedSchema<any, any>,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class UpdateEntitiesAction {
    constructor(
        public entities: PartialEntityHash,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class ApplyImageDimensionsAction {
    constructor(
        public imageId: number,
        public sizeX: number,
        public sizeY: number,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class ClearEntitiesAction {
    constructor(
        public entityType: keyof EntityState,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class ResetPageLockAction {
    constructor(
        public pageId: number,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class LockItemAction {
    constructor(
        public itemType: 'page' | 'form',
        public itemId: number,
        public editMode: EditMode,
    ) {}
}

@ActionDeclaration(ENTITIES_STATE_KEY)
export class SetMessageEntitiesAction {
    constructor(
        public messages: { [id: number]: Message<Normalized> },
    ) {}
}
