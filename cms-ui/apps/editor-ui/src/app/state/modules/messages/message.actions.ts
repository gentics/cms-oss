import { MessageFromServer } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const MESSAGES_STATE_KEY: keyof AppState = 'messages';

@ActionDeclaration(MESSAGES_STATE_KEY)
export class StartMessagesFetchingAction {}

@ActionDeclaration(MESSAGES_STATE_KEY)
export class MessagesFetchingSuccessAction {
    constructor(
        public onlyUnread: boolean,
        public unread: MessageFromServer[],
        public all?: MessageFromServer[],
    ) {}
}

@ActionDeclaration(MESSAGES_STATE_KEY)
export class MessagesFetchingErrorAction {
    constructor (
        public errorMessage: string,
    ) {}
}

@ActionDeclaration(MESSAGES_STATE_KEY)
export class MessagesReadAction {
    constructor(
        public messageIds: number[],
    ) {}
}
