import { ActionDeclaration } from '@gentics/cms-components';
import { deepFreeze } from '@gentics/ui-core';

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
export class SetAlohaRessources {
    static readonly type = 'SetAlohaRessources';
    constructor(
        public jsFiles: string[],
        public cssFiles: string[],
    ) {}
}
