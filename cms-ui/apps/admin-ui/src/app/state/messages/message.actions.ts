import { MaintenanceModeResponse, MessageFromServer } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';

const MESSAGE: keyof AppState = 'messages';

@ActionDeclaration(MESSAGE)
export class FetchAllMessageStart {
    static readonly type = 'FetchAllMessageStart';
}

@ActionDeclaration(MESSAGE)
export class FetchAllMessageSuccess {
    static readonly type = 'FetchAllMessageSuccess';
    constructor(public allMessagesFromServer: MessageFromServer[], public unreadMessagesFromServer: MessageFromServer[]) {}
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
    static readonly type = 'FetchUnreadMessageSuccess';
    constructor(public unreadMessagesFromServer: MessageFromServer[]) {}
}

@ActionDeclaration(MESSAGE)
export class FetchUnreadMessageError {
    static readonly type = 'FetchUnreadMessageError';
    constructor(public errorMessage: string) {}
}

@ActionDeclaration(MESSAGE)
export class MarkMessagesAsRead {
    static readonly type = 'MarkMessagesAsRead';
    constructor(public messageIds: number[]) {}
}

@ActionDeclaration(MESSAGE)
export class ClearMessageState {
    static readonly type = 'ClearMessageState';
}

@ActionDeclaration(MESSAGE)
export class DeleteMessageError {
    static readonly type = 'DeleteMessageError';
    constructor(public errorMessage: string) {}
}
