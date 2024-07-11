/* eslint-disable id-blacklist */
/* eslint-disable @typescript-eslint/naming-convention */
import { normalize } from 'normalizr';
import {
    CmsFormType,
    File as FileModel,
    Folder,
    Form,
    NodePageLanguageCode,
    Image,
    IndexById,
    Language,
    Node,
    Normalized,
    ObjectTag,
    Page,
    Raw,
    TagPropertyType,
    TagType,
    Template,
    User,
} from '../models';
import { EditableObjectTag } from '../models/editable-tag';
import { GcmsNormalizationSchemas } from '../models/gcms-normalizer/schemas';

const schemas = new GcmsNormalizationSchemas();

/** Configuration for a mock node. */
export type MockNodeConfig = Partial<Pick<Node, 'inheritedFromId' | 'masterNodeId' | 'folderId'>> & {
    id: number;
    userId?: number;
};

export function getExampleLanguageData(): IndexById<Language> {
    return {
        1: {
            id: 1,
            code: 'en',
            name: 'English',
        },
        2: {
            id: 2,
            code: 'de',
            name: 'Deutsch (German)',
        },
        5: {
            id: 5,
            code: 'ar',
            name: 'العربية (Arabic)',
        },
        17: {
            id: 17,
            code: 'ja',
            name: '日本語 (Japanese)',
        },
    };
}

