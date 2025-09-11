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
    IMPORT_TYPE_SCHEDULE,
    IMPORT_TYPE_USER,
    ImportData,
    ITEM_TYPE_FILE,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_FORM,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    LANGUAGE_DE,
    LANGUAGE_EN,
    NodeImportData,
    PageImportData,
    ScheduleImportData,
    TASK_LINK_CHECKER,
    TASK_PUBLISH,
    TestSize,
    UserImportData,
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

export const rootGroup: GroupImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_GROUP,
    [IMPORT_ID]: 'rootGroup',

    name: 'Root-Group',
    description: 'Integration Tests Root Group',

    permissions: [
        // Remove Admin permissions for this group
        {
            type: AccessControlledType.ADMIN,
            subGroups: true,
            perms: [
                { type: GcmsPermission.READ, value: false },
            ],
        },
    ],
};

export const userAlpha: UserImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_USER,
    [IMPORT_ID]: 'userAlpha',

    group: rootGroup[IMPORT_ID],

    email: 'alpha-test-user@localhost',
    firstName: 'Test User',
    lastName: 'Alpha',
    password: 'alpha',
    login: 'alpha',
    description: 'Test User Alpha',
};

export const userBeta: UserImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_USER,
    [IMPORT_ID]: 'userBeta',

    group: rootGroup[IMPORT_ID],

    email: 'beta-test-user@localhost',
    firstName: 'Test User',
    lastName: 'beta',
    password: 'beta',
    login: 'beta',
    description: 'Test User Beta',
};

export const schedulePublisher: ScheduleImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_SCHEDULE,
    [IMPORT_ID]: 'schedulePublish',

    active: true,
    parallel: false,
    name: 'Run Publish Process',
    task: TASK_PUBLISH,
    scheduleData: {
        type: ScheduleType.MANUAL,
    },
};

export const scheduleLinkChecker: ScheduleImportData = {
    [IMPORT_TYPE]: IMPORT_TYPE_SCHEDULE,
    [IMPORT_ID]: 'scheduleLinkChecker',

    active: true,
    parallel: false,
    name: 'Run Link Checker',
    task: TASK_LINK_CHECKER,
    scheduleData: {
        type: ScheduleType.MANUAL,
    },
};

/*
 * MINIMAL SETUP
 * ---------------------------------------------------------------- */

export const minimalNode: NodeImportData = {
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

export const folderA = createRootFolder(minimalNode, 'A');
export const folderB = createRootFolder(minimalNode, 'B');

export const pageOne: PageImportData = {
    ...createPage(minimalNode, minimalNode, BASIC_TEMPLATE_ID, 'One'),
    translations: [LANGUAGE_DE],
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

export const pageOneDE: PageImportData = {
    ...createPage(minimalNode, minimalNode, BASIC_TEMPLATE_ID, 'Eins', LANGUAGE_DE),
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
};

export const fileOne: FileImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FILE,
    [IMPORT_ID]: 'fileOne',

    nodeId: minimalNode[IMPORT_ID],
    folderId: minimalNode[IMPORT_ID],

    name: 'File #1',
    description: 'First file',
};

export const imageOne: ImageImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_IMAGE,
    [IMPORT_ID]: 'imageOne',

    nodeId: minimalNode[IMPORT_ID],
    folderId: minimalNode[IMPORT_ID],

    name: 'Image #1',
    description: 'First image',
};

