import { CmsI18nValue } from './cms-i18n-value';
import { TagType } from './tag';
import { DefaultModelType, ModelType } from './type-util';

/** Construct Category */
export interface ConstructCategoryBase<T extends ModelType> {
    /** Name of this construct category */
    name: string
    /** Map of constructs for this category */
    constructs: Record<string, TagType<T>>;
    /** Global ID */
    globalId: string;
    /** Map of names in the different languages */
    nameI18n: CmsI18nValue;
    /** The sort order of the category */
    sortOrder?: number;
}

/** Data model as defined by backend. */
export interface ConstructCategory<T extends ModelType = DefaultModelType> extends ConstructCategoryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ConstructCategoryBO<T extends ModelType = DefaultModelType> extends ConstructCategoryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
