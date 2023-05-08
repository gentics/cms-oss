import { StagedItemsMap, StagingStatus } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const CONTENT_STAGING_STATE_KEY: keyof AppState = 'contentStaging';

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class LoadContentPackagesAction {}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class ContentPackageSuccessAction {}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class ContentPackageErrorAction {
    constructor(
        public error: Error,
    ) {}
}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class SetActiveContentPackageAction {
    constructor(
        public name: string | null,
    ) {}
}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class ClearContentStagingMapAction {}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class RemoveContentStagingEntryAction {
    constructor(
        public entityId: string,
    ) {}
}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class AddContentStagingEntryAction {
    constructor(
        public entityId: string,
        public status: StagingStatus,
    ) {}
}

@ActionDeclaration(CONTENT_STAGING_STATE_KEY)
export class AddContentStagingMapAction {
    constructor(
        public map: StagedItemsMap,
    ) {}
}