/** Returns actual page data from the ContentNode API for "GCN5 Demo" */
export function getExamplePageData(
    { id, userId, idVariant1 }: { id: number, userId?: number, idVariant1?: number } = { id: 95, userId: 3, idVariant1: 48 },
): Page<Raw> {
    userId = userId || 3;
    idVariant1 = idVariant1 || 48;
    return {
        locked: false,
        publisher: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        } as any,
        // status: 3,
        modified: false,
        planned: false,
        queued: false,
        master: true,
        tags: {
            content : {
                id : 137,
                name : 'content',
                constructId : 65,
                active : true,
                properties : {
                    text : {
                        type : TagPropertyType.RICHTEXT,
                        id : 685,
                        partId : 349,
                        stringValue : '<br><node blogposts1><br><br><br>',
                    },
                },
                type : 'CONTENTTAG',
            },
        },
        languageVariants: {
            2: {
                locked: false,
                publisher: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                // status: 3,
                modified: false,
                planned: false,
                queued: false,
                master: true,
                disinherited: false,
                excluded: false,
                disinheritDefault: false,
                channelSetId: 50,
                templateId: 1,
                online: false,
                folderId: 21,
                contentId: id,
                lockedSince: -1,
                masterNodeId: 1,
                inheritedFrom: 'GCN5 Demo',
                inheritedFromId: 1,
                masterNode: 'GCN5 Demo',
                contentSetId: 1630,
                pdate: 1334663423,
                languageName: 'English',
                timeManagement: {
                    at: 0,
                    offlineAt: 0,
                },
                liveUrl: '',
                contentGroupId: 2,
                // pageStatus: 'offline',
                description: '',
                language: 'en',
                inherited: false,
                priority: 1,
                path: '/GCN5 Demo/News/',
                publishPath: '/GCN5 Demo/News/',
                fileName: 'Braintribe-Mashup-Demo.en.html',
                readOnly: false,
                editor: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                creator: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                globalId: 'A547.76346',
                deleted: {
                    at: 1485272628,
                    by: {
                        email: 'nowhere@gentics.com',
                        firstName: 'Node',
                        lastName: 'Admin',
                        id: userId,
                    },
                },
                cdate: 1328174302,
                edate: 1328174336,
                folderDeleted: {
                    at: 1485272628,
                    by: {
                        email: 'nowhere@gentics.com',
                        firstName: 'Node',
                        lastName: 'Admin',
                        id: userId,
                    },
                },
                name: 'Braintribe Mashup Demo',
                id: id,
                type: 'page',
            },
            1: {
                locked: false,
                publisher: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                // status: 3,
                modified: false,
                planned: false,
                queued: false,
                master: true,
                disinherited: false,
                excluded: false,
                disinheritDefault: false,
                channelSetId: 50,
                templateId: 1,
                online: false,
                folderId: 21,
                contentId: idVariant1,
                lockedSince: -1,
                masterNodeId: 1,
                inheritedFrom: 'GCN5 Demo',
                inheritedFromId: 1,
                masterNode: 'GCN5 Demo',
                contentSetId: 1630,
                pdate: 1334663423,
                languageName: 'Deutsch (German)',
                timeManagement: {
                    at: 0,
                    offlineAt: 0,
                },
                liveUrl: '',
                contentGroupId: 2,
                // pageStatus: 'offline',
                description: '',
                language: 'de',
                inherited: false,
                priority: 1,
                path: '/GCN5 Demo/News/',
                publishPath: '/GCN5 Demo/News/',
                fileName: 'Braintribe-Mashup-Demo.de.html',
                readOnly: false,
                editor: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                creator: {
                    email: 'nowhere@gentics.com',
                    firstName: 'Node',
                    lastName: 'Admin',
                    id: userId,
                },
                globalId: 'A547.76349',
                deleted: {
                    at: 1485272628,
                    by: {
                        email: 'nowhere@gentics.com',
                        firstName: 'Node',
                        lastName: 'Admin',
                        id: userId,
                    },
                },
                cdate: 1328174302,
                edate: 1328174336,
                folderDeleted: {
                    at: 1485272628,
                    by: {
                        email: 'nowhere@gentics.com',
                        firstName: 'Node',
                        lastName: 'Admin',
                        id: userId,
                    },
                },
                name: 'Braintribe Mashup Demo',
                id: idVariant1,
                type: 'page',
            },
        },
        disinherited: false,
        excluded: false,
        disinheritDefault: false,
        channelSetId: 50,
        templateId: 1,
        online: false,
        folderId: 21,
        contentId: id,
        lockedSince: -1,
        masterNodeId: 1,
        inheritedFrom: 'GCN5 Demo',
        inheritedFromId: 1,
        masterNode: 'GCN5 Demo',
        contentSetId: 1630,
        pdate: 1334663423,
        languageName: 'English',
        timeManagement: {
            at: 0,
            offlineAt: 0,
        },
        liveUrl: '',
        contentGroupId: 2,
        // pageStatus: 'offline',
        description: '',
        language: 'en',
        inherited: false,
        priority: 1,
        path: '/GCN5 Demo/News/',
        publishPath: '/Content.Node/Braintribe-Mashup-Demo.en.html',
        fileName: 'Braintribe-Mashup-Demo.en.html',
        readOnly: false,
        editor: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        creator: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        globalId: 'A547.76346',
        deleted: {
            at: 1485272628,
            by: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                lastName: 'Admin',
                id: userId,
            },
        },
        cdate: 1328174302,
        edate: 1328174336,
        customCdate: 0,
        customEdate: 0,
        folderDeleted: {
            at: 1485272628,
            by: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                lastName: 'Admin',
                id: userId,
            },
        },
        name: 'Braintribe Mashup Demo',
        id: id,
        type: 'page',
    };
}

export function getExamplePageDataNormalized(
    { id, userId, idVariant1 }: { id: number, userId?: number, idVariant1?: number } = { id: 95, userId: 3, idVariant1: 48 },
): Page<Normalized> {
    const rawPage = getExamplePageData({ id, userId, idVariant1 });
    const normalized = normalize(rawPage, schemas.page);
    return normalized.entities.page[normalized.result] as Page<Normalized>;
}


