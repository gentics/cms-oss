import { Item, SortField } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';
import { WastebinItemType } from './wastebin.state-module';

export const WASTE_BIN_STATE_KEY: keyof AppState = 'wastebin';

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class StartWasteBinItemsFetchingAction {
    constructor(
        public itemType: WastebinItemType,
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class WasteBinItemsFetchingSuccessAction {
    constructor(
        public itemType: WastebinItemType,
        public items: Item[],
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class WasteBinItemsFetchingErrorAction {
    constructor(
        public itemType: WastebinItemType,
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class StartWasteBinItemsDeletionAction {
    constructor(
        public itemType: WastebinItemType,
        public ids: number[],
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class WasteBinItemsDeletionSuccessAction {
    constructor(
        public itemType: WastebinItemType,
        public ids: number[],
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class WasteBinItemsDeletionErrorAction {
    constructor(
        public itemType: WastebinItemType,
        public ids: number[],
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class RestoreWasteBinItemsAction {
    constructor(
        public itemType: WastebinItemType,
        public ids: number[],
    ) {}
}

@ActionDeclaration(WASTE_BIN_STATE_KEY)
export class SetWasteBinSortingAction {
    constructor(
        public sortBy: SortField,
        public sortOrder: 'asc' | 'desc',
    ) {}
}
