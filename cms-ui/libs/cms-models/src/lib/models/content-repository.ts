import { DefaultModelType, ModelType } from './type-util';

/** Possible ContentRepository types
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Type.html
 */
export enum ContentRepositoryType {
    /** Normal CR (SQL based, no multichannelling) */
    CR = 'cr',
    /** Multichannelling aware CR (SQL based) */
    MCCR = 'mccr',
    /** Mesh CR */
    MESH = 'mesh',
}

/** Model of the Elasticsearch specific configuration for Mesh CRs
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_CRElasticsearchModel.html
 */
export interface CRElasticsearchModel {
    page: any;
    folder: any;
    file: any;
}

/**
 * Possible Check Status values
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Status.html
 */
export type ContentRepositoryCheckStatus =
    /** Check was never done */
    'unchecked' |
    /** Check produced an error */
    'error' |
    /** Check was ok */
    'ok' |
    /** Check is currently running (in background) */
    'running' |
    /** Check is queued to run in background */
    'queued';

/**
 * Possible Check Status values
 *
 * @see https://www.gentics.com/Content.Node/guides/restapi/json_Status.html
 */
export type ContentRepositoryDataStatus =
    /** Check was never done */
    'unchecked' |
    /** Check produced an error */
    'error' |
    /** Check was ok */
    'ok' |
    /** Check is currently running (in background) */
    'running' |
    /** Check is queued to run in background */
    'queued';

/**
 * Possible Password Type values
 */
export enum ContentRepositoryPasswordType {
    /** No password is set */
    NONE = 'none',
    /** The password is set as value */
    VALUE = 'value',
    /** The password is set as property */
    PROPERTY = 'property',
}

/**
 * Possible Username Type values
 */
export enum UsernameType {
    /** The username is set as value */
    VALUE = 'value',
    /** The username is set as property */
    PROPERTY = 'property',
}

/**
 * Possible URL Type values
 */
export enum UrlType {
    /** The URL is set as value */
    VALUE = 'value',
    /** The URL is set as property */
    PROPERTY = 'property',
}

/**
 * Possible basepath Type values
 */
export enum BasepathType {
    /** The basepath is set as value */
    VALUE = 'value',
    /** The basepath is set as property */
    PROPERTY = 'property',
}

/** @see https://www.gentics.com/Content.Node/guides/restapi/json_ContentRepositoryModel.html */
export interface ContentRepositoryBase<T extends ModelType> {
    /** Global ID */
    globalId: string;
    /** Name of the ContentRepository */
    name: string;
    /** Type of the ContentRepository */
    crType: ContentRepositoryType;
    /** DB Type of the ContentRepository */
    dbType: string;
    /** Username for accessing the ContentRepository */
    username: string;
    /** Username property for accessing the ContentRepository */
    usernameProperty: string;
    /** Password for accessing the ContentRepository */
    password: string;
    /** Property, which will resolve to the password. */
    passwordProperty: string;
    /** Type of password */
    passwordType: ContentRepositoryPasswordType;
    /** URL for accessing the ContentRepository */
    url: string;
    /** URL property for accessing the ContentRepository */
    urlProperty: string;
    /** Basepath for filesystem attributes */
    basepath: string;
    /** Basepath property for filesystem attributes */
    basepathProperty: string;
    /** Flag for instant publishing */
    instantPublishing: boolean;
    /** Flag for publishing language information */
    languageInformation: boolean;
    /** Flag for publishing permission information */
    permissionInformation: boolean;
    /** Property containing the permission (role) information for Mesh CRs */
    permissionProperty: string;
    /** Default permission (role) to be set on objects in Mesh CRs */
    defaultPermission: string;
    /** Flag for differential deleting of superfluous objects */
    diffDelete: boolean;
    /** Get the elasticsearch specific configuration of a Mesh CR */
    elasticsearch: CRElasticsearchModel;
    /** Flag for publishing every node into its own project for Mesh contentrepositories */
    projectPerNode: boolean;
    /** Implementation version of the Mesh ContentRepository */
    version: string;
    /** Date of last check of structure */
    checkDate: number;
    /** Status of last structure check */
    checkStatus: ContentRepositoryCheckStatus;
    /** Result of last structure check */
    checkResult: string;
    /** Date of data status (last publish process) */
    statusDate: number;
    /** Status of last data check */
    dataStatus: ContentRepositoryDataStatus;
    /** Result of last data check */
    dataCheckResult: string;
    /** HTTP/2 usage flag */
    http2: boolean;
    /** Exclude folders from indexing */
    noFoldersIndex: boolean;
    /** Exclude files from indexing */
    noFilesIndex: boolean;
    /** Exclude pages from indexing */
    noPagesIndex: boolean;
    /** Exclude forms from indexing */
    noFormsIndex: boolean;
}

/** Data model as defined by backend. */
export interface ContentRepository<T extends ModelType = DefaultModelType> extends ContentRepositoryBase<T> {
    /** Internal ID of the object property definition */
    id: number;
}

/**
 * Data model as defined by frontend.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface ContentRepositoryBO<T extends ModelType = DefaultModelType> extends ContentRepositoryBase<T> {
    /** Internal ID of the object property definition */
    id: string;
}
