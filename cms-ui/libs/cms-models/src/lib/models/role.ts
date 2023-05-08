import { GcmsUiLanguage } from '../gcms-ui-bridge';
import { DefaultModelType, Index, ModelType } from './type-util';

/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_RoleModel.html */
export interface Role<T extends ModelType = DefaultModelType> {
    /** Internal ID of the object property definition */
    id: number;

    /** Name of the role in all possible translations */
    name: Index<GcmsUiLanguage, string>;

    /** Description in all possible translations */
    description: Index<GcmsUiLanguage, string>;
}

/** Data model as defined by frontend. */
export interface RoleBO<T extends ModelType = DefaultModelType> {
    /** Internal ID of the object property definition */
    id: string;

    name: string;
    description: string;
}

/** Data model as defined by backend. */
/** @see https://www.gentics.com/Content.Node/guides/restapi/json_RolePermissionsModel.html */
export interface RolePermissions {
    page: PagePrivileges;
    pageLanguages: Index<string, PagePrivileges>;
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
