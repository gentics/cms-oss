import { DefaultModelType, ModelType } from './type-util';

/** @see https://gentics.com/Content.Node/guides/restapi/json_DataSourceEntryModel.html */
export interface DataSourceEntryBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** DataSource ID */
    dsId: number;
    /** Entry Key */
    key: string;
    /** Entry Value */
    value: string;
}

/** Data model as defined by backend. */
export interface DataSourceEntry<T extends ModelType = DefaultModelType> extends DataSourceEntryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface DataSourceEntryBO<T extends ModelType = DefaultModelType> extends DataSourceEntryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
