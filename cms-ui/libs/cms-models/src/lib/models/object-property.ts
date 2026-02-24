import { CmsI18nValue } from './cms-i18n-value';
import { ObjectPropertyCategory } from './object-property-category';
import { TagType } from './tag';
import { DefaultModelType, ModelType, Normalizable, Raw } from './type-util';

export enum ObjectPropertiesObjectType {
    FOLDER = 10002,
    PAGE = 10007,
    IMAGE = 10011,
    FILE = 10008,
    TEMPLATE = 10006,
};

/** DevTools Package
 * @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Package.html
 */
export interface ObjectPropertyBase<T extends ModelType> {
    /** globald ID */
    globalId: string;
    /** Name in the current language */
    name?: string;
    /** Name in all available languages */
    nameI18n?: CmsI18nValue;
    /** Description in the current language */
    descriptionI18n?: CmsI18nValue;
    /** Keyword */
    keyword: string;
    /** Type of objects, this object property definition is for */
    type: ObjectPropertiesObjectType;
    /** Internal construct ID */
    constructId: number;
    /** Construct used by the object property (may be null, if not embedded in the response) */
    construct?: Normalizable<T, TagType<Raw>, number>;
    /** Get the category ID (may be null) */
    categoryId: number;
    /** Category used by the object property (may be null, if not embedded in the response) */
    category?: Normalizable<T, ObjectPropertyCategory<Raw>, number>;
    /** True if the object property is required, false if not */
    required: boolean;
    /** True if the object property is inheritable, false if not */
    inheritable: boolean;
    /** True if the object property is synchronized for all languages (only for pages) */
    syncContentset: boolean;
    /** True if the object property is synchronized for all channel variants */
    syncChannelset: boolean;
    /** True if the object property is synchronized for all page variants */
    syncVariants: boolean;
    /** True if the object property is restricted */
    restricted: boolean;
}

/** Data model as defined by backend. */
export interface ObjectProperty<T extends ModelType = DefaultModelType> extends ObjectPropertyBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

export type EditableObjectProperty = Omit<ObjectProperty, 'id' | 'globalId' | 'name' | 'construct'| 'category'>;

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ObjectPropertyBO<T extends ModelType = DefaultModelType> extends ObjectPropertyBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
