import { AlohaCoreComponentNames } from '@gentics/aloha-models';
import {
    AccessControlledType,
    EditableFileProps,
    FolderCreateRequest,
    GroupCreateRequest,
    GroupUserCreateRequest,
    NodeCreateRequest,
    Page,
    PageCreateRequest,
    PermissionInfo,
    ScheduleCreateReqeust,
    ScheduleTaskCreateRequest,
    Variant,
} from '@gentics/cms-models';

export interface LoginInformation {
    username: string;
    password: string;
}

export enum TestSize {
    NONE = 'none',
    MINIMAL = 'minimal',
    FULL = 'full',
}

export type EntityMap = Record<string, any>;
export type BinaryMap = Record<string, File>;

export interface ImportBinary {
    /** The path to the fixture file to load. */
    fixturePath: string;
    /** The File name. If left empty, it'll be determined from the fixture-path. */
    name?: string;
    /** The mime-type of the binary, because cypress doesn't provide it. */
    type: string;
}
export interface ContentFile {
    contents: string | Buffer;
    fileName: string;
    mimeType: string;
}

export interface BinaryFileLoadOptions extends BinaryLoadOptions {}

export interface BinaryContentFileLoadOptions extends BinaryLoadOptions {
    asContent: true;
}

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

export const IMPORT_TYPE_TASK = 'task';
export const IMPORT_TYPE_SCHEDULE = 'schedule';

export const ENV_CMS_IMPORTER_USERNAME = 'CMS_IMPORTER_USERNAME';
export const ENV_CMS_IMPORTER_PASSWORD = 'CMS_IMPORTER_PASSWORD';

export const ENV_CMS_VARIANT = 'CMS_VARIANT';
export const ENV_CI = 'CI';
export const ENV_BASE_URL = 'BASE_URL';
export const ENV_KEYCLOAK_URL = 'KEYCLOAK_URL';
export const ENV_FORCE_REPEATS = 'FORCE_REPEATS';

export const ENV_LOCAL_PLAYWRIGHT = 'LOCAL_PLAYWRIGHT';
export const ENV_LOCAL_APP = 'LOCAL_APP';
export const ENV_SKIP_LOCAL_APP_LAUNCH = 'SKIP_LOCAL_APP_LAUNCH';

export const DEFAULT_KEYCLOAK_URL = 'http://keycloak.localhost.gentics.com';

export const ATTR_CONTEXT_ID = 'data-context-id';

declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    namespace NodeJS {
        interface ProcessEnv {
            /** Username override for setup rest calls */
            [ENV_CMS_IMPORTER_USERNAME]?: string;
            /** Password override for setup rest calls */
            [ENV_CMS_IMPORTER_PASSWORD]?: string;
            /** The CMS Variant that is being tested. */
            [ENV_CMS_VARIANT]: Variant;
            /** Flag which determines if we're running in a CI context. */
            [ENV_CI]: boolean | string | number;
            /** Override for the URL where the app to test is reachable. */
            [ENV_BASE_URL]?: string;
            /** Override for the URL where keycloak is reachable. */
            [ENV_KEYCLOAK_URL]?: string;
            /** If it should force repeats of intergration tests. */
            [ENV_FORCE_REPEATS]?: boolean | string | number;
            /** If it should use the local playwright server instead of the container. */
            [ENV_LOCAL_PLAYWRIGHT]?: boolean | string | number;
            /** If it should use the local application instead of the application in the container. */
            [ENV_LOCAL_APP]?: boolean | string | number;
            /** If it should not automatically launch the local application. */
            [ENV_SKIP_LOCAL_APP_LAUNCH]?: boolean | string | number;
        }
    }
}

export const ENV_ALOHA_PLUGIN_CITE = 'ALOHA_PLUGIN_CITE';

export type ItemType = typeof ITEM_TYPE_FOLDER | typeof ITEM_TYPE_PAGE | typeof ITEM_TYPE_FILE | typeof ITEM_TYPE_IMAGE | typeof ITEM_TYPE_FORM;
export type ImportType = ItemType
| typeof IMPORT_TYPE_NODE
| typeof IMPORT_TYPE_USER
| typeof IMPORT_TYPE_GROUP
| typeof IMPORT_TYPE_TASK
| typeof IMPORT_TYPE_SCHEDULE;

/** Type to determine how to import/delete the entity */
export const IMPORT_TYPE = Symbol('gtx-e2e-import-type');
/** ID which can be referenced in other entities to determine relationships. */
export const IMPORT_ID = Symbol('gtx-e2e-import-id');

export const BASIC_TEMPLATE_ID = '57a5.5db4acfa-3224-11ef-862c-0242ac110002';

export const CONTENT_REPOSITORY_MESH = '7f25.8f630a60-355e-11ef-8e0a-0242ac110002';
export const CR_PREFIX_MESH = 'example';

export const CONSTRUCT_ALOHA_LINK = 'A547.70950';
export const CONSTRUCT_ALOHA_TEXT = 'A547.75403';
export const CONSTRUCT_TEXT_SHORT = 'A547.69478';
export const CONSTRUCT_VELOCITY = 'A547.69716';
export const CONSTRUCT_BOOLEAN = 'A547.69744';
export const CONSTRUCT_URL_IMAGE = 'A547.69940';
export const CONSTRUCT_URL_FILE = 'A547.14546';
export const CONSTRUCT_URL_FOLDER = 'B230.93539';
export const CONSTRUCT_URL_PAGE = 'A547.69527';

