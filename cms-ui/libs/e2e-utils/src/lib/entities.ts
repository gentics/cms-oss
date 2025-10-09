/* eslint-disable @typescript-eslint/naming-convention */
/*
 * This file defines all neccessary test entities directly.
 * Importing them via JSON would work as well, but here we have proper type
 * checks to all entities without having to jump through hoops.
 */
import { AccessControlledType, CmsFormType, GcmsPermission, NodePageLanguageCode, NodeUrlMode, ScheduleType, TagPropertyType } from '@gentics/cms-models';
import {
    BASIC_TEMPLATE_ID,
    FileImportData,
    FolderImportData,
    FormImportData,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_PAGE_TRANSLATION,
    IMPORT_TYPE_SCHEDULE,
    ImportData,
    ImportPermissions,
    ImportSinglePermission,
    ITEM_TYPE_FILE,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_FORM,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    NodeImportData,
    PageImportData,
    PageTranslationImportData,
    ScheduleImportData,
    TASK_LINK_CHECKER,
    TASK_PUBLISH,
    TestSize,
} from './common';

/*
 * REQUIRED SETUP
 * ---------------------------------------------------------------- */

/** This node exists only, so all devtool-packages are linked to this node, to make the cleanup of our actual test nodes possible. */
export const emptyNode: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'emptyNode',

    node: {
        name : 'empty node',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        publishImageVariants : false,
        host : 'empty.localhost',
        publishFs : false,
        publishFsPages : false,
        publishFsFiles : false,
        publishContentMap : false,
        publishContentMapPages : false,
        publishContentMapFiles : false,
        publishContentMapFolders : false,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension : false,
        pageLanguageCode : NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty : '',
    },
    description: 'empty node',

    languages : [LANGUAGE_EN],
    templates: [BASIC_TEMPLATE_ID],
};

export const CLEAR_ALL_CONTENT_PERMISSIONS: ImportSinglePermission[] = [
    { type: GcmsPermission.READ, value: false },
    { type: GcmsPermission.SET_PERMISSION, value: false },

    // Folder
    { type: GcmsPermission.CREATE, value: false }, // No idea why it isn't createfolder
    { type: GcmsPermission.UPDATE_FOLDER, value: false },
    { type: GcmsPermission.DELETE_FOLDER, value: false },

    // Items
    { type: GcmsPermission.READ_ITEMS, value: false },
    { type: GcmsPermission.CREATE_ITEMS, value: false },
    { type: GcmsPermission.UPDATE_ITEMS, value: false },
    { type: GcmsPermission.DELETE_ITEMS, value: false },
    { type: GcmsPermission.IMPORT_ITEMS, value: false },

    // Pages
    { type: GcmsPermission.PUBLISH_PAGES, value: false },

    // Templates
    { type: GcmsPermission.READ_TEMPLATES, value: false },
    { type: GcmsPermission.CREATE_TEMPLATES, value: false },
    { type: GcmsPermission.UPDATE_TEMPLATES, value: false },
    { type: GcmsPermission.DELETE_TEMPLATES, value: false },
    { type: GcmsPermission.LINK_TEMPLATES, value: false },

    // Misc
    { type: GcmsPermission.UPDATE_CONSTRUCTS, value: false },
    { type: GcmsPermission.WASTE_BIN, value: false },
];