/** Returns actual form data from the ContentNode API for "GCN5 Demo" */
export function getExampleFormData(
    { id, userId }: { id: number, userId?: number} = { id: 95, userId: 3 },
): Form<Raw> {
    userId = userId || 3;
    return {
        id: id,
        globalId: '8E86.576061db-b701-11eb-93a2-0242ac130004',
        name: 'Form',
        creator: {
            id: userId,
            firstName: 'Node',
            lastName: 'Admin',
            email: 'nowhere@gentics.com',
        },
        cdate: 1621250195,
        editor: {
            id: userId,
            firstName: 'Node',
            lastName: 'Admin',
            email: 'nowhere@gentics.com',
        },
        edate: 1621331754,
        type: 'form',
        deleted: {
            at: 0,
            by: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                lastName: 'Admin',
                id: userId,
            },
        },
        description: 'Description',
        folderId: 1,
        languages: [
            'en',
            'de',
        ],
        data: {
            elements: [
                {
                    globalId: '7f188c5f-7ef4-481d-be7c-b7e19a5d54e9',
                    name: 'formpage_7f188c5f_7ef4_481d_be7c_b7e19a5d54e9',
                    type: 'formpage',
                    active: true,
                    elements: [
                        {
                            globalId: '5d597213-30c8-4886-907d-147d7a2d49c9',
                            name: 'sectionheadline_5d597213_30c8_4886_907d_147d7a2d49c9',
                            type: 'sectionheadline',
                            active: false,
                            elements: [],
                            text_i18n: {
                                de: 'Your Name!',
                            },
                        },
                        {
                            globalId: 'cccc3d60-e5c3-4dec-985a-dfe63c72b69e',
                            name: 'input_cccc3d60_e5c3_4dec_985a_dfe63c72b69e',
                            type: 'input',
                            active: true,
                            elements: [],
                            label_i18n: {
                                de: 'Name',
                            },
                            mandatory_i18n: {
                                de: true,
                            },
                            placeholder_i18n: {
                                de: 'Name',
                            },
                            info_i18n: null,
                            tooltip_i18n: null,
                            validation_i18n: null,
                            value_i18n: null,
                            minVal_i18n: null,
                            maxVal_i18n: null,
                            maxCharCount_i18n: null,
                        },
                    ],
                    description_i18n: {
                        de: 'Main Page',
                    },
                    info_i18n: null,
                },
                {
                    globalId: 'f81e4c10-5433-4e61-ba1b-045e946071b1',
                    name: 'buttons_f81e4c10_5433_4e61_ba1b_045e946071b1',
                    type: 'buttons',
                    active: true,
                    elements: [],
                    submitlabel_i18n: null,
                    showreset_i18n: null,
                    resetlabel_i18n: null,
                },
            ],
            email: 'test@test.at',
            successurl: 'https://gentics.com/',
            mailsubject_i18n: {
                de: 'Danke!',
                en: 'Thank You!',
            },
            mailtemp_i18n: {
                de: '',
            },
            templateContext: '',
            type: CmsFormType.GENERIC,
        },
        pdate: 0,
        online: false,
        modified: false,
        planned: false,
        locked: false,
        lockedSince: 1621331754,
        lockedBy: {
            id: userId,
            firstName: 'Node',
            lastName: 'Admin',
            email: 'nowhere@gentics.com',
        },
        version: {
            number: '0.9',
            timestamp: 1621331754,
            editor: {
                id: userId,
                firstName: 'Node',
                lastName: 'Admin',
                email: 'nowhere@gentics.com',
            },
        },
        timeManagement: {
            at: 0,
            offlineAt: 0,
        },
        fileName: undefined,
        queued: undefined,
        master: undefined,
        excluded: undefined,
        inherited: undefined,
        inheritedFrom: undefined,
        inheritedFromId: undefined,
        masterNode: undefined,
        masterNodeId: undefined,
        disinheritDefault: undefined,
        disinherited: undefined,
    }
}

export function getExampleFormDataNormalized(
    { id, userId }: { id: number, userId?: number } = { id: 95, userId: 3 },
): Form<Normalized> {
    const rawForm = getExampleFormData({ id, userId });
    const normalized = normalize(rawForm, schemas.form);
    return normalized.entities.form[normalized.result] as Form<Normalized>;
}


