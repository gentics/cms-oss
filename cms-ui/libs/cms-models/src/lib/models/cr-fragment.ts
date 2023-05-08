import { DefaultModelType, ModelType } from './type-util';

/** @see https://www.gentics.com/Content.Node/guides/restapi/json_contentRepositoryFragmentModel.html */
export interface ContentRepositoryFragmentBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** Name */
    name: string;
}

/** Data model as defined by backend. */
export interface ContentRepositoryFragment<T extends ModelType = DefaultModelType> extends ContentRepositoryFragmentBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/** Data model as defined by frontend. */
export interface ContentRepositoryFragmentBO<T extends ModelType = DefaultModelType> extends ContentRepositoryFragmentBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
