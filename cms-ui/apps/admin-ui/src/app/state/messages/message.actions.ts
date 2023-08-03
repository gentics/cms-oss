import { MessageFromServer } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';

const MESSAGE: keyof AppState = 'messages';

@ActionDeclaration(MESSAGE)
export class FetchAllMessageStart {
    static readonly TYPE = 'FetchAllMessageStart';
}

@ActionDeclaration(MESSAGE)
export class FetchAllMessageSuccess {
    static readonly TYPE = 'FetchAllMessageSuccess';
    constructor(
        public allMessagesFromServer: MessageFromServer[],
        public unreadMessagesFromServer: MessageFromServer[],
        public instantMessagesFromServer: MessageFromServer[],
    ) {}
}

@ActionDeclaration(MESSAGE)
export class FetchAllMessageError {
    static readonly type = 'FetchAllMessageError';
    constructor(public errorMessage: string) {}
}

@ActionDeclaration(MESSAGE)
export class FetchUnreadMessageStart {
    static readonly type = 'FetchUnreadMessageStart';
}

@ActionDeclaration(MESSAGE)
export class FetchUnreadMessageSuccess {
    static readonly TYPE = 'FetchUnreadMessageSuccess';
    constructor(public unreadMessagesFromServer: MessageFromServer[]) {}
}

@ActionDeclaration(MESSAGE)
export class FetchUnreadMessageError {
    static readonly TYPE = 'FetchUnreadMessageError';
    constructor(public errorMessage: string) {}
}

@ActionDeclaration(MESSAGE)
export class MarkMessagesAsRead {
    static readonly TYPE = 'MarkMessagesAsRead';
    constructor(public messageIds: number[]) {}
}

@ActionDeclaration(MESSAGE)
export class ClearMessageState {
    static readonly TYPE = 'ClearMessageState';
}

@ActionDeclaration(MESSAGE)
export class DeleteMessageError {
    static readonly TYPE = 'DeleteMessageError';
    constructor(public errorMessage: string) {}
}