export const GROUP_ROOT: GroupImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
    [IMPORT_ID]: 'group_root',

    parent: null,

    name: 'group_test_root',
    description: 'Integration Tests Root Group',

    // Remove all permissions from this group
    permissions: [
        {
            type: AccessControlledType.SETTING,
            perms: [
                { type: GcmsPermission.READ, value: false },
                { type: GcmsPermission.SET_PERMISSION, value: false },
            ],
        },
        {
            type: AccessControlledType.INBOX,
            perms: [
                { type: GcmsPermission.READ, value: false },
                { type: GcmsPermission.SET_PERMISSION, value: false },
                { type: GcmsPermission.INSTANT_MESSAGES, value: false },
            ],
        },
        {
            type: AccessControlledType.PUBLISH_QUEUE,
            perms: [
                { type: GcmsPermission.READ, value: false },
                { type: GcmsPermission.SET_PERMISSION, value: false },
            ],
        },
        {
            type: AccessControlledType.ADMIN,
            subObjects: true,
            perms: [
                { type: GcmsPermission.READ, value: false },
                { type: GcmsPermission.SET_PERMISSION, value: false },
            ],
        },
        {
            type: AccessControlledType.USER_ADMIN,
            perms: [
                { type: GcmsPermission.CREATE_USER, value: false },
                { type: GcmsPermission.UPDATE_USER, value: false },
                { type: GcmsPermission.DELETE_USER, value: false },
            ],
        },
        {
            type: AccessControlledType.GROUP_ADMIN,
            perms: [
                { type: GcmsPermission.CREATE_GROUP, value: false },
                { type: GcmsPermission.UPDATE_GROUP, value: false },
                { type: GcmsPermission.DELETE_GROUP, value: false },
                { type: GcmsPermission.USER_ASSIGNMENT, value: false },
                { type: GcmsPermission.UPDATE_GROUP_USER, value: false },
                { type: GcmsPermission.SET_USER_PERMISSIONS, value: false },
            ],
        },
        {
            type: AccessControlledType.ROLE,
            perms: [
                { type: GcmsPermission.ASSIGN_ROLES, value: false },
            ],
        },
        {
            type: AccessControlledType.LICENSING,
            perms: [
                { type: GcmsPermission.UPDATE, value: false },
            ],
        },
        {
            type: AccessControlledType.CONTENT_ADMIN,
            perms: [
                { type: GcmsPermission.SYSTEM_INFORMATION, value: false },
            ],
        },
        ...[10002, 10006, 10007, 10011, 10008].map(id => ({
            type: AccessControlledType.OBJECT_PROPERTY_TYPE,
            instanceId: `${id}`,
            subObjects: true,
            perms: [
                { type: GcmsPermission.UPDATE, value: false },
            ],
        } as ImportPermissions)),
        {
            type: AccessControlledType.CONSTRUCT_ADMIN,
            perms: [
                { type: GcmsPermission.UPDATE, value: false },
            ],
        },
        {
            type: AccessControlledType.MAINTENANCE,
            perms: [
                { type: GcmsPermission.UPDATE, value: false },
            ],
        },
        {
            type: AccessControlledType.CONTENT_REPOSITORY_ADMIN,
            perms: [
                { type: GcmsPermission.CREATE, value: false },
            ],
        },
        {
            type: AccessControlledType.CR_FRAGMENT_ADMIN,
            perms: [
                { type: GcmsPermission.CREATE, value: false },
            ],
        },
        {
            type: AccessControlledType.ERROR_LOG,
            perms: [
                { type: GcmsPermission.DELETE_ERROR_LOG, value: false },
            ],
        },
        {
            type: AccessControlledType.SCHEDULER,
            perms: [
                { type: GcmsPermission.SUSPEND_SCHEDULER, value: false },
                { type: GcmsPermission.READ_TASKS, value: false },
                { type: GcmsPermission.UPDATE_TASKS, value: false },
                { type: GcmsPermission.READ_SCHEDULES, value: false },
                { type: GcmsPermission.UPDATE_SCHEDULES, value: false },
            ],
        },
        {
            type: AccessControlledType.CONTENT,
            subObjects: true,
            perms: CLEAR_ALL_CONTENT_PERMISSIONS,
        },
    ],
};

export const SCHEDULE_PUBLISHER: ScheduleImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_SCHEDULE,
    [IMPORT_ID]: 'schedulePublish',

    active: true,
    parallel: false,
    name: 'Run Publish Process',
    taskId: TASK_PUBLISH,
    scheduleData: {
        type: ScheduleType.MANUAL,
    },
};

export const SCHEDULE_LINK_CHECKER: ScheduleImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_SCHEDULE,
    [IMPORT_ID]: 'scheduleLinkChecker',

    active: true,
    parallel: false,
    name: 'Run Link Checker',
    taskId: TASK_LINK_CHECKER,
    scheduleData: {
        type: ScheduleType.MANUAL,
    },
};

/*
 * MINIMAL SETUP
 * ---------------------------------------------------------------- */

