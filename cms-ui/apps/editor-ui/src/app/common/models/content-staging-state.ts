import { StagedItemsMap } from '@gentics/cms-models';

export interface ContentStagingState {
    fetching: boolean;
    lastError?: Error;
    activePackage: string;
    stagingMap: StagedItemsMap;
}
