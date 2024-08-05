import {
    File,
    FileUploadOptions,
    Folder,
    FolderCreateRequest,
    Group,
    Node,
    NodeFeature,
    Page,
    PageCreateRequest,
    PagingSortOrder,
    Template,
    User,
} from '@gentics/cms-models';
import { GCMSRestClient, GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import {
    BinaryMap,
    EntityMap,
    ENV_CMS_PASSWORD,
    ENV_CMS_REST_PATH,
    ENV_CMS_USERNAME,
    FileImportData,
    FolderImportData,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_USER,
    ImportData,
    ITEM_TYPE_FILE,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    NodeImportData,
    PageImportData,
    TestSize,
    UserImportData,
} from './common';
import { CypressDriver } from './cypress-driver';
import {
    emptyNode,
    PACKAGE_IMPORTS,
    PACKAGE_MAP,
} from './entities';

// To enable for debugging
const IMPORT_LOGGING = false;

export interface ImportBootstrapData {
    dummyNode: number;
    languages: Record<string, number>;
    templates: Record<string, Template>;
}

export function createClient(): Promise<GCMSRestClient> {
    const client = new GCMSRestClient(
        new CypressDriver(),
        {
            // The baseUrl (aka. protocol/host/port) has to be already setup when started
            connection: {
                absolute: false,
                basePath: Cypress.env(ENV_CMS_REST_PATH),
            },
        },
    );

    return client.auth.login({
        login: Cypress.env(ENV_CMS_USERNAME),
        password: Cypress.env(ENV_CMS_PASSWORD),
    })
        .send()
        .then(res => {
            // Set the SID for future requests
            client.sid = res.sid;
            return client;
        });
}

export async function setNodeFeatures(
    client: GCMSRestClient,
    nodeId: number | string,
    features: NodeFeature[],
): Promise<void> {
    for (const feature of features) {
        await client.node.activateFeature(nodeId, feature).send();
    }
}

async function importNode(
    client: GCMSRestClient,
    pkgName: TestSize | null,
    entityMap: EntityMap,
    langMap: Record<string, number>,
    data: NodeImportData,
): Promise<Node> {
    const {
        features,
        languages,
        templates,
        ...req
    } = data;

    if (IMPORT_LOGGING) {
        cy.log(`Importing node ${data[IMPORT_ID]}`, req);
    }
    const created = (await client.node.create(req).send()).node;
    if (IMPORT_LOGGING) {
        cy.log(`Imported node ${data[IMPORT_ID]} -> ${created.id} (${created.folderId})`);
    }

    await setNodeFeatures(client, created.id, features);

    for (const lang of languages) {
        await client.node.assignLanguage(created.id, langMap[lang]).send();
    }

    // Assigns all Dev-Tool package elements to the node
    let packages: string[];
    if (pkgName) {
        packages = PACKAGE_IMPORTS[pkgName];
    } else {
        // If no package is provided, it'll aggregate all of them and assign those
        packages = Array.from(new Set(Object.values(PACKAGE_IMPORTS).flatMap(v => v)));
    }
    for (const pkg of packages) {
        await client.devTools.assignToNode(pkg, created.id).send();
    }

    // We need the local template-ids for page references, so load all referenced templates via global id
    for (const tplId of templates) {
        const tpl = (await client.template.get(tplId).send()).template;

        // Additionally, we have to link the templates to the root-folder
        await client.template.link(tplId, {
            nodeId: created.id,
            folderIds: [created.folderId || created.id],
        }).send();

        if (tpl) {
            if (IMPORT_LOGGING) {
                cy.log(`Loaded node template ${tplId} -> ${tpl.id}`);
            }
            entityMap[tplId] = tpl;
        }
    }

    return created;
}

async function importFolder(
    client: GCMSRestClient,
    entityMap: EntityMap,
    data: FolderImportData,
): Promise<Folder | null> {
    const {
        motherId,
        nodeId,
        ...req
    } = data;

    const parentEntity = entityMap[motherId];
    if (!parentEntity) {
        return null;
    }
    const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
    const body: FolderCreateRequest = {
        ...req,

        motherId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
    };

    if (IMPORT_LOGGING) {
        cy.log(`Importing folder ${data[IMPORT_ID]}`, body);
    }
    const created = (await client.folder.create(body).send()).folder;
    if (IMPORT_LOGGING) {
        cy.log(`Imported folder ${data[IMPORT_ID]} -> ${created.id}`);
    }

    return created;
}

async function importPage(
    client: GCMSRestClient,
    entityMap: EntityMap,
    data: PageImportData,
): Promise<Page | null> {
    const {
        folderId,
        nodeId,
        templateId,
        tags,
        ...req
    } = data;

    const folderEntity = entityMap[folderId];
    if (!folderEntity) {
        return null;
    }

    const parentId = (folderEntity as Node).folderId ?? (folderEntity as (Node | Folder)).id;
    const tplId = (entityMap[templateId] as Template).id;
    const body: PageCreateRequest = {
        ...req,

        folderId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
        templateId: tplId,
    };

    if (IMPORT_LOGGING) {
        cy.log(`Importing page ${data[IMPORT_ID]}`, body);
    }
    const created = (await client.page.create(body).send()).page;
    if (tags) {
        await client.page.update(created.id, {
            page: {
                tags,
            },
        }).send();
    }
    if (IMPORT_LOGGING) {
        cy.log(`Imported page ${data[IMPORT_ID]} -> ${created.id}`);
    }

    return created;
}

async function importFile(
    client: GCMSRestClient,
    entityMap: EntityMap,
    binaryMap: BinaryMap,
    data: FileImportData,
): Promise<File | null> {
    const { folderId, nodeId, ...updateData } = data;

    const bin = binaryMap[data[IMPORT_ID]];

    if (!bin) {
        if (IMPORT_LOGGING) {
            cy.log(`No binary for ${data[IMPORT_ID]} defined!`);
        }
        return;
    }

    const parentEntity = entityMap[folderId];
    const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
    const body: FileUploadOptions = {
        folderId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
    };

    if (IMPORT_LOGGING) {
        cy.log(`Importing file ${data[IMPORT_ID]}`, body);
    }
    const created = (await client.file.upload(new Blob([bin]), body).send())?.file;
    if (IMPORT_LOGGING) {
        cy.log(`Imported file ${data[IMPORT_ID]} ->`, created);
    }

    await client.file.update(created.id, { file: updateData }).send();

    return created;
}

async function importImage(
    client: GCMSRestClient,
    entityMap: EntityMap,
    binaryMap: BinaryMap,
    data: ImageImportData,
): Promise<File | null> {
    const { folderId, nodeId, ...updateData } = data;

    const bin = binaryMap[data[IMPORT_ID]];

    if (!bin) {
        if (IMPORT_LOGGING) {
            cy.log(`No binary for ${data[IMPORT_ID]} defined!`);
        }
        return;
    }

    const parentEntity = entityMap[folderId];
    const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
    const body: FileUploadOptions = {
        folderId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
    };

    if (IMPORT_LOGGING) {
        cy.log(`Importing image ${data[IMPORT_ID]}`, data);
    }
    const created = (await client.file.upload(new Blob([bin]), body).send()).file;
    if (IMPORT_LOGGING) {
        cy.log(`Imported image ${data[IMPORT_ID]} -> ${created.id}`);
    }

    await client.image.update(created.id, { image: updateData }).send();

    return created;
}

async function importGroup(
    client: GCMSRestClient,
    entityMap: EntityMap,
    data: GroupImportData,
): Promise<Group | null> {
    const { parent, permissions, ...reqData } = data;

    let parentId: number | null = null;

    if (parent != null) {
        parentId = (entityMap[parent] as Group)?.id;
    }

    if (parentId == null) {
        parentId = (await client.group.list({
            pageSize: 1,
            sort: [{ attribute: 'id', sortOrder: PagingSortOrder.Asc }],
        }).send())?.items?.[0]?.id;
    }

    let importedGroup: Group;

    try {
        if (IMPORT_LOGGING) {
            cy.log(`Importing group ${data[IMPORT_ID]}`, data);
        }
        importedGroup = (await client.group.create(parentId, reqData).send()).group;
        if (IMPORT_LOGGING) {
            cy.log(`Imported group ${data[IMPORT_ID]} -> ${importedGroup.id}`);
        }
    } catch (err) {
        // If the group already exists, ignore it
        if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
            throw err;
        }

        if (IMPORT_LOGGING) {
            cy.log(`Group ${data[IMPORT_ID]} already exists`);
        }
        const foundGroups = (await client.group.list({ q: reqData.name }).send()).items || [];
        importedGroup = foundGroups.filter(g => g.name === reqData.name)?.[0] ?? foundGroups?.[0];
    }

    if (importedGroup && permissions) {
        for (const perm of permissions) {
            const body = {
                perms: perm.perms,
                subGroups: perm.subGroups ?? false,
                subObjects: perm.subObjects ?? false,
            };

            if (perm.instanceId) {
                await client.group.setInstancePermission(
                    importedGroup.id,
                    perm.type,
                    entityMap[perm.instanceId]?.id ?? perm.instanceId,
                    body,
                ).send();
            } else {
                await client.group.setPermission(importedGroup.id, perm.type, body).send();
            }
        }
    }

    return importedGroup;
}

