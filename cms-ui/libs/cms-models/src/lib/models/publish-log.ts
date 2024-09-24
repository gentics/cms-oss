import { BaseListOptionsWithPaging } from './request';
import { User } from './user';


export interface PublishLogEntry {
    objId: number;
    type: PublishType;
    state: PublishState;
    user: User;
    date: string;
}

export enum PublishType {
    PAGE = 'PAGE',
    FORM = 'FORM',
}

export enum PublishState {
    ONLINE = 'ONLINE',
    OFFLINE = 'OFFLINE',
}

export interface PublishLogListOption extends BaseListOptionsWithPaging<PublishLogEntry> {
    objId?: number;
    type?: string;
}