export const NODE_MINIMAL: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'minimalNode',

    node: {
        name : 'Minimal',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        publishImageVariants : false,
        host : 'minimal.localhost',
        publishFs : false,
        publishFsPages : false,
        publishFsFiles : false,
        publishContentMap : true,
        publishContentMapPages : true,
        publishContentMapFiles : true,
        publishContentMapFolders : true,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension : false,
        pageLanguageCode : NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty : '',
    },
    description: 'minimal test',

    languages : [LANGUAGE_DE, LANGUAGE_EN],
    templates: [
        BASIC_TEMPLATE_ID,
    ],
};

function createFolder(node: NodeImportData, parent: NodeImportData | FolderImportData, id: string): FolderImportData {
    return {
        [IMPORT_TYPE]: ITEM_TYPE_FOLDER,
        [IMPORT_ID]: `folder${id}`,

        nodeId: node[IMPORT_ID],
        motherId: parent[IMPORT_ID],

        name: `Folder ${id}`,
        description: `Description of Folder ${id}`,
        publishDir: `folder-${id.toLocaleLowerCase()}`,
    };
}

function createRootFolder(node: NodeImportData, id: string): FolderImportData {
    return createFolder(node, node, id);
}

function createPage(
    node: NodeImportData,
    folder: NodeImportData | FolderImportData,
    templateId: string,
    id: string,
    language: string = LANGUAGE_EN,
): PageImportData {
    return {
        [IMPORT_TYPE]: ITEM_TYPE_PAGE,
        [IMPORT_ID]: `page${id}`,

        folderId: folder[IMPORT_ID],
        nodeId: node[IMPORT_ID],
        templateId: templateId,

        pageName: `Page Nr. ${id}`,
        fileName: `page-${id.toLowerCase()}`,
        description: `Example Page number ${id}`,
        language: language,
        priority: 1,
    };
}

function createPageTranslation(
    page: PageImportData,
    id: string,
    language: string,
    data: Omit<PageTranslationImportData, typeof IMPORT_ID | typeof IMPORT_TYPE | 'pageId' | 'language'>,
): PageTranslationImportData {
    return {
        ...data,
        [IMPORT_TYPE]: IMPORT_TYPE_PAGE_TRANSLATION,
        [IMPORT_ID]: id,
        pageId: page[IMPORT_ID],
        language: language,
    };
}

export const FOLDER_A = createRootFolder(NODE_MINIMAL, 'A');
export const FOLDER_B = createRootFolder(NODE_MINIMAL, 'B');

export const PAGE_ONE: PageImportData = {
    ...createPage(NODE_MINIMAL, NODE_MINIMAL, BASIC_TEMPLATE_ID, 'One'),
    tags: {
        content: {
            id: null,
            constructId: 7,
            name: 'content',
            active: true,
            type: 'CONTENTTAG',
            properties: {
                text: {
                    type: TagPropertyType.RICHTEXT,
                    stringValue: `
Lorem ipsum odor amet, consectetuer adipiscing elit. Tortor consectetur cras aliquam ipsum commodo gravida.
Duis id ut elit suscipit, litora feugiat sollicitudin gravida. Ex at venenatis congue lacinia at orci eu primis.
Faucibus netus lobortis porta vulputate lorem molestie porttitor magnis feugiat. Facilisi maximus sollicitudin diam, neque nam per.
Nisl interdum convallis arcu blandit orci integer parturient. Aliquet eleifend risus ullamcorper consectetur elementum posuere nisl.
<br>
<br>
Neque ullamcorper euismod magnis nec; cubilia magnis vulputate molestie. Vitae ligula scelerisque porttitor quam orci penatibus tortor taciti.
Aliquam porttitor in volutpat ante semper ad. Lacinia blandit duis egestas metus aliquet mus suscipit potenti.
Finibus maximus habitant proin facilisi ligula vulputate. Netus sed accumsan parturient sit torquent finibus tempor adipiscing.
Feugiat ac integer viverra fermentum auctor ipsum tristique rutrum.`,
                },
            },
        },
    },
};

