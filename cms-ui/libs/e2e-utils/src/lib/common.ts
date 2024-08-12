import {
    AccessControlledType,
    EditableFileProps,
    FolderCreateRequest,
    GroupCreateRequest,
    GroupUserCreateRequest,
    NodeCreateRequest,
    NodeFeature,
    Page,
    PageCreateRequest,
    PermissionInfo,
} from '@gentics/cms-models';

export interface LoginInformation {
    username: string;
    password: string;
}

export enum TestSize {
    MINIMAL = 'minimal',
    FULL = 'full',
}

export type EntityMap = Record<string, any>;
export type BinaryMap = Record<string, File>;

export const LANGUAGE_EN = 'en';
export const LANGUAGE_DE = 'de';

export const ITEM_TYPE_FOLDER = 'folder';
export const ITEM_TYPE_PAGE = 'page';
export const ITEM_TYPE_FILE = 'file';
export const ITEM_TYPE_IMAGE = 'image';
export const ITEM_TYPE_FORM = 'form';

export const IMPORT_TYPE_NODE = 'node';
export const IMPORT_TYPE_USER = 'user';
export const IMPORT_TYPE_GROUP = 'group';

export const ENV_CMS_REST_PATH = 'CMS_REST_PATH';
export const ENV_CMS_ADMIN_PATH = 'CMS_ADMIN_PATH';
export const ENV_CMS_USERNAME = 'CMS_USERNAME';
export const ENV_CMS_PASSWORD = 'CMS_PASSWORD';
export const ENV_CMS_VARIANT = 'CMS_VARIANT';

export type ItemType = typeof ITEM_TYPE_FOLDER | typeof ITEM_TYPE_PAGE | typeof ITEM_TYPE_FILE | typeof ITEM_TYPE_IMAGE | typeof ITEM_TYPE_FORM;
export type ImportType = ItemType | typeof IMPORT_TYPE_NODE | typeof IMPORT_TYPE_USER | typeof IMPORT_TYPE_GROUP;

/** Type to determine how to import/delete the entity */
export const IMPORT_TYPE = Symbol('gtx-e2e-import-type');
/** ID which can be referenced in other entities to determine relationships. */
export const IMPORT_ID = Symbol('gtx-e2e-import-id');

export const BASIC_TEMPLATE_ID = '57a5.5db4acfa-3224-11ef-862c-0242ac110002';

export const OBJECT_PROPERTY_PAGE_COLOR = '994d.ff379678-37b9-11ef-a38e-0242ac110002';
export const OBJECT_PROPERTY_FOLDER_COLOR = 'a986.40be20e1-4318-11ef-bf28-0242ac110002';

export interface ImportBinary {
    /** The path to the fixture file to load. */
    fixturePath: string;
    /** The File name. If left empty, it'll be determined from the fixture-path. */
    name?: string;
    /** The mime-type of the binary, because cypress doesn't provide it. */
    type: string;
}

export interface ImportPermissions {
    type: AccessControlledType;
    instanceId?: string;
    subGroups?: boolean;
    subObjects?: boolean;
    perms: Pick<PermissionInfo, 'type' | 'value'>[];
}

export interface ImportData {
    [IMPORT_TYPE]: ImportType;
    [IMPORT_ID]: string;
}

export interface NodeImportData extends NodeCreateRequest, ImportData {
    [IMPORT_TYPE]: typeof IMPORT_TYPE_NODE;
    /** Language codes which will be assigned */
    languages: string[];
    /** Templates which will be assigned */
    templates?: string[];
}

export interface FolderImportData extends Omit<FolderCreateRequest, 'nodeId' | 'motherId'>, ImportData {
    [IMPORT_TYPE]: typeof ITEM_TYPE_FOLDER;

    /** The nodes `IMPORT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    motherId: string;
}

export interface PageImportData extends Omit<PageCreateRequest, 'nodeId' | 'folderId' | 'templateId'>, Partial<Pick<Page, 'tags'>>, ImportData {
    [IMPORT_TYPE]: typeof ITEM_TYPE_PAGE,

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
    /** The Global-ID of the template from the Dev-Tool Package */
    templateId: string;
}

export interface FileImportData extends EditableFileProps, ImportData {
    [IMPORT_TYPE]: typeof ITEM_TYPE_FILE,

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
}

export interface ImageImportData extends EditableFileProps, ImportData {
    [IMPORT_TYPE]: typeof ITEM_TYPE_IMAGE,

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
}

export interface GroupImportData extends GroupCreateRequest, ImportData {
    [IMPORT_TYPE]: typeof IMPORT_TYPE_GROUP,

    /** The parent `IMPORT_ID` value */
    parent?: string;

    permissions?: ImportPermissions[];
}

export interface UserImportData extends GroupUserCreateRequest, ImportData {
    [IMPORT_TYPE]: typeof IMPORT_TYPE_USER,

    /** The groups `IMPORT_ID` value */
    group: string;
}
