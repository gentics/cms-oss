/* eslint-disable @typescript-eslint/no-use-before-define */
import {
    Feature,
    File,
    FileUploadOptions,
    Folder,
    FolderCreateRequest,
    Group,
    Image,
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
import { getFeatures, getItem, getNodeFeatures } from './utils';

export interface ImporterOptions {
    logRequests?: boolean;
    logImports?: boolean;
}

export interface ClientOptions {
    log?: boolean;
}

const DEFAULT_IMPORTER_OPTIONS: ImporterOptions = {
    logRequests: false,
    logImports: false,
}

export class EntityImporter {

    public client: GCMSRestClient | null = null;
    public entityMap: EntityMap = {};
    public binaryMap: BinaryMap = {};
    public dummyNode: number | null = null;
    public languages: Record<string, number> = {};
    public templates: Record<string, Template> = {};

    public bootstrapped = false;

    constructor(
        public options?: ImporterOptions,
    ) {
        this.options = {
            ...DEFAULT_IMPORTER_OPTIONS,
            ...(this.options || {}),
        };
    }

    public async importData(
        importList: ImportData[],
    ): Promise<EntityMap> {
        if (!this.client) {
            this.client = await createClient({ log: this.options?.logRequests });
        }

        for (const importData of importList) {
            const entity = await this.importEntity(
                null,
                importData[IMPORT_TYPE],
                importData,
            );
            if (!entity) {
                continue;
            }
            this.entityMap[importData[IMPORT_ID]] = entity;
        }

        return this.entityMap;
    }

    public async bootstrapSuite(size: TestSize): Promise<void> {
        if (!this.client) {
            this.client = await createClient({ log: this.options?.logRequests });
        }

        // First import all dev-tool packages from the FS
        const devtoolPackages = PACKAGE_IMPORTS[size] || [];
        for (const devtoolPkg of devtoolPackages) {
            await this.client.devTools.syncFromFileSystem(devtoolPkg).send();
        }

        this.templates = await this.getTemplateMapping();
        this.languages = await this.getLanguageMapping();
        this.dummyNode = await this.setupDummyNode();

        this.bootstrapped = true;
    }

    public async setupTest(size: TestSize): Promise<EntityMap> {
        if (!this.client) {
            this.client = await createClient({ log: this.options?.logRequests });
        }

        const map = await this.setupContent(size);

        return map;
    }

    public async cleanupTest(completeClean: boolean = false): Promise<void> {
        // For cleanups, we always create a new client
        this.client = await createClient({ log: this.options?.logRequests });

        // Reset the entity-map
        this.entityMap = {
            ...this.templates,
        };

        const nodes = (await this.client.node.list().send()).items || [];

        for (const node of nodes) {
            if (node.name === emptyNode.node.name) {
                // Skip the node if it's a simple cleanup
                if (!completeClean) {
                    continue;
                }
            }

            await this.clearEmptyNodeForDeletion(node);
            await this.client.node.delete(node.id).send();
        }
    }

    public async setupFeatures(
        size: TestSize,
        features: Partial<Record<Feature | NodeFeature, boolean>>,
    ): Promise<void> {
        // Reset to initial config
        await this.client.admin.reloadConfiguration().send();

        // Get all the required node-ids from the package
        const nodeIds: number[] = PACKAGE_MAP[size]
            .map(data => data[IMPORT_TYPE] === IMPORT_TYPE_NODE ? (this.get(data as NodeImportData))?.id : null)
            .filter(id => id != null);

        for (const entry of Object.entries(features || {})) {
            const [feature, enabled] = entry;
            if (Feature[feature]) {
                // TODO: Update the features when the endpoints are available
                // Don't add the endpoints to the client, as they are ment for testing only.
                continue;
            }

            for (const id of nodeIds) {
                if (enabled) {
                    await this.client.node.activateFeature(id, feature as NodeFeature).send();
                } else {
                    await this.client.node.deactivateFeature(id, feature as NodeFeature).send();
                }
            }
        }
    }

    public get(data: NodeImportData): Node | null;
    public get(data: FolderImportData): Folder | null;
    public get(data: PageImportData): Page | null;
    public get(data: ImageImportData): Image | null;
    public get(data: FileImportData): File | null;
    public get(data: GroupImportData): Group | null;
    public get(data: UserImportData): User | null;
    public get(data: ImportData | string): any {
        return getItem(data as any, this.entityMap);
    }

    private async setNodeFeatures(
        nodeId: number | string,
        features: NodeFeature[],
    ): Promise<void> {
        for (const feature of features) {
            await this.client.node.activateFeature(nodeId, feature).send();
        }
    }

    private async importNode(
        pkgName: TestSize | null,
        data: NodeImportData,
    ): Promise<Node> {
        const {
            languages,
            templates,
            ...req
        } = data;

        if (this.options?.logImports) {
            cy.log(`Importing node ${data[IMPORT_ID]}`, req);
        }
        const created = (await this.client.node.create(req).send()).node;
        if (this.options?.logImports) {
            cy.log(`Imported node ${data[IMPORT_ID]} -> ${created.id} (${created.folderId})`);
        }

        for (const lang of languages) {
            await this.client.node.assignLanguage(created.id, this.languages[lang]).send();
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
            await this.client.devTools.assignToNode(pkg, created.id).send();
        }

        // We need the local template-ids for page references, so load all referenced templates via global id
        for (const tplId of templates) {
            const tpl = (await this.client.template.get(tplId).send()).template;

            // Additionally, we have to link the templates to the root-folder
            await this.client.template.link(tplId, {
                nodeId: created.id,
                folderIds: [created.folderId || created.id],
            }).send();

            if (tpl) {
                if (this.options?.logImports) {
                    cy.log(`Loaded node template ${tplId} -> ${tpl.id}`);
                }
                this.entityMap[tplId] = tpl;
            }
        }

        return created;
    }

    private async importFolder(
        data: FolderImportData,
    ): Promise<Folder | null> {
        const {
            motherId,
            nodeId,
            ...req
        } = data;

        const parentEntity = this.entityMap[motherId];
        if (!parentEntity) {
            return null;
        }
        const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
        const body: FolderCreateRequest = {
            ...req,

            motherId: parentId,
            nodeId: (this.entityMap[nodeId] as Node).id,
        };

        if (this.options?.logImports) {
            cy.log(`Importing folder ${data[IMPORT_ID]}`, body);
        }
        const created = (await this.client.folder.create(body).send()).folder;
        if (this.options?.logImports) {
            cy.log(`Imported folder ${data[IMPORT_ID]} -> ${created.id}`);
        }

        return created;
    }

    private async importPage(
        data: PageImportData,
    ): Promise<Page | null> {
        const {
            folderId,
            nodeId,
            templateId,
            tags,
            ...req
        } = data;

        const folderEntity = this.entityMap[folderId];
        if (!folderEntity) {
            return null;
        }

        const parentId = (folderEntity as Node).folderId ?? (folderEntity as (Node | Folder)).id;
        const tplId = (this.entityMap[templateId] as Template).id;
        const body: PageCreateRequest = {
            ...req,

            folderId: parentId,
            nodeId: (this.entityMap[nodeId] as Node).id,
            templateId: tplId,
        };

        if (this.options?.logImports) {
            cy.log(`Importing page ${data[IMPORT_ID]}`, body);
        }
        const created = (await this.client.page.create(body).send()).page;
        if (tags) {
            await this.client.page.update(created.id, {
                page: {
                    tags,
                },
            }).send();
        }
        if (this.options?.logImports) {
            cy.log(`Imported page ${data[IMPORT_ID]} -> ${created.id}`);
        }

        return created;
    }

    private async importFile(
        data: FileImportData,
    ): Promise<File | null> {
        const { folderId, nodeId, ...updateData } = data;

        const bin = this.binaryMap[data[IMPORT_ID]];

        if (!bin) {
            if (this.options?.logImports) {
                cy.log(`No binary for ${data[IMPORT_ID]} defined!`);
            }
            return;
        }

        const parentEntity = this.entityMap[folderId];
        const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
        const body: FileUploadOptions = {
            folderId: parentId,
            nodeId: (this.entityMap[nodeId] as Node).id,
        };

        if (this.options?.logImports) {
            cy.log(`Importing file ${data[IMPORT_ID]}`, body);
        }
        const created = (await this.client.file.upload(new Blob([bin]), body).send())?.file;
        if (this.options?.logImports) {
            cy.log(`Imported file ${data[IMPORT_ID]} ->`, created);
        }

        await this.client.file.update(created.id, { file: updateData }).send();

        return created;
    }

    private async importImage(
        data: ImageImportData,
    ): Promise<File | null> {
        const { folderId, nodeId, ...updateData } = data;

        const bin = this.binaryMap[data[IMPORT_ID]];

        if (!bin) {
            if (this.options?.logImports) {
                cy.log(`No binary for ${data[IMPORT_ID]} defined!`);
            }
            return;
        }

        const parentEntity = this.entityMap[folderId];
        const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
        const body: FileUploadOptions = {
            folderId: parentId,
            nodeId: (this.entityMap[nodeId] as Node).id,
        };

        if (this.options?.logImports) {
            cy.log(`Importing image ${data[IMPORT_ID]}`, data);
        }
        const created = (await this.client.file.upload(new Blob([bin]), body).send()).file;
        if (this.options?.logImports) {
            cy.log(`Imported image ${data[IMPORT_ID]} -> ${created.id}`);
        }

        await this.client.image.update(created.id, { image: updateData }).send();

        return created;
    }

    private async importGroup(
        data: GroupImportData,
    ): Promise<Group | null> {
        const { parent, permissions, ...reqData } = data;

        let parentId: number | null = null;

        if (parent != null) {
            parentId = (this.entityMap[parent] as Group)?.id;
        }

        if (parentId == null) {
            parentId = (await this.client.group.list({
                pageSize: 1,
                sort: [{ attribute: 'id', sortOrder: PagingSortOrder.Asc }],
            }).send())?.items?.[0]?.id;
        }

        let importedGroup: Group;

        try {
            if (this.options?.logImports) {
                cy.log(`Importing group ${data[IMPORT_ID]}`, data);
            }
            importedGroup = (await this.client.group.create(parentId, reqData).send()).group;
            if (this.options?.logImports) {
                cy.log(`Imported group ${data[IMPORT_ID]} -> ${importedGroup.id}`);
            }
        } catch (err) {
            // If the group already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                cy.log(`Group ${data[IMPORT_ID]} already exists`);
            }
            const foundGroups = (await this.client.group.list({ q: reqData.name }).send()).items || [];
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
                    await this.client.group.setInstancePermission(
                        importedGroup.id,
                        perm.type,
                        this.entityMap[perm.instanceId]?.id ?? perm.instanceId,
                        body,
                    ).send();
                } else {
                    await this.client.group.setPermission(importedGroup.id, perm.type, body).send();
                }
            }
        }

        return importedGroup;
    }

    private async importUser(
        data: UserImportData,
    ): Promise<User | null> {
        const { group, ...reqData } = data;

        try {
            if (this.options?.logImports) {
                cy.log(`Importing user ${data[IMPORT_ID]}`, data);
            }
            const created = (await this.client.group.createUser((this.entityMap[group] as Group).id, reqData).send()).user;
            if (this.options?.logImports) {
                cy.log(`Imported user ${data[IMPORT_ID]} -> ${created.id}`);
            }

            return created;
        } catch (err) {
            // If the user already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                cy.log(`User ${data[IMPORT_ID]} already exists`);
            }
            const foundUsers = (await this.client.user.list({ q: data.login }).send()).items || [];
            const found = foundUsers.filter(g => g.login === data.login)?.[0] ?? foundUsers?.[0];

            return found;
        }
    }

    private async getLanguageMapping(): Promise<Record<string, number>> {
        const res = await this.client.language.list().send();
        const mapping: Record<string, number> = {};

        for (const lang of res.items) {
            mapping[lang.code] = lang.id;
        }

        return mapping;
    }

    private async getTemplateMapping(): Promise<Record<string, Template>> {
        const templates = (await this.client.template.list({ reduce: true }).send()).templates || [];
        const mapping: Record<string, Template> = {};

        for (const tpl of templates) {
            mapping[tpl.globalId] = tpl;
        }

        return mapping;
    }

    private importEntity(
        pkgName: TestSize,
        type: string,
        entity: ImportData,
    ): Promise<any> {
        switch (type) {
            case IMPORT_TYPE_NODE:
                return this.importNode(pkgName, entity as NodeImportData);

            case ITEM_TYPE_FOLDER:
                return this.importFolder(entity as FolderImportData);

            case ITEM_TYPE_PAGE:
                return this.importPage(entity as PageImportData);

            case ITEM_TYPE_FILE:
                return this.importFile(entity as FileImportData);

            case ITEM_TYPE_IMAGE:
                return this.importImage(entity as ImageImportData);

            case IMPORT_TYPE_GROUP:
                return this.importGroup(entity as GroupImportData);

            case IMPORT_TYPE_USER:
                return this.importUser(entity as UserImportData);

            default:
                return Promise.resolve(null);
        }
    }

    private async setupContent(
        pkgName: TestSize,
    ): Promise<EntityMap> {
        const importList = PACKAGE_MAP[pkgName];
        if (!importList) {
            return {};
        }

        // Then attempt to import all
        for (const importData of importList) {
            const entity = await this.importEntity(
                pkgName,
                importData[IMPORT_TYPE],
                importData,
            );
            if (!entity) {
                continue;
            }
            this.entityMap[importData[IMPORT_ID]] = entity;
        }

        return this.entityMap;
    }

    private async setupDummyNode(): Promise<number> {
        let nodeId: number | null = null;

        const nodesRes = await this.client.node.list().send();
        for (const node of nodesRes.items) {
            if (node.name === emptyNode.node.name) {
                nodeId = node.id;
            }
        }

        if (nodeId == null) {
            const importedNode = await this.importNode(null, emptyNode);
            nodeId = importedNode.id;
        }

        return nodeId;
    }

    private async clearEmptyNodeForDeletion(node: Node): Promise<void> {
        const constructsRes = await this.client.node.listConstructs(node.id).send();
        const packagesRes = await this.client.devTools.listFromNodes(node.id).send();
        const objPropRes = await this.client.node.listObjectProperties(node.id).send();

        for (const construct of (constructsRes.items || [])) {
            await this.client.node.unassignConstruct(node.id, construct.id).send();
        }

        for (const pkg of (packagesRes.items || [])) {
            await this.client.devTools.unassignFromNode(pkg.name, node.id).send();
        }

        for (const objProp of (objPropRes.items || [])) {
            await this.client.node.unassignObjectProperty(node.id, objProp.id).send();
        }
    }
}

export function createClient(options?: ClientOptions): Promise<GCMSRestClient> {
    const client = new GCMSRestClient(
        new CypressDriver(options?.log ?? false),
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