export const PAGE_ONE_DE = createPageTranslation(PAGE_ONE, 'Eins', LANGUAGE_DE, {
    pageName: 'Seite Eins',
    tags: {
        content: {
            id: null,
            constructId: 7,
            name: 'content',
            active: true,
            type: 'CONTENTTAG',
            properties: {
                text: {
                    type: TagPropertyType.RICHTEXT,
                    stringValue: `
Integer iaculis consectetur nulla id pulvinar. Vestibulum dictum congue ligula, eget commodo eros vestibulum rutrum.
Sed finibus purus at tortor rutrum maximus. Praesent urna arcu, laoreet ac egestas lacinia, sodales vel turpis.
Donec orci mi, ultricies sit amet nisl vitae, efficitur malesuada nunc. Nunc et felis iaculis, fermentum quam in, vulputate risus.
<br>
<br>
Donec dictum enim eu velit sodales, non eleifend est venenatis. C
ras condimentum, elit in facilisis sodales, risus urna consectetur justo, eget facilisis diam purus eu elit. `,
                },
            },
        },
    },
});

export const FILE_ONE: FileImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FILE,
    [IMPORT_ID]: 'fileOne',

    nodeId: NODE_MINIMAL[IMPORT_ID],
    folderId: NODE_MINIMAL[IMPORT_ID],

    name: 'File #1',
    description: 'First file',
};

export const IMAGE_ONE: ImageImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_IMAGE,
    [IMPORT_ID]: 'imageOne',

    nodeId: NODE_MINIMAL[IMPORT_ID],
    folderId: NODE_MINIMAL[IMPORT_ID],

    name: 'Image #1',
    description: 'First image',
};

/** Has to be imported manually/per test, as forms are a EE feature */
export const FORM_ONE: FormImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FORM,
    [IMPORT_ID]: 'formOne',

    nodeId: NODE_MINIMAL[IMPORT_ID],
    folderId: NODE_MINIMAL[IMPORT_ID],

    languages: [LANGUAGE_EN],

    name: 'Form One',
    description: 'Test Form one',
    data: {
        type: CmsFormType.GENERIC,
    },
};

/*
 * FULL SETUP
 * ---------------------------------------------------------------- */

export const NODE_FULL: NodeImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_NODE,
    [IMPORT_ID]: 'fullNode',

    node: {
        name : 'Full',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        publishImageVariants : false,
        host : 'full.localhost',
        publishFs : false,
        publishFsPages : false,
        publishFsFiles : false,
        publishContentMap : true,
        publishContentMapPages : true,
        publishContentMapFiles : true,
        publishContentMapFolders : true,
        urlRenderWayPages: NodeUrlMode.AUTOMATIC,
        urlRenderWayFiles: NodeUrlMode.AUTOMATIC,
        omitPageExtension : false,
        pageLanguageCode : NodePageLanguageCode.FILENAME,
        meshPreviewUrlProperty : '',
    },
    description: 'full test',

    languages : [LANGUAGE_DE, LANGUAGE_EN],
    templates: [
        BASIC_TEMPLATE_ID,
    ],
};

export const FOLDER_C = createRootFolder(NODE_FULL, 'C');
export const FOLDER_D = createRootFolder(NODE_FULL, 'D');
export const FOLDER_E = createRootFolder(NODE_FULL, 'E');
export const FOLDER_F = createRootFolder(NODE_FULL, 'F');
export const FOLDER_G = createRootFolder(NODE_FULL, 'G');
export const FOLDER_H = createRootFolder(NODE_FULL, 'H');
export const FOLDER_I = createRootFolder(NODE_FULL, 'I');
export const FOLDER_J = createRootFolder(NODE_FULL, 'J');
export const FOLDER_K = createRootFolder(NODE_FULL, 'K');
export const FOLDER_L = createRootFolder(NODE_FULL, 'L');
export const FOLDER_M = createRootFolder(NODE_FULL, 'M');
export const FOLDER_N = createRootFolder(NODE_FULL, 'N');
export const FOLDER_O = createRootFolder(NODE_FULL, 'O');
export const FOLDER_P = createRootFolder(NODE_FULL, 'P');
export const FOLDER_Q = createRootFolder(NODE_FULL, 'Q');
export const FOLDER_R = createRootFolder(NODE_FULL, 'R');
export const FOLDER_S = createRootFolder(NODE_FULL, 'S');
export const FOLDER_T = createRootFolder(NODE_FULL, 'T');
export const FOLDER_U = createRootFolder(NODE_FULL, 'U');
export const FOLDER_V = createRootFolder(NODE_FULL, 'V');
export const FOLDER_W = createRootFolder(NODE_FULL, 'W');
export const FOLDER_X = createRootFolder(NODE_FULL, 'X');
export const FOLDER_Y = createRootFolder(NODE_FULL, 'Y');
export const FOLDER_Z = createRootFolder(NODE_FULL, 'Z');

