import { ModelType, DefaultModelType } from "./type-util";

/**
 * A language object as returned from the /markupLanguage endpoint:
 * @see https://www.gentics.com/Content.Node/guides/restapi/resource_MarkupLanguageResource.html
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_MarkupLanguage.html
 */
export interface MarkupLanguageBase<T extends ModelType> {
    /** Name */
    name: string;
    /** Extension */
    extension: string;
    /** ContentType */
    contentType: string;
}

export interface MarkupLanguage<T extends ModelType = DefaultModelType> extends MarkupLanguageBase<T> {
    /** ID of the markup language */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface MarkupLanguageBO<T extends ModelType = DefaultModelType> extends MarkupLanguageBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}
