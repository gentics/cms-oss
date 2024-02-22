import { DefaultModelType, ModelType } from './type-util';
import { GcmsUiLanguage } from './ui-state';

interface BaseRole<T extends ModelType = DefaultModelType> {
    /** Name of the role in the current language. Only available when loading a role. */
    name: string;
    /** Description of the role in the current language. Only available when loading a role. */
    description: string;

    /** Name of the role in all possible translations */
    nameI18n: Record<GcmsUiLanguage, string>;

    /** Description in all possible translations */
    descriptionI18n: Record<GcmsUiLanguage, string>;
}

/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_RoleModel.html */
export interface Role<T extends ModelType = DefaultModelType> extends BaseRole<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface RoleBO<T extends ModelType = DefaultModelType> extends BaseRole<T> {
    /** Internal ID of the object property definition */
    id: string;
}

/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_RolePermissionsModel.html */
export interface RolePermissions {
    page: PagePrivileges;
    pageLanguages: Record<string, PagePrivileges>;
    file: FilePrivileges;
}


/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_PagePrivileges.html */
export interface PagePrivileges {
    viewpage: boolean;
    createpage: boolean;
    updatepage: boolean;
    deletepage: boolean;
    publishpage: boolean;
    translatepage: boolean;
}


/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_FilePrivileges.html */
export interface FilePrivileges {
    viewfile: boolean;
    createfile: boolean;
    updatefile: boolean;
    deletefile: boolean;
}
