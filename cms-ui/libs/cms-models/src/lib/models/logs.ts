import { DefaultModelType, ModelType } from './type-util';

export interface LogTypeListItem {
    name: string;
    label: string;
}

export interface ActionLogType {
    name: string;
    label: string;
}

export interface Action {
    name: string;
    label: string;
}

/**
 * A logs object as returned from the `admin/actionlog` endpoint:
 */
export interface ActionLogEntryBase<T extends ModelType> {
    date: number;
    action: Action;
    user: string;
    type: ActionLogType;
    objId: number;
    info: any;
}

export interface ActionLogEntry<T extends ModelType = DefaultModelType> extends ActionLogEntryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/** Data model as defined by frontend. */
export interface LogsBO<T extends ModelType = DefaultModelType> extends ActionLogEntryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