async function importUser(
    client: GCMSRestClient,
    entityMap: EntityMap,
    data: UserImportData,
): Promise<User | null> {
    const { group, ...reqData } = data;

    try {
        if (IMPORT_LOGGING) {
            cy.log(`Importing user ${data[IMPORT_ID]}`, data);
        }
        const created = (await client.group.createUser((entityMap[group] as Group).id, reqData).send()).user;
        if (IMPORT_LOGGING) {
            cy.log(`Imported user ${data[IMPORT_ID]} -> ${created.id}`);
        }

        return created;
    } catch (err) {
        // If the user already exists, ignore it
        if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
            throw err;
        }

        if (IMPORT_LOGGING) {
            cy.log(`User ${data[IMPORT_ID]} already exists`);
        }
        const foundUsers = (await client.user.list({ q: data.login }).send()).items || [];
        const found = foundUsers.filter(g => g.login === data.login)?.[0] ?? foundUsers?.[0];

        return found;
    }
}

async function getLanguageMapping(client: GCMSRestClient): Promise<Record<string, number>> {
    const res = await client.language.list().send();
    const mapping: Record<string, number> = {};

    for (const lang of res.items) {
        mapping[lang.code] = lang.id;
    }

    return mapping;
}

async function getTemplateMapping(client: GCMSRestClient): Promise<Record<string, Template>> {
    const templates = (await client.template.list({ reduce: true }).send()).templates || [];
    const mapping: Record<string, Template> = {};

    for (const tpl of templates) {
        mapping[tpl.globalId] = tpl;
    }

    return mapping;
}

