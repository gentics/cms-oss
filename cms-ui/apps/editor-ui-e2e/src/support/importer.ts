import { Folder, Node, Page, Template } from '@gentics/cms-models';
import { GCMSRestClient } from '@gentics/cms-rest-client';
import { setup } from '../fixtures/auth.json';
import { TestSize } from './common';
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
} from './entities';

export interface ImportBootstrapData {
    languages: Record<string, number>;
    templates: Record<string, Template>;
}

type EntityMap = Record<string, any>;

function createClient(loginData: { username: string, password: string }): Promise<GCMSRestClient> {
    let sid: number | null = null;
    const client = new GCMSRestClient(
        new CypressDriver(),
        {
            connection: {
                absolute: true,
                host: 'localhost',
                port: 8080,
                ssl: false,
                basePath: '/rest',
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
        login: loginData.username,
        password: loginData.password,
    })
        .send()
        .then(res => {
            sid = res.sid;
            return client;
        });
}

async function importNode(
    client: GCMSRestClient,
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
    for (const feature of features) {
        await client.node.activateFeature(created.id, feature).send();
    }
    for (const lang of languages) {
        await client.node.assignLanguage(created.id, langMap[lang]).send();
    }
    for (const tplId of templates) {
        await client.node.assignTemplate(created.id, tplId).send()
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
    entityMap: EntityMap,
    languages: Record<string, number>,
    type: string,
    entity: ImportData,
): Promise<any> {
    switch (type) {
        case 'node':
            return importNode(client, entityMap, languages, entity as NodeImportData);

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
        const entity = await importEntity(client, entityMap, data.languages, importData[IMPORT_TYPE], importData);
        if (!entity) {
            continue;
        }
        entityMap[importData[IMPORT_ID]] = entity;
    }

    return entityMap;
}

export async function bootstrapSuite(size: TestSize): Promise<ImportBootstrapData> {
    const client = await createClient(setup);

    // First import all dev-tool packages from the FS
    const devtoolPackages = PACKAGE_IMPORTS[size] || [];
    for (const devtoolPkg of devtoolPackages) {
        await client.devTools.syncFromFileSystem(devtoolPkg).send();
    }

    const templates = await getTemplateMapping(client);
    const languages = await getLanguageMapping(client);

    return {
        templates,
        languages,
    };
}

export async function setupTest(size: TestSize, data: ImportBootstrapData): Promise<GCMSRestClient> {
    const client = await createClient(setup);

    await setupContent(client, size, data);

    return client;
}

export async function cleanupTest(): Promise<void> {
    const client = await createClient(setup);

    const nodes = (await client.node.list().send()).items || [];

    for (const node of nodes) {
        await client.node.delete(node.id).send();
    }
}