/** Returns actual folder data from the ContentNode API for "GCN5 Demo" */
export function getExampleFolderData({ id, userId, publishDir }: { id: number, userId?: number, publishDir?: string }
= { id: 115, userId: 3, publishDir: '/' }): Folder<Raw> {
    userId = userId || 3;
    publishDir = publishDir || '/';
    return {
        disinherited: false,
        excluded: false,
        disinheritDefault: false,
        nodeId: 1,
        motherId: 1,
        publishDir: publishDir,
        masterId: 0,
        channelId: 0,
        masterNodeId: 1,
        inheritedFrom: 'GCN5 Demo',
        inheritedFromId: 1,
        masterNode: 'GCN5 Demo',
        atposidx: `-1-${id}`,
        hasSubfolders: false,
        privilegeMap: {
            privileges: {
                viewfolder: true,
                createfolder: true,
                updatefolder: true,
                deletefolder: true,
                assignpermissions: true,
                viewpage: true,
                createpage: true,
                updatepage: true,
                deletepage: true,
                publishpage: true,
                viewfile: true,
                createfile: true,
                updatefile: true,
                deletefile: true,
                viewtemplate: true,
                createtemplate: true,
                linktemplate: true,
                updatetemplate: true,
                deletetemplate: true,
                updatetagtypes: true,
                inheritance: true,
                importpage: true,
                linkworkflow: true,
                synchronizechannel: false,
                wastebin: true,
                translatepage: true,
                createform: true,
                updateform: true,
                deleteform: true,
                publishform: true,
                viewform: true,
                formreport: true,
            },
            languages: [
                {
                    privileges: {
                        viewpage: true,
                        createpage: true,
                        updatepage: true,
                        deletepage: true,
                        publishpage: true,
                        translatepage: false,
                        viewfile: true,
                        createfile: true,
                        updatefile: true,
                        deletefile: true,
                    },
                    language: {
                        code: 'en',
                        name: 'English',
                        id: 2,
                    },
                } as any,
                {
                    privileges: {
                        viewpage: true,
                        createpage: true,
                        updatepage: true,
                        deletepage: true,
                        publishpage: true,
                        translatepage: false,
                        viewfile: true,
                        createfile: true,
                        updatefile: true,
                        deletefile: true,
                    },
                    language: {
                        code: 'de',
                        name: 'Deutsch (German)',
                        id: 1,
                    },
                },
            ],
        },
        channelsetId: 487,
        isMaster: true,
        description: '',
        inherited: false,
        path: '/GCN5 Demo/A new folder/',
        editor: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        creator: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        globalId: '554F.8c7109f7-dd93-11e6-873c-0242ac120002',
        deleted: {
            at: 1484753650,
            by: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                lastName: 'Admin',
                id: userId,
            },
        },
        cdate: 1484753646,
        edate: 1484753646,
        name: 'A new folder',
        id: id,
        type: 'folder',
        breadcrumbs: [
            {
                id: 1,
                name: 'GCN5 Demo',
            },
            {
                id: 2,
                name: 'A new folder',
            },
        ],
    };
}

export function getExampleFolderDataNormalized({ id, userId, publishDir }: { id: number, userId?: number, publishDir?: string }
= { id: 115, userId: 3, publishDir: '/' }): Folder<Normalized> {
    const rawFolder = getExampleFolderData({ id, userId, publishDir });
    const normalized = normalize(rawFolder, schemas.folder);
    return normalized.entities.folder[normalized.result] as Folder<Normalized>;
}