function importEntity(
    client: GCMSRestClient,
    pkgName: TestSize,
    entityMap: EntityMap,
    languages: Record<string, number>,
    type: string,
    entity: ImportData,
    binaryData?: BinaryMap,
): Promise<any> {
    switch (type) {
        case IMPORT_TYPE_NODE:
            return importNode(client, pkgName, entityMap, languages, entity as NodeImportData);

        case ITEM_TYPE_FOLDER:
            return importFolder(client, entityMap, entity as FolderImportData);

        case ITEM_TYPE_PAGE:
            return importPage(client, entityMap, entity as PageImportData);

        case ITEM_TYPE_FILE:
            return importFile(client, entityMap, binaryData, entity as FileImportData);

        case ITEM_TYPE_IMAGE:
            return importImage(client, entityMap, binaryData, entity as ImageImportData);

        case IMPORT_TYPE_GROUP:
            return importGroup(client, entityMap, entity as GroupImportData);

        case IMPORT_TYPE_USER:
            return importUser(client, entityMap, entity as UserImportData);

        default:
            return Promise.resolve(null);
    }
}

export async function importData(
    importList: ImportData[],
    binaryData?: BinaryMap,
): Promise<EntityMap> {
    const client = await createClient();
    const entityMap: EntityMap = {};

    for (const importData of importList) {
        const entity = await importEntity(
            client,
            null,
            entityMap,
            {},
            importData[IMPORT_TYPE],
            importData,
            binaryData,
        );
        if (!entity) {
            continue;
        }
        entityMap[importData[IMPORT_ID]] = entity;
    }

    return entityMap;
}

