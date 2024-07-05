import { Folder, Node, NodeFeature, Page, Template } from '@gentics/cms-models';
import { GCMSRestClient } from '@gentics/cms-rest-client';
import { EntityMap, ENV_CMS_PASSWORD, ENV_CMS_REST_PATH, ENV_CMS_USERNAME, TestSize } from './common';
import { CypressDriver } from './cypress-driver';
import {
    FolderImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    ImportData,
    NodeImportData,
    PACKAGE_IMPORTS,
    PACKAGE_MAP,
    PageImportData,
    emptyNode,
} from './entities';

export interface ImportBootstrapData {
    dummyNode: number;
    languages: Record<string, number>;
    templates: Record<string, Template>;
}

function createClient(): Promise<GCMSRestClient> {
    let sid: number | null = null;
    const client = new GCMSRestClient(
        new CypressDriver(),
        {
            // Connection is either via DEV Server or direct, but doesn't matter
            connection: {
                absolute: false,
                basePath: Cypress.env(ENV_CMS_REST_PATH),
            },
            interceptors: [
                (data) => {
                    if (sid != null) {
                        data.params['sid'] = `${sid}`;
                    }
                    return data;
                },
            ],
        },
    );

    return client.auth.login({
        login: Cypress.env(ENV_CMS_USERNAME),
        password: Cypress.env(ENV_CMS_PASSWORD),
    })
        .send()
        .then(res => {
            sid = res.sid;
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
    const created = (await client.node.create(req).send()).node;

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
        if (tpl) {
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

    const created = (await client.folder.create({
        ...req,

        motherId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
    }).send()).folder;

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
        ...req
    } = data;

    const folderEntity = entityMap[folderId];
    if (!folderEntity) {
        return null;
    }

    const parentId = (folderEntity as Node).folderId ?? (folderEntity as (Node | Folder)).id;
    const tplId = (entityMap[templateId] as Template).id;

    const created = (await client.page.create({
        ...req,

        folderId: parentId,
        nodeId: (entityMap[nodeId] as Node).id,
        templateId: tplId,
    }).send()).page;

    return created;
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
): Promise<any> {
    switch (type) {
        case 'node':
            return importNode(client, pkgName, entityMap, languages, entity as NodeImportData);

        case 'folder':
            return importFolder(client, entityMap, entity as FolderImportData);

        case 'page':
            return importPage(client, entityMap, entity as PageImportData);

        default:
            return Promise.resolve(null);
    }
}

async function setupContent(
    client: GCMSRestClient,
    pkgName: TestSize,
    data: ImportBootstrapData,
): Promise<Record<string, number>> {
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

export async function setupTest(size: TestSize, data: ImportBootstrapData): Promise<EntityMap> {
    const client = await createClient();

    const map = await setupContent(client, size, data);

    return map;
}

export async function cleanupTest(): Promise<void> {
    const client = await createClient();

    const nodes = (await client.node.list().send()).items || [];

    for (const node of nodes) {
        // Skip the node
        if (node.name === emptyNode.node.name) {
            continue;
        }
        await client.node.delete(node.id).send();
    }
}
