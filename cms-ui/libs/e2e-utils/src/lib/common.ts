import {
    AccessControlledType,
    FileUploadOptions,
    FolderCreateRequest,
    GroupCreateRequest,
    GroupUserCreateRequest,
    NodeCreateRequest,
    NodeFeature,
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

export const ENV_CMS_REST_PATH = 'CMS_REST_PATH';
export const ENV_CMS_EDITOR_PATH = 'CMS_EDITOR_PATH';
export const ENV_CMS_ADMIN_PATH = 'CMS_ADMIN_PATH';
export const ENV_CMS_USERNAME = 'CMS_USERNAME';
export const ENV_CMS_PASSWORD = 'CMS_PASSWORD';

export const ENV_MESH_CR_ENABLED = 'FEATURE_MESH_CR';
export const ENV_KEYCLOAK_ENABLED = 'FEATURE_KEYCLOAK';
export const ENV_MULTI_CHANNELING_ENABLED = 'FEATURE_MULTI_CHANNELING';
export const ENV_FORMS_ENABLED = 'FEATURE_FORMS';
export const ENV_CONTENT_STAGING_ENABLED = 'FEATURE_CONTENT_STAGING';

/** Type to determine how to import/delete the entity */
export const IMPORT_TYPE = Symbol('gtx-e2e-import-type');
/** ID which can be referenced in other entities to determine relationships. */
export const IMPORT_ID = Symbol('gtx-e2e-import-id');

export const BASIC_TEMPLATE_ID = '57a5.5db4acfa-3224-11ef-862c-0242ac110002';

export const OBJECT_PROPERTY_PAGE_COLOR = '994d.ff379678-37b9-11ef-a38e-0242ac110002';
export const OBJECT_PROPERTY_FOLDER_COLOR = 'a986.40be20e1-4318-11ef-bf28-0242ac110002';

export interface ImportPermissions {
    type: AccessControlledType;
    instanceId?: string;
    subGroups?: boolean;
    subObjects?: boolean;
    perms: Pick<PermissionInfo, 'type' | 'value'>[];
}

export interface ImportData {
    [IMPORT_TYPE]: 'node' | 'folder' | 'page' | 'file' | 'image' | 'user' | 'group';
    [IMPORT_ID]: string;
}

export interface NodeImportData extends NodeCreateRequest, ImportData {
    [IMPORT_TYPE]: 'node';
    /** Language codes which will be assigned */
    languages: string[];
    /** Features which will be assigned */
    features: NodeFeature[];
    /** Templates which will be assigned */
    templates?: string[];
}

export interface FolderImportData extends Omit<FolderCreateRequest, 'nodeId' | 'motherId'>, ImportData {
    [IMPORT_TYPE]: 'folder';

    /** The nodes `IMPORT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    motherId: string;
}

export interface PageImportData extends Omit<PageCreateRequest, 'nodeId' | 'folderId' | 'templateId'>, ImportData {
    [IMPORT_TYPE]: 'page',

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
    /** The Global-ID of the template from the Dev-Tool Package */
    templateId: string;
}

export interface FileImportData extends Omit<FileUploadOptions, 'folderId' | 'nodeId'>, ImportData {
    [IMPORT_TYPE]: 'file',

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
    /** The file/blob to upload */
    data: Blob | File;
}

export interface ImageImportData extends Omit<FileUploadOptions, 'folderId' | 'nodeId'>, ImportData {
    [IMPORT_TYPE]: 'image',

    /** The nodes `IMPROT_ID` value */
    nodeId: string;
    /** The folders/nodes `IMPORT_ID` value */
    folderId: string;
    /** The file/blob to upload */
    data: Blob | File;
}

export interface GroupImportData extends GroupCreateRequest, ImportData {
    [IMPORT_TYPE]: 'group',

    /** The parent `IMPORT_ID` value */
    parent?: string;

    permissions?: ImportPermissions[];
}

export interface UserImportData extends GroupUserCreateRequest, ImportData {
    [IMPORT_TYPE]: 'user',

    /** The groups `IMPORT_ID` value */
    group: string;
}