async function setupContent(
    client: GCMSRestClient,
    pkgName: TestSize,
    data: ImportBootstrapData,
    binaryData?: BinaryMap,
): Promise<EntityMap> {
    const importList = PACKAGE_MAP[pkgName];
    if (!importList) {
        return {};
    }

    const entityMap: EntityMap = {
        ...data.templates,
    };

    // Then attempt to import all
    for (const importData of importList) {
        const entity = await importEntity(
            client,
            pkgName,
            entityMap,
            data.languages,
            importData[IMPORT_TYPE],
            importData,
            binaryData,
        );
        if (!entity) {
            continue;
        }
        entityMap[importData[IMPORT_ID]] = entity;
    }

    return entityMap;
}

async function setupDummyNode(client: GCMSRestClient, languages: Record<string, number>): Promise<number> {
    let nodeId: number | null = null;

    const nodesRes = await client.node.list().send();
    for (const node of nodesRes.items) {
        if (node.name === emptyNode.node.name) {
            nodeId = node.id;
        }
    }

    if (nodeId == null) {
        const importedNode = await importNode(client, null, {}, languages, emptyNode);
        nodeId = importedNode.id;
    }

    return nodeId;
}

export async function bootstrapSuite(size: TestSize): Promise<ImportBootstrapData> {
    const client = await createClient();

    // First import all dev-tool packages from the FS
    const devtoolPackages = PACKAGE_IMPORTS[size] || [];
    for (const devtoolPkg of devtoolPackages) {
        await client.devTools.syncFromFileSystem(devtoolPkg).send();
    }

    const templates = await getTemplateMapping(client);
    const languages = await getLanguageMapping(client);
    const dummyNode = await setupDummyNode(client, languages);

    return {
        templates,
        languages,
        dummyNode,
    };
}

export async function setupTest(size: TestSize, data: ImportBootstrapData, binaryData: BinaryMap = {}): Promise<EntityMap> {
    const client = await createClient();

    const map = await setupContent(client, size, data, binaryData);

    return map;
}

async function clearEmptyNodeForDeletion(client: GCMSRestClient, node: Node): Promise<void> {
    const constructsRes = await client.node.listConstructs(node.id).send();
    const packagesRes = await client.devTools.listFromNodes(node.id).send();
    const objPropRes = await client.node.listObjectProperties(node.id).send();

    for (const construct of (constructsRes.items || [])) {
        await client.node.unassignConstruct(node.id, construct.id).send();
    }

    for (const pkg of (packagesRes.items || [])) {
        await client.devTools.unassignFromNode(pkg.name, node.id).send();
    }

    for (const objProp of (objPropRes.items || [])) {
        await client.node.unassignObjectProperty(node.id, objProp.id).send();
    }
}

export async function cleanupTest(completeClean: boolean = false): Promise<void> {
    const client = await createClient();

    const nodes = (await client.node.list().send()).items || [];

    for (const node of nodes) {
        if (node.name === emptyNode.node.name) {
            // Skip the node if it's a simple cleanup
            if (!completeClean) {
                continue;
            }
        }

        await clearEmptyNodeForDeletion(client, node);
        await client.node.delete(node.id).send();
    }
}
