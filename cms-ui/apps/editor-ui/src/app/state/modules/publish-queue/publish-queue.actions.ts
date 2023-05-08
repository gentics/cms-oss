import { FolderItemOrTemplateType, Page, Raw, User } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const PUBLISH_QUEUE_STATE_KEY: keyof AppState = 'publishQueue';

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class StartAssigningUsersToPagesAction {}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class AssigningUsersToPagesSuccessAction {
    constructor(
        public pageIds: number[],
    ) {}
}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class AssigningUsersToPagesErrorAction {}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class StartPublishQueueFetchingAction {}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class PublishQueuePagesFetchingSuccessAction {
    constructor(
        public pages: Page<Raw>[],
    ) {}
}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class PublishQueueUsersFetchingSuccessAction {
    constructor(
        public users: User<Raw>[],
    ) {}
}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class PublishQueueFetchingErrorAction {}

@ActionDeclaration(PUBLISH_QUEUE_STATE_KEY)
export class SetPublishQueueListDisplayFieldsAction {
    constructor(
        public type: FolderItemOrTemplateType,
        public displayFields: string[],
    ) {}
}