/** Has to be imported manually/per test, as forms are a EE feature */
export const formOne: FormImportData = {
    [IMPORT_TYPE]: ITEM_TYPE_FORM,
    [IMPORT_ID]: 'formOne',

    nodeId: minimalNode[IMPORT_ID],
    folderId: minimalNode[IMPORT_ID],

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

export const fullNode: NodeImportData = {
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

export const folderFullA = createRootFolder(fullNode, 'A');
export const folderFullB = createRootFolder(fullNode, 'B');
export const folderC = createRootFolder(fullNode, 'C');
export const folderD = createRootFolder(fullNode, 'D');
export const folderE = createRootFolder(fullNode, 'E');
export const folderF = createRootFolder(fullNode, 'F');
export const folderG = createRootFolder(fullNode, 'G');
export const folderH = createRootFolder(fullNode, 'H');
export const folderI = createRootFolder(fullNode, 'I');
export const folderJ = createRootFolder(fullNode, 'J');
export const folderK = createRootFolder(fullNode, 'K');
export const folderL = createRootFolder(fullNode, 'L');
export const folderM = createRootFolder(fullNode, 'M');
export const folderN = createRootFolder(fullNode, 'N');
export const folderO = createRootFolder(fullNode, 'O');
export const folderP = createRootFolder(fullNode, 'P');
export const folderQ = createRootFolder(fullNode, 'Q');
export const folderR = createRootFolder(fullNode, 'R');
export const folderS = createRootFolder(fullNode, 'S');
export const folderT = createRootFolder(fullNode, 'T');
export const folderU = createRootFolder(fullNode, 'U');

export const folderA_A = createFolder(fullNode, folderFullA, 'A-A');
export const folderA_B = createFolder(fullNode, folderFullA, 'A-B');
export const folderA_C = createFolder(fullNode, folderFullA, 'A-C');

export const folderA_B_A = createFolder(fullNode, folderA_A, 'A-B-A');
export const folderA_B_B = createFolder(fullNode, folderA_A, 'A-B-B');
export const folderA_B_C = createFolder(fullNode, folderA_A, 'A-B-C');
export const folderA_B_D = createFolder(fullNode, folderA_A, 'A-B-D');
export const folderA_B_E = createFolder(fullNode, folderA_A, 'A-B-E');
export const folderA_B_F = createFolder(fullNode, folderA_A, 'A-B-F');
export const folderA_B_G = createFolder(fullNode, folderA_A, 'A-B-G');
export const folderA_B_H = createFolder(fullNode, folderA_A, 'A-B-H');

export const folderA_B_E_A = createFolder(fullNode, folderA_B_E, 'A-B-E-A');

export const folderF_A = createFolder(fullNode, folderF, 'F-A');
export const folderF_B = createFolder(fullNode, folderF, 'F-B');
export const folderF_C = createFolder(fullNode, folderF, 'F-C');
export const folderF_D = createFolder(fullNode, folderF, 'F-D');
export const folderF_E = createFolder(fullNode, folderF, 'F-E');
export const folderF_F = createFolder(fullNode, folderF, 'F-F');
export const folderF_G = createFolder(fullNode, folderF, 'F-G');
export const folderF_H = createFolder(fullNode, folderF, 'F-H');

export const pageFullOne = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'One');
export const pageTwo = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Two');
export const pageThree = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Three');
export const pageFour = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Four');
export const pageFive = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Five');
export const pageSix = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Six');
export const pageSeven = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Seven');
export const pageEight = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Eight');
export const pageNine = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Nine');
export const pageTen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Ten');
export const pageEleven = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Eleven');
export const pageTwelve = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twelve');
export const pageThirteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Thirteen');
export const pageFourteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Fourteen');
export const pageFiveteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Fiveteen');
export const pageSixteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Sixteen');
export const pageSeventeen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Seventeen');
export const pageEighteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Eighteen');
export const pageNineteen = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Nineteen');
export const pageTwenty = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twenty');
export const pageTwentyone = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentyone');
export const pageTwentytwo = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentytwo');
export const pageTwentythree = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentythree');
export const pageTwentyfour = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentyfour');
export const pageTwentyfive = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentyfive');
export const pageTwentysix = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentysix');
export const pageTwentyseven = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentyseven');
export const pageTwentyeight = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentyeight');
export const pageTwentynine = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Twentynine');
export const pageThirty = createPage(fullNode, fullNode, BASIC_TEMPLATE_ID, 'Thirty');

export const pageA_B_One = createPage(fullNode, folderA_B, BASIC_TEMPLATE_ID, 'One');
export const pageA_B_Two = createPage(fullNode, folderA_B, BASIC_TEMPLATE_ID, 'Two');
export const pageA_B_Three = createPage(fullNode, folderA_B, BASIC_TEMPLATE_ID, 'Three');
export const pageA_B_Four = createPage(fullNode, folderA_B, BASIC_TEMPLATE_ID, 'Four');
export const pageA_B_Five = createPage(fullNode, folderA_B, BASIC_TEMPLATE_ID, 'Five');

export const pageA_B_E_One = createPage(fullNode, folderA_B_E, BASIC_TEMPLATE_ID, 'One');
export const pageA_B_E_Two = createPage(fullNode, folderA_B_E, BASIC_TEMPLATE_ID, 'Two');

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
        minimalNode,
        folderA,
        folderB,
        pageOne,
        pageOneDE,
        fileOne,
        imageOne,
    ],
    [TestSize.FULL]: [
        fullNode,
        folderFullA,
        folderFullB,
        folderC,
        folderD,
        folderE,
        folderF,
        folderG,
        folderH,
        folderI,
        folderJ,
        folderK,
        folderL,
        folderM,
        folderN,
        folderO,
        folderP,
        folderQ,
        folderR,
        folderS,
        folderT,
        folderU,
        folderA_A,
        folderA_B,
        folderA_C,
        folderA_B_A,
        folderA_B_B,
        folderA_B_C,
        folderA_B_D,
        folderA_B_E,
        folderA_B_F,
        folderA_B_G,
        folderA_B_H,
        folderA_B_E_A,
        folderF_A,
        folderF_B,
        folderF_C,
        folderF_D,
        folderF_E,
        folderF_F,
        folderF_G,
        folderF_H,
        pageFullOne,
        pageTwo,
        pageThree,
        pageFour,
        pageFive,
        pageSix,
        pageSeven,
        pageEight,
        pageNine,
        pageTen,
        pageEleven,
        pageTwelve,
        pageThirteen,
        pageFourteen,
        pageFiveteen,
        pageSixteen,
        pageSeventeen,
        pageEighteen,
        pageNineteen,
        pageTwenty,
        pageTwentyone,
        pageTwentytwo,
        pageTwentythree,
        pageTwentyfour,
        pageTwentyfive,
        pageTwentysix,
        pageTwentyseven,
        pageTwentyeight,
        pageTwentynine,
        pageThirty,
        pageA_B_One,
        pageA_B_Two,
        pageA_B_Three,
        pageA_B_Four,
        pageA_B_Five,
        pageA_B_E_One,
        pageA_B_E_Two,
    ],
};
