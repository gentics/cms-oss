import { CmsI18nValue } from './cms-i18n-value';
import { DefaultModelType, ModelType } from './type-util';

/**
 * DevTools Package
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Package.html
 */
export interface ObjectPropertyCategoryBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** Name in the current language */
    name?: string;
    /** Name in all languages */
    nameI18n?: CmsI18nValue;
}

/** Data model as defined by backend. */
export interface ObjectPropertyCategory<T extends ModelType = DefaultModelType> extends ObjectPropertyCategoryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ObjectPropertyCategoryBO<T extends ModelType = DefaultModelType> extends ObjectPropertyCategoryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