export const CORE_CONSTRUCTS = [
    CONSTRUCT_ALOHA_LINK,
    CONSTRUCT_ALOHA_TEXT,
    CONSTRUCT_TEXT_SHORT,
    CONSTRUCT_VELOCITY,
    CONSTRUCT_BOOLEAN,
    CONSTRUCT_URL_IMAGE,
    CONSTRUCT_URL_FILE,
    CONSTRUCT_URL_FOLDER,
    CONSTRUCT_URL_PAGE,
];

export const OBJECT_PROPERTY_PAGE_COLOR = '994d.ff379678-37b9-11ef-a38e-0242ac110002';
export const OBJECT_PROPERTY_FOLDER_COLOR = 'a986.40be20e1-4318-11ef-bf28-0242ac110002';

// Internal tasks, which are defined by their internal commands
export const TASK_CONVERT_IMAGES = 'convertimages';
export const TASK_LINK_CHECKER = 'linkchecker';
export const TASK_PURGE_MESSAGES = 'purgemessages';
export const TASK_PURGE_LOGS = 'purgelogs';
export const TASK_PURGE_VERSIONS = 'purgeversions';
export const TASK_PURGE_WASTEBIN = 'purgebastebin';
export const TASK_PUBLISH = 'publish';

export const INTERNAL_TASKS = [
    TASK_CONVERT_IMAGES,
    TASK_LINK_CHECKER,
    TASK_PURGE_MESSAGES,
    TASK_PURGE_LOGS,
    TASK_PURGE_VERSIONS,
    TASK_PURGE_WASTEBIN,
    TASK_PUBLISH,
];

export const BOUNDARY_NODES = new Set([
    'article',
    'aside',
    'body',
    'canvas',
    'col',
    'colgroup',
    'datalist',
    'details',
    'dialog',
    'div',
    'embed',
    'fieldset',
    'figure',
    'footer',
    'form',
    'header',
    'hgroup',
    'iframe',
    'input',
    'main',
    'menu',
    'meter',
    'nav',
    'object',
    'optgroup',
    'option',
    'progress',
    'search',
    'section',
    'select',
    'slot',
    'summary',
    'table',
    'template',
    'textarea',
    'thead',
    'tbody',
    'tfoot',
    'video',
]);
/** Nodes which can simply appear in a formatted flow text */
export const IGNORE_NODES = new Set([
    'a',
    'bdi',
    'bdo',
    'br',
    'dd',
    'dl',
    'dt',
    'ul',
    'ol',
    'li',
    'hr',
    'img',
    'wbr',
    'p',
    'picture',
    'span',
    'tr',
    'td',
]);
/** Nodes which dedicate formatting, but nothing else */
export const FORMATTING_NODES = new Set([
    'abbr',
    'b',
    'blockquote',
    'cite',
    'code',
    'data',
    'del',
    'dfn',
    'em',
    'h1',
    'h2',
    'h3',
    'h4',
    'h5',
    'h6',
    'i',
    'ins',
    'kbd',
    'mark',
    'pre',
    'q',
    'output',
    's',
    'samp',
    'small',
    'strong',
    'sub',
    'sup',
    'time',
    'u',
    'var',
]);

export const RENDERABLE_ALOHA_COMPONENTS: Record<string, string> = [
    AlohaCoreComponentNames.ATTRIBUTE_BUTTON,
    AlohaCoreComponentNames.ATTRIBUTE_TOGGLE_BUTTON,
    AlohaCoreComponentNames.BUTTON,
    AlohaCoreComponentNames.CHECKBOX,
    AlohaCoreComponentNames.COLOR_PICKER,
    AlohaCoreComponentNames.CONTEXT_BUTTON,
    AlohaCoreComponentNames.CONTEXT_TOGGLE_BUTTON,
    AlohaCoreComponentNames.DATE_TIME_PICKER,
    AlohaCoreComponentNames.IFRAME,
    AlohaCoreComponentNames.INPUT,
    AlohaCoreComponentNames.LINK_TARGET,
    AlohaCoreComponentNames.SELECT,
    AlohaCoreComponentNames.SELECT_MENU,
    AlohaCoreComponentNames.SPLIT_BUTTON,
    AlohaCoreComponentNames.SYMBOL_GRID,
    AlohaCoreComponentNames.SYMBOL_SEARCH_GRID,
    AlohaCoreComponentNames.TABLE_SIZE_SELECT,
    AlohaCoreComponentNames.TOGGLE_BUTTON,
    AlohaCoreComponentNames.TOGGLE_SPLIT_BUTTON,
].reduce((acc, name) => {
    acc[name] = `gtx-aloha-${name}-renderer`;
    return acc;
}, {});

export interface FormattedText {
    text: string;
    formats: string[];
}

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

export interface PageImportData
    extends Omit<PageCreateRequest, 'nodeId' | 'folderId' | 'templateId' | 'language'>,
    Partial<Pick<Page, 'tags' | 'language'>>, ImportData {

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

export interface ScheduleTaskImportData extends ScheduleTaskCreateRequest, ImportData {
    [IMPORT_TYPE]: typeof IMPORT_TYPE_TASK;
}

export interface ScheduleImportData extends Omit<ScheduleCreateReqeust, 'taskId'>, ImportData {
    [IMPORT_TYPE]: typeof IMPORT_TYPE_SCHEDULE;

    /** The task import-id or internal command to be executed */
    task: string;
}
