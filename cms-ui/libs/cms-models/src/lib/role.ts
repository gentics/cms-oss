import { DefaultModelType, ModelType } from './type-util';

interface BaseRole<T extends ModelType = DefaultModelType> {
    /** Name of the role in the current language. Only available when loading a role. */
    name: string;
    /** Description of the role in the current language. Only available when loading a role. */
    description: string;

    /** Name of the role in all possible translations */
    nameI18n: Record<string, string>;

    /** Description in all possible translations */
    descriptionI18n: Record<string, string>;
}

/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_RoleModel.html */
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
/** @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_RolePermissionsModel.html */
export interface RolePermissions {
    page: PagePrivileges;
    pageLanguages: Record<string, PagePrivileges>;
    file: FilePrivileges;
}


/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_PagePrivileges.html */
export interface PagePrivileges {
    viewpage: boolean;
    createpage: boolean;
    updatepage: boolean;
    deletepage: boolean;
    publishpage: boolean;
    translatepage: boolean;
}


/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_FilePrivileges.html */
export interface FilePrivileges {
    viewfile: boolean;
    createfile: boolean;
    updatefile: boolean;
    deletefile: boolean;
}

/**
 * Determines whether a `Role` is assigned to a group.
 */
export interface RoleAssignment {

    /** Role ID. */
    id: number;

    /** Role label (translated by CMS). */
    label: string;

    /** Role description (translated by CMS). */
    description: string;

    /** `true` if role is assigned, `false` if not */
    value: boolean;

    /** `true` if the role assignment can be edited by the current user. */
    editable: boolean;

}
