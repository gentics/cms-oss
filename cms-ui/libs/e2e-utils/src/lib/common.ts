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
    ScheduleTaskCreateRequest,
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

export const ENV_CMS_REST_PATH = 'CMS_REST_PATH';
export const ENV_CMS_ADMIN_PATH = 'CMS_ADMIN_PATH';
export const ENV_CMS_USERNAME = 'CMS_USERNAME';
export const ENV_CMS_PASSWORD = 'CMS_PASSWORD';
export const ENV_CMS_VARIANT = 'CMS_VARIANT';

export const ENV_ALOHA_PLUGIN_CITE = 'ALOHA_PLUGIN_CITE';

export type ItemType = typeof ITEM_TYPE_FOLDER | typeof ITEM_TYPE_PAGE | typeof ITEM_TYPE_FILE | typeof ITEM_TYPE_IMAGE | typeof ITEM_TYPE_FORM;
export type ImportType = ItemType | typeof IMPORT_TYPE_NODE | typeof IMPORT_TYPE_USER | typeof IMPORT_TYPE_GROUP | typeof IMPORT_TYPE_TASK;

/** Type to determine how to import/delete the entity */
export const IMPORT_TYPE = Symbol('gtx-e2e-import-type');
/** ID which can be referenced in other entities to determine relationships. */
export const IMPORT_ID = Symbol('gtx-e2e-import-id');

export const BASIC_TEMPLATE_ID = '57a5.5db4acfa-3224-11ef-862c-0242ac110002';

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
