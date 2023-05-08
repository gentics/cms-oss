import { DefaultModelType, ModelType } from './type-util';

/**
 * Possible DataSource types
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_DataSourceType.html
 */
export type DataSourceType =
    /** Static dataSource */
    | 'STATIC'
    /**
     * Siteminder dataSource.
     *
     * @deprecated DO NOT USE. Only here for compatibility reasons.
     */
    | 'SITEMINDER';

/** @see https://www.gentics.com/Content.Node/guides/restapi/json_DataSource.html */
export interface DataSourceBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** DataSource type */
    type: DataSourceType;
    /** Name */
    name: string;
}

/** Data model as defined by backend. */
export interface DataSource<T extends ModelType = DefaultModelType> extends DataSourceBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/** Data model as defined by frontend. */
export interface DataSourceBO<T extends ModelType = DefaultModelType> extends DataSourceBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
