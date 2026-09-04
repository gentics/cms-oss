import { ActionDeclaration } from '@gentics/cms-components';
import { deepFreeze } from '@gentics/common';

const MODULE_STATE = 'aloha';

export interface AlohaStateModel {
    jsFiles: string[];
    cssFiles: string[];
}

export const INITIAL_ALOHA_STATE = deepFreeze<AlohaStateModel>({
    jsFiles: [],
    cssFiles: [],
});

@ActionDeclaration(MODULE_STATE)
export class SetAlohaResources {
    static readonly type = 'SetAlohaResources';
    constructor(
        public jsFiles: string[],
        public cssFiles: string[],
    ) {}
}