export const FOLDER_C_A = createFolder(NODE_FULL, FOLDER_C, 'C-A');
export const FOLDER_C_B = createFolder(NODE_FULL, FOLDER_C, 'C-B');
export const FOLDER_C_C = createFolder(NODE_FULL, FOLDER_C, 'C-C');

export const FOLDER_C_B_A = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-A');
export const FOLDER_C_B_B = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-B');
export const FOLDER_C_B_C = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-C');
export const FOLDER_C_B_D = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-D');
export const FOLDER_C_B_E = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-E');
export const FOLDER_C_B_F = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-F');
export const FOLDER_C_B_G = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-G');
export const FOLDER_C_B_H = createFolder(NODE_FULL, FOLDER_C_A, 'C-B-H');

export const FOLDER_C_B_E_A = createFolder(NODE_FULL, FOLDER_C_B_E, 'C-B-E-A');

export const FOLDER_F_A = createFolder(NODE_FULL, FOLDER_F, 'F-A');
export const FOLDER_F_B = createFolder(NODE_FULL, FOLDER_F, 'F-B');
export const FOLDER_F_C = createFolder(NODE_FULL, FOLDER_F, 'F-C');
export const FOLDER_F_D = createFolder(NODE_FULL, FOLDER_F, 'F-D');
export const FOLDER_F_E = createFolder(NODE_FULL, FOLDER_F, 'F-E');
export const FOLDER_F_F = createFolder(NODE_FULL, FOLDER_F, 'F-F');
export const FOLDER_F_G = createFolder(NODE_FULL, FOLDER_F, 'F-G');
export const FOLDER_F_H = createFolder(NODE_FULL, FOLDER_F, 'F-H');

export const PAGE_TWO = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Two');
export const PAGE_THREE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Three');
export const PAGE_FOUR = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Four');
export const PAGE_FIVE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Five');
export const PAGE_SIX = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Six');
export const PAGE_SEVEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Seven');
export const PAGE_EIGHT = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Eight');
export const PAGE_NINE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Nine');
export const PAGE_TEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Ten');
export const PAGE_ELEVEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Eleven');
export const PAGE_TWELVE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twelve');
export const PAGE_THIRTEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Thirteen');
export const PAGE_FOURTEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Fourteen');
export const PAGE_FIVETEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Fiveteen');
export const PAGE_SIXTEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Sixteen');
export const PAGE_SEVENTEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Seventeen');
export const PAGE_EIGHTEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Eighteen');
export const PAGE_NINETEEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Nineteen');
export const PAGE_TWENTY = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twenty');
export const PAGE_TWNETYONE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentyone');
export const PAGE_TWENTYTWO = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentytwo');
export const PAGE_TWENTYTHREE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentythree');
export const PAGE_TWENTYFOUR = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentyfour');
export const PAGE_TWENTYFIVE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentyfive');
export const PAGE_TWENTYSIX = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentysix');
export const PAGE_TWENTYSEVEN = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentyseven');
export const PAGE_TWENTYEIGHT = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentyeight');
export const PAGE_TWENTYNINE = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Twentynine');
export const PAGE_THIRTY = createPage(NODE_FULL, NODE_FULL, BASIC_TEMPLATE_ID, 'Thirty');

