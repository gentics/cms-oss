import { BaseListOptionsWithPaging } from './request';


export interface PublishLogEntry {
    objId: number;
    type: PublishType;
    state: PublishState;
    user: number;
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
}

