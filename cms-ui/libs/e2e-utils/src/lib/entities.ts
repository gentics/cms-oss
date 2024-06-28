/*
 * This file defines all neccessary test entities directly.
 * Importing them via JSON would work as well, but here we have proper type
 * checks to all entities without having to jump through hoops.
 */
import {
    FolderCreateRequest,
    NodeCreateRequest,
    NodeFeature,
    NodePageLanguageCode,
    NodeUrlMode,
    PageCreateRequest,
} from '@gentics/cms-models';
import { TestSize } from './common';

/** Type to determine how to import/delete the entity */
export const IMPORT_TYPE = Symbol('gtx-e2e-import-type');
/** ID which can be referenced in other entities to determine relationships. */
export const IMPORT_ID = Symbol('gtx-e2e-import-id');

const BASIC_TEMPLATE_ID = '57a5.5db4acfa-3224-11ef-862c-0242ac110002';

export interface ImportData {
    [IMPORT_TYPE]: 'node' | 'folder' | 'page' | 'file' | 'image';
    [IMPORT_ID]: string;
}

export interface NodeImportData extends NodeCreateRequest, ImportData {
    [IMPORT_TYPE]: 'node';
    /** Language codes which will be assigned */
    languages: string[];
    /** Features which will be assigned */
    features: NodeFeature[];
    templates: string[];
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

export const minimalNode: NodeImportData = {
    [IMPORT_TYPE]: 'node',
    [IMPORT_ID]: 'minimalNode',

    node: {
        name : 'Minimal',
        publishDir : '',
        binaryPublishDir : '',
        pubDirSegment : true,
        https : false,
        publishImageVariants : false,
        host : 'minimal.localhost',
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
    description: 'test',

    languages : [ 'de', 'en' ],
    features: [],
    templates: [
        BASIC_TEMPLATE_ID,
    ],
};

export const folderA: FolderImportData = {
    [IMPORT_TYPE]: 'folder',
    [IMPORT_ID]: 'folderA',

    nodeId: minimalNode[IMPORT_ID],
    motherId: minimalNode[IMPORT_ID],

    name: 'Folder A',
    description: 'Description of Folder A',
    publishDir: 'folder-a',
};

export const pageOne: PageImportData = {
    [IMPORT_TYPE]: 'page',
    [IMPORT_ID]: 'pageOne',

    folderId: minimalNode[IMPORT_ID],
    nodeId: minimalNode[IMPORT_ID],
    templateId: BASIC_TEMPLATE_ID,

    pageName: 'Page #1',
    fileName: 'page-one',
    description: 'Example Page number one',
    language: 'en',
    priority: 1,
}

export const PACKAGE_IMPORTS: Record<TestSize, string[]> = {
    [TestSize.MINIMAL]: ['minimal'],
    [TestSize.FULL]: ['minimal', 'full'],
}

export const PACKAGE_MAP: Record<TestSize, ImportData[]> = {
    [TestSize.MINIMAL]: [
        minimalNode,
        folderA,
        pageOne,
    ],
    [TestSize.FULL]: [],
};