export const PAGE_C_B_ONE = createPage(NODE_FULL, FOLDER_C_B, BASIC_TEMPLATE_ID, 'One_C-B');
export const PAGE_C_B_TWO = createPage(NODE_FULL, FOLDER_C_B, BASIC_TEMPLATE_ID, 'Two_C-B');
export const PAGE_C_B_THREE = createPage(NODE_FULL, FOLDER_C_B, BASIC_TEMPLATE_ID, 'Three_C-B');
export const PAGE_C_B_FOUR = createPage(NODE_FULL, FOLDER_C_B, BASIC_TEMPLATE_ID, 'Four_C-B');
export const PAGE_C_B_FIVE = createPage(NODE_FULL, FOLDER_C_B, BASIC_TEMPLATE_ID, 'Five_C-B');

export const PAGE_C_B_E_ONE = createPage(NODE_FULL, FOLDER_C_B_E, BASIC_TEMPLATE_ID, 'One_C-B-E');
export const PAGE_C_B_E_TWO = createPage(NODE_FULL, FOLDER_C_B_E, BASIC_TEMPLATE_ID, 'Two_C-B-E');

/*
 * PACKAGES
 * ---------------------------------------------------------------- */

export const PACKAGE_IMPORTS: Record<TestSize, string[]> = {
    [TestSize.NONE]: [],
    [TestSize.MINIMAL]: ['minimal'],
    [TestSize.FULL]: ['minimal', 'full'],
}

export const PACKAGE_MAP: Record<TestSize, ImportData[]> = {
    [TestSize.NONE]: [],
    [TestSize.MINIMAL]: [
        NODE_MINIMAL,
        FOLDER_A,
        FOLDER_B,
        PAGE_ONE,
        FILE_ONE,
        IMAGE_ONE,
    ],
    [TestSize.FULL]: [
        NODE_FULL,
        FOLDER_C,
        FOLDER_D,
        FOLDER_C,
        FOLDER_D,
        FOLDER_E,
        FOLDER_F,
        FOLDER_G,
        FOLDER_H,
        FOLDER_I,
        FOLDER_J,
        FOLDER_K,
        FOLDER_L,
        FOLDER_M,
        FOLDER_N,
        FOLDER_O,
        FOLDER_P,
        FOLDER_Q,
        FOLDER_R,
        FOLDER_S,
        FOLDER_T,
        FOLDER_U,
        FOLDER_V,
        FOLDER_W,
        FOLDER_X,
        FOLDER_Y,
        FOLDER_Z,
        FOLDER_C_A,
        FOLDER_C_B,
        FOLDER_C_C,
        FOLDER_C_B_A,
        FOLDER_C_B_B,
        FOLDER_C_B_C,
        FOLDER_C_B_D,
        FOLDER_C_B_E,
        FOLDER_C_B_F,
        FOLDER_C_B_G,
        FOLDER_C_B_H,
        FOLDER_C_B_E_A,
        FOLDER_F_A,
        FOLDER_F_B,
        FOLDER_F_C,
        FOLDER_F_D,
        FOLDER_F_E,
        FOLDER_F_F,
        FOLDER_F_G,
        FOLDER_F_H,
        PAGE_TWO,
        PAGE_THREE,
        PAGE_THREE,
        PAGE_FOUR,
        PAGE_FIVE,
        PAGE_SIX,
        PAGE_SEVEN,
        PAGE_EIGHT,
        PAGE_NINE,
        PAGE_TEN,
        PAGE_ELEVEN,
        PAGE_TWELVE,
        PAGE_THIRTEEN,
        PAGE_FOURTEEN,
        PAGE_FIVETEEN,
        PAGE_SIXTEEN,
        PAGE_SEVENTEEN,
        PAGE_EIGHTEEN,
        PAGE_NINETEEN,
        PAGE_TWENTY,
        PAGE_TWNETYONE,
        PAGE_TWENTYTWO,
        PAGE_TWENTYTHREE,
        PAGE_TWENTYFOUR,
        PAGE_TWENTYFIVE,
        PAGE_TWENTYSIX,
        PAGE_TWENTYSEVEN,
        PAGE_TWENTYEIGHT,
        PAGE_TWENTYNINE,
        PAGE_THIRTY,
        PAGE_C_B_ONE,
        PAGE_C_B_TWO,
        PAGE_C_B_THREE,
        PAGE_C_B_FOUR,
        PAGE_C_B_FIVE,
        PAGE_C_B_E_ONE,
        PAGE_C_B_E_TWO,
    ],
};