/** Returns actual user data from the ContentNode API for "GCN5 Demo" */
export function getExampleImageData({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): Image<Raw> {
    userId = userId || 3;
    return {
        '@class': 'com.gentics.contentnode.rest.model.Image',
        typeId: 10011,
        iconCls: 'gtx_image',
        gisResizable: true,
        sizeX: 600,
        sizeY: 331,
        dpiX: 0,
        dpiY: 0,
        text: 'aloha_editor.png',
        fileType: 'image/png',
        folderName: '[Images]',
        cls: 'file',
        leaf: true,
        description: '',
        disinherited: false,
        excluded: false,
        disinheritDefault: false,
        online: true,
        channelId: 0,
        folderId: 21,
        fpX: 0.5,
        fpY: 0.5,
        fileSize: 150318,
        forceOnline: false,
        broken: false,
        masterNodeId: 1,
        inheritedFrom: 'GCN5 Demo',
        inheritedFromId: 1,
        masterNode: 'GCN5 Demo',
        liveUrl: '',
        inherited: false,
        path: '/GCN5 Demo/[Media]/[Images]/',
        publishPath: '/Content.Node/images/aloha_editor.png',
        globalId: 'A547.72642',
        editor: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        creator: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        cdate: 1288632521,
        edate: 1288632521,
        name: 'aloha_editor.png',
        id: id,
        type: 'image',
    };
}

export function getExampleImageDataNormalized({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): Image<Normalized> {
    const rawImage = getExampleImageData({ id, userId });
    const normalized = normalize(rawImage, schemas.image);
    return normalized.entities.image[normalized.result] as Image<Normalized>;
}

export function getExampleFileData({ id, userId }: { id: number, userId?: number } = { id: 41, userId: 3 }): FileModel<Raw> {
    return {
        id: id,
        globalId: 'A547.74274',
        name: 'Gentics_Content_Node_Technologie.pdf',
        creator: {
            id: userId,
            firstName: 'Node',
            lastName: 'Admin',
            email: 'nowhere@gentics.com',
        },
        cdate: 1303996901,
        editor: {
            id: userId,
            firstName: 'Node',
            lastName: 'Admin',
            email: 'nowhere@gentics.com',
        },
        edate: 1303996901,
        type: 'file',
        typeId: 10008,
        fileType: 'application/pdf',
        description: '',
        folderId: 21,
        folderName: '[Files]',
        fileSize: 388811,
        channelId: 0,
        inherited: false,
        tags: { },
        liveUrl: '',
        publishPath: '/Content.Node/files/Gentics_Content_Node_Technologie.pdf',
        inheritedFrom: 'GCN5 Demo',
        inheritedFromId: 1,
        masterNode: 'GCN5 Demo',
        masterNodeId: 1,
        path: '/GCN5 Demo/[Media]/[Files]/',
        forceOnline: false,
        online: true,
        broken: false,
        excluded: false,
        disinheritDefault: false,
        disinherited: false,
        leaf: true,
        cls: 'file',
        iconCls: 'gtx_file',
        text: 'Gentics_Content_Node_Technologie.pdf',
    };
}

export function getExampleFileDataNormalized({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): FileModel<Normalized> {
    const rawFile = getExampleFileData({ id, userId });
    const normalized = normalize(rawFile, schemas.file);
    return normalized.entities.file[normalized.result] as FileModel<Normalized>;
}

/** Returns actual user data from the ContentNode API when editing an image in "GCN5 Demo" */
export function getExampleNewImageData({ id, userId }: { id: number, userId?: number } = { id: 1, userId: 3 }): FileModel<Raw> {
    userId = userId || 3;
    return {
        typeId: 10008,
        text: 'sprachreisen-hawaii_2.jpg',
        fileType: 'image/jpeg',
        folderName: '[Images]',
        cls: 'file',
        iconCls: 'gtx_file',
        leaf: true,
        description: '',
        tags: {
            objectcopyright: {
                constructId: 1,
                active: false,
                name: 'object.copyright',
                properties: {
                    text: {
                        partId: 1,
                        stringValue: '',
                        id: 4316,
                        type: TagPropertyType.STRING,
                    },
                },
                id: 1560,
                type: 'OBJECTTAG',
            },
        },
        disinherited: false,
        excluded: false,
        disinheritDefault: false,
        online: true,
        channelId: 0,
        folderId: 3,
        fileSize: 30923,
        forceOnline: false,
        broken: false,
        masterNodeId: 1,
        inheritedFrom: 'GCN5 Demo',
        inheritedFromId: 1,
        masterNode: 'GCN5 Demo',
        liveUrl: '',
        inherited: false,
        path: '/GCN5 Demo/[Media]/[Images]/',
        publishPath: '/Content.Node/images/sprachreisen-hawaii_2.jpg',
        globalId: '554F.b4844b21-f8dc-11e6-af33-0242ac120002',
        editor: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        creator: {
            email: 'nowhere@gentics.com',
            firstName: 'Node',
            lastName: 'Admin',
            id: userId,
        },
        cdate: 1487753748,
        edate: 1487753748,
        name: 'sprachreisen-hawaii_2.jpg',
        id: id,
        type: 'file',
    };
}

/** Returns actual user data from the ContentNode API for "GCN5 Demo" */
export function getExampleUserData({ id }: { id: number } = { id: 3 }): User<Raw> {
    return {
        email: 'nowhere@gentics.com',
        login: 'node',
        firstName: 'Node',
        lastName: 'Admin',
        description: 'System Administrator',
        id: id,
    };
}

export function getExampleObjectTag(data: Partial<ObjectTag> = {}): ObjectTag {
    return {
        constructId: 1,
        active: false,
        name: 'object.copyright',
        displayName: 'Copyright',
        description: 'Example Copyright',
        readOnly: false,
        inheritable: false,
        required: false,
        sortOrder: 10,
        properties: {
            text: {
                partId: 1,
                stringValue: '',
                id: 4316,
                type: TagPropertyType.STRING,
            },
        },
        id: 1560,
        type: 'OBJECTTAG',
        ...data,
    };
}

export function getExampleConstruct(data: Partial<TagType> = {}): TagType {
    return {
        id: 1337,
        keyword: 'example',
        parts: [],
        ...data,
    }
}

export function getExampleEditableObjectTag(data: Partial<EditableObjectTag> = {}): EditableObjectTag {
    return {
        ...getExampleObjectTag(),
        tagType: getExampleConstruct(),
        ...data,
    }
}


export function getExampleUserDataNormalized({ id }: { id: number } = { id: 3 }): User<Normalized> {
    const rawUser = getExampleUserData({ id });
    const normalized = normalize(rawUser, schemas.user);
    return normalized.entities.user[normalized.result] as User<Normalized>;
}

/** Returns actual node data from the ContentNode API for "GCN5 Demo" */
export function getExampleNodeData(config: MockNodeConfig = { id: 1, userId: 3 }): Node<Raw> {
    if (typeof config.inheritedFromId !== 'number') {
        config.inheritedFromId = config.id;
    }
    if (typeof config.masterNodeId !== 'number') {
        config.masterNodeId = config.inheritedFromId;
    }
    if (typeof config.folderId !== 'number') {
        config.folderId = config.id;
    }
    if (typeof config.userId !== 'number') {
        config.userId = 3;
    }
    const nodeName = config.id !== 1 ? `GCN5 Demo ${config.id}` : 'GCN5 Demo';

    return {
        id: config.id,
        globalId: `A547.69432${config.id}`,
        name: nodeName,
        creator: getExampleUserData({ id: config.userId }),
        cdate: 1280413310,
        editor: getExampleUserData({ id: config.userId }),
        edate: 1295049499,
        type: 'node',
        folderId: config.folderId,
        inheritedFromId: config.inheritedFromId,
        publishDir: '/Content.Node',
        binaryPublishDir: '/Content.Node',
        https: false,
        host: 'gcn5demo.gentics.com',
        hostProperty: null,
        utf8: true,
        publishFs: false,
        publishFsPages: false,
        publishFsFiles: false,
        publishContentMap: true,
        publishContentMapPages: true,
        publishContentMapFiles: true,
        publishContentMapFolders: true,
        contentRepositoryId: 1,
        disablePublish: false,
        languagesId: [
            2,
            1,
            5,
            17,
        ],
        editorVersion: 1,
        urlRenderWayPages: 0,
        urlRenderWayFiles: 0,
        meshProject: null,
        meshPreviewUrl: null,
        meshPreviewUrlProperty: null,
        insecurePreviewUrl: null,
        meshProjectName: null,
        masterNodeId: config.masterNodeId,
        omitPageExtension: false,
        pageLanguageCode: NodePageLanguageCode.FILENAME,
        publishImageVariants: false,
    };
}

export function getExampleNodeDataNormalized(config: MockNodeConfig = { id: 1, userId: 3 }): Node<Normalized> {
    const rawNode = getExampleNodeData(config);
    const normalized = normalize(rawNode, schemas.node);
    return normalized.entities.node[normalized.result] as Node<Normalized>;
}

/** Returns actual template data from the ContentNode API for "GCN5 Demo" */
export function getExampleTemplateData(
    {
        id,
        masterId,
        userId,
    }: {
        id: number,
        masterId: number,
        userId?: number,
    } = {
        id: 115,
        masterId: 1,
        userId: 3,
    }): Template<Raw> {

    masterId = masterId || 1;
    userId = userId || 3;

    return {
        id,
        globalId : 'A547.69449',
        masterId,
        type: 'template',
        name : 'Contentpage',
        description : '',
        creator : {
            id : userId,
            firstName : '.Node',
            lastName : 'Gentics',
            email : 'nowhere@gentics.com',
        },
        cdate : 1280413392,
        editor : {
            id : userId,
            firstName : '.Node',
            lastName : 'Gentics',
            email : 'nowhere@gentics.com',
        },
        edate : 1316086962,
        locked : false,
        inherited : false,
        master : true,
        markupLanguage : {
            id : 1,
            name : 'HTML',
            extension : 'html',
            contentType : 'text/html',
        },
        inheritedFrom : 'GCN5 Demo',
        masterNode : 'GCN5 Demo',
        channelSetId : 66,
        folderId: 0,
        path: '',
        objectTags: null,
        source: null,
        channelId: null,
        templateTags: {
            content : {
                id : 137,
                name : 'content',
                constructId : 65,
                active : true,
                editableInPage: true,
                mandatory: false,
                properties : {
                    text : {
                        type : TagPropertyType.RICHTEXT,
                        id : 685,
                        partId : 349,
                        stringValue : '<br><node blogposts1><br><br><br>',
                    },
                },
                type : 'CONTENTTAG',
            },
        },
    };
}

export function getExampleTemplateDataNormalized(
    {
        id,
        masterId,
        userId,
    }: {
        id: number,
        masterId: number,
        userId?: number,
    } = {
        id: 115,
        masterId: 1,
        userId: 3,
    }): Template<Normalized> {
    const rawTemplate = getExampleTemplateData({ id, masterId, userId });
    const normalized = normalize(rawTemplate, schemas.template);
    return normalized.entities.template[normalized.result] as Template<Normalized>;
}

export function getExampleFileObjectData(): Partial<File> {
    return {
        lastModified: 1591614031860,
        name: 'test.test',
        size: 3405,
        slice: {
            start: 1,
            end: 2,
            contentType: '',
        } as any,
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    };
}

export function getExampleReports(): any {
    return {
        totalCount : 2,
        currentPage : 1,
        pageCount : 1,
        perPage : 12,
        entries : [ {
            uuid : '291b80fc0f344b4abc15ff7fb0487da1',
            version : '1.0',
            fields : {
                input_fbeb53d4_79c5_4745_b60e_adb17fbf250 : '12345',
                file_127d4c49_9415_4ef8_aa1a_0aa96d1b94ce : {
                    fileName : 'öpäßü!_-$%&()[]{}+#éáúíóâêîôû.pdf',
                },
                errors : '',
            },
        }, {
            uuid : '72a7334c56c641fe93a44b10bdffc98f',
            version : '1.0',
            fields : {
                input_fbeb53d4_79c5_4745_b60e_adb17fbf2508 : '234234',
                file_127d4c49_9415_4ef8_aa1a_0aa96d1b94ce : {
                    fileName : '1.jpg',
                },
                errors : '',
            },
        } ],
        elements : {
            buttons_7f4a6124_865b_4c04_937b_d43ea4c8495f : {
                type : 'buttons',
                active : true,
                mandatory : false,
                elements : [],
                multivalue : false,
                globalId : '7f4a6124-865b-4c04-937b-d43ea4c8495f',
                submitlabel : 'Submit',
                resetlabel : null,
                showreset : null,
            },
            file_127d4c49_9415_4ef8_aa1a_0aa96d1b94ce : {
                label : 'Fileupload',
                type : 'file',
                active : true,
                mandatory : false,
                elements : [],
                multivalue : false,
                max_filesize : 0,
                globalId : '127d4c49-9415-4ef8-aa1a-0aa96d1b94ce',
            },
            input_fbeb53d4_79c5_4745_b60e_adb17fbf2508 : {
                label : 'Input 1 Mandatory telephone',
                type : 'input',
                active : true,
                mandatory : true,
                validation : 'telephone',
                elements : [],
                multivalue : false,
                globalId : 'fbeb53d4-79c5-4745-b60e-adb17fbf2508',
                maxVal : null,
                placeholder : null,
                minVal : null,
                maxCharCount : null,
                value : null,
            },
        },
    };
}
