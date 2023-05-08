import {
    Folder,
    Image,
    ItemType,
    Page,
    Template,
} from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';
import { PartialUsageData } from './usage.state-module';

export const USAGE_STATE_KEY: keyof AppState = 'usage';

@ActionDeclaration(USAGE_STATE_KEY)
export class StartItemUsageFetchingAction {
    constructor(
        public itemType: ItemType,
        public itemId: number,
    ) {}
}

@ActionDeclaration(USAGE_STATE_KEY)
export class ItemUsageFetchingSuccessAction {
    constructor(
        public usageData: PartialUsageData | { [key: string]: Array<File | Folder | Image | Page | Template> },
    ) {}
}

@ActionDeclaration(USAGE_STATE_KEY)
export class ItemUsageFetchingErrorAction {}
