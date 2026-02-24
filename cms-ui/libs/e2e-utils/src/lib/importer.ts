/* eslint-disable import/order */
/* eslint-disable import/no-nodejs-modules */
/* eslint-disable @typescript-eslint/no-use-before-define */
import {
    ConstructCategory,
    ContentRepository,
    ContentRepositoryType,
    Feature,
    File,
    FileUploadResponse,
    Folder,
    FolderCreateRequest,
    Form,
    FormCreateRequest,
    Group,
    Node,
    NodeFeature,
    Page,
    PageCreateRequest,
    PageSaveRequest,
    PagingSortOrder,
    ResponseCode,
    Schedule,
    ScheduleStatus,
    ScheduleTask,
    Template,
    User,
} from '@gentics/cms-models';
import { GCMSRestClient, GCMSRestClientConfig, GCMSRestClientRequestError, RequestMethod } from '@gentics/cms-rest-client';
import { MeshRestClient } from '@gentics/mesh-rest-client';
import { APIRequestContext } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { basename, join } from 'node:path';
import {
    BinaryMap,
    BufferedFixtureFile,
    ClientOptions,
    CONSTRUCT_CATEGORY_CORE,
    CONSTRUCT_CATEGORY_TESTS,
    ConstructCategoryImportData,
    CORE_CONSTRUCTS,
    CORE_OBJECT_PROPERTIES,
    EntityMap,
    ENV_E2E_CMS_URL,
    FileImportData,
    FixtureFile,
    FolderImportData,
    FormImportData,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_CONSTRUCT_CATEGORY,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_PAGE_TRANSLATION,
    IMPORT_TYPE_SCHEDULE,
    IMPORT_TYPE_TASK,
    IMPORT_TYPE_USER,
    ImportData,
    ImportEntityType,
    ImporterOptions,
    ImportReference,
    ImportType,
    ITEM_TYPE_FILE,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_FORM,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    NodeImportData,
    PageImportData,
    PageTranslationImportData,
    ScheduleImportData,
    ScheduleTaskImportData,
    TestSize,
    UserImportData,
} from './common';
import {
    emptyNode,
    GROUP_ROOT,
    PACKAGE_IMPORTS,
    PACKAGE_MAP,
    SCHEDULE_PUBLISHER,
} from './entities';
import { MeshPlaywrightDriver } from './mesh-playwright-driver';
import { createMeshProxy } from './mesh-proxy';
import { GCMSPlaywrightDriver } from './playwright-driver';
import { getDefaultSystemLogin, wait } from './utils';

const DEFAULT_IMPORTER_OPTIONS: ImporterOptions = {
    logImports: false,
}

const GLOBAL_FEATURES = Object.values(Feature);
const NODE_FEATURES = Object.values(NodeFeature);

/**
 * This Importer imports entities defined in [`./entities.ts`](./entities.ts) into a running CMS
 * instance, by using a dedicated GCMS REST Client.
 * The client uses the [`playwright-driver`](./playwright-driver.ts), as requests are being sent from the
 * playwright `node` process in the background, rather than the page where the e2e tests are executed.
 *
 * All imported or resolved entities are stored in this entity importer and can be accessed for tests.
 *
 * Usage:
 * 1. `before`/`beforeEach`: Cleanup the environment via `cleanupTest`.
 * 2. `before`: Bootstrap the Importer/CMS with `bootstrapSuite`.
 * 3. `beforeEach`: Optional - Prepare fixtures/binaries and put them into the `binaryMap`.
 * 4. `beforeEach`: Optional - Import CMS entities via `setupTest`.
 */
export class EntityImporter {

    /** The client which is used to interact with the REST API. */
    public client: GCMSRestClient | null = null;
    /** Map of imported entities `{ [IMPORT_ID]: Item }` */
    public entityMap: EntityMap = {};
    /**
     * Map of id -> BufferedFixtureFile, which to import.
     * Use the {@link setupBinaryFiles} function to populate this map.
     */
    private binaryMap: BinaryMap = {};
    /** The ID of the dummy-node, if present/checked. */
    public dummyNode: number | null = null;
    /** Mapping of language-code to language-id. */
    public languages: Record<string, number> = {};
    /** Mapping of template global-id to template instance. */
    public templates: Record<string, Template> = {};
    /** Mapping of schedule-task command to task instance. Only contains internal commands. */
    public tasks: Record<string, ScheduleTask> = {};
    /** The top most group that can be accessed from the admin account. */
    public cmsRootGroup: Group;
    /** The root of all test groups, which is imported via the {@link GROUP_ROOT} import data.*/
    public testRootGroup: Group;
    /** If the `bootstrapSuite` has been successfully run through. */
    public bootstrapped = false;

    public apiContext: APIRequestContext;

    constructor(
        public options?: ImporterOptions,
    ) {
        this.options = {
            ...DEFAULT_IMPORTER_OPTIONS,
            ...(this.options || {}),
        };
    }

    /**
     * Imports the provided entities in order and stores all results in the local entity map,
     * which is also returned as result.
     * @param importList The entities to import. Respects the order they are defined.
     * @returns Reference to the local entity-map.
     */
    public async importData(
        importList: ImportData[],
        size: TestSize | null = null,
    ): Promise<EntityMap> {
        await this.setupClient();

        for (const importData of importList) {
            const entity = await this.importEntity(
                size,
                importData[IMPORT_TYPE],
                importData,
            );
            if (!entity) {
                continue;
            }
            // TODO: Save with compound ID (type + id) instead, to avoid potential id collission.
            this.entityMap[importData[IMPORT_ID]] = entity;
        }

        return this.entityMap;
    }

    /**
     * Bootstraps the CMS and this importer with the basic infos for further imports.
     * @param size Which size to import/bootstrap
     */
    public async bootstrapSuite(size: TestSize): Promise<void> {
        await this.setupClient();

        await this.syncTestPackages(size);

        this.templates = await this.getTemplateMapping();
        this.languages = await this.getLanguageMapping();
        this.dummyNode = await this.setupDummyNode();
        // Make sure root groups are present
        if (!this.cmsRootGroup) {
            this.cmsRootGroup = (await this.client.group.list({
                pageSize: 1,
                sort: [{ attribute: 'id', sortOrder: PagingSortOrder.Asc }],
            }).send())?.items?.[0];
        }
        this.testRootGroup = await this.importGroup(GROUP_ROOT);

        // Store all Tasks in the entity map
        const tasks = (await this.client.schedulerTask.list().send()).items || [];
        for (const singleTask of tasks) {
            if (singleTask.internal) {
                this.tasks[singleTask.command] = singleTask;
            }
        }

        // Import default schedules
        await this.importSchedule(SCHEDULE_PUBLISHER);

        this.bootstrapped = true;
    }

    /**
     * Imports all entities from the size package in order, and returns the local entity-map.
     * @param size The size to import content from.
     * @returns The local entity-map reference.
     */
    public async setupTest(size: TestSize): Promise<EntityMap> {
        await this.setupClient();

        const map = await this.setupContent(size);

        // Store all CRs in the entity map
        const crs = (await this.client.contentRepository.list().send()).items || [];
        for (const singleCr of crs) {
            this.entityMap[singleCr.globalId] = singleCr;
        }

        const objectProperties = (await this.client.objectProperty.list().send()).items || [];
        for (const singleObjectProperty of objectProperties) {
            this.entityMap[singleObjectProperty.globalId] = singleObjectProperty;
        }

        return map;
    }

    public async executeSchedule(idOrData: string | number | ScheduleImportData, retryCount: number = 0): Promise<void> {
        let id = idOrData;
        if (typeof id === 'object') {
            id = this.get(id)?.id;
        }

        await this.client.scheduler.execute(id).send();
        let schedule: Schedule;

        for (let i = 0; i < 100; i++) {
            schedule = (await this.client.scheduler.get(id).send()).item;
            if (
                schedule.status === ScheduleStatus.IDLE
                && schedule.lastExecution != null
                && !schedule.lastExecution.running
            ) {
                // If it was successful, then we can safely exit
                if (schedule.lastExecution.result) {
                    break;
                }

                console.warn(`The schedule "${schedule.name}" failed, retries left: ${retryCount}`);

                // Retry limit reached
                if (retryCount <= 0) {
                    throw new Error(`The schedule "${schedule.name}" encountered an error and failed`, {
                        cause: new Error(schedule.lastExecution.log),
                    });
                }

                retryCount--;
            }

            if (this.options?.logImports) {
                console.log(`Waiting for the schedule "${schedule.name}" execution to finish`);
            }
            await wait(1_000);
        }
    }

    /**
     * Clears all CMS Nodes and therefore all CMS entities which can not be restored via
     * devtool imports.
     * @param completeClean If it should also remove the `dummyNode` to completely clear the CMS out.
     */
    public async cleanupTest(completeClean: boolean = false): Promise<void> {
        await this.setupClient();

        // cleanup entities, which were created in tests before
        await this.cleanupEntities();

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

    /** Apply global features to the CMS */
    public async setupFeatures(features: Partial<Record<Feature, boolean>>): Promise<void>;
    /** Apply node features for the nodes in the specified TestSize, and global features */
    public async setupFeatures(
        size: TestSize,
        features: Partial<Record<Feature | NodeFeature, boolean>>,
    ): Promise<void>;
    /** Apply global features; or node and global features with a reference to a TestSize. */
    public async setupFeatures(
        sizeOrGlobalFeatures: TestSize | Partial<Record<Feature, boolean>>,
        features?: Partial<Record<Feature | NodeFeature, boolean>>,
    ): Promise<void> {
        // Safety check
        if (sizeOrGlobalFeatures == null) {
            return;
        }

        await this.setupClient();

        let size: TestSize;
        let nodeIds: number[] = [];

        if (typeof sizeOrGlobalFeatures === 'object') {
            features = sizeOrGlobalFeatures;
        } else {
            size = sizeOrGlobalFeatures;
        }

        // Reset to initial config
        await this.client.admin.reloadConfiguration().send();

        // Get all the required node-ids from the package, if present
        if (size) {
            nodeIds = PACKAGE_MAP[size]
                .filter(data => data[IMPORT_TYPE] === IMPORT_TYPE_NODE)
                .map((data: NodeImportData) => this.getDependency(data)?.id);
        }

        for (const entry of Object.entries(features || {})) {
            const [feature, enabled] = entry;

            // If it's a global feature
            if (GLOBAL_FEATURES.includes(feature as any)) {
                try {
                    // Undocumented, internal, testing endpoint, to dynamically en-/disable features.
                    // Should not be used aside from e2e tests, as these might have weird side-effects.
                    await this.client.executeMappedJsonRequest(RequestMethod.POST, `/admin/feature/${feature}`, null, {
                        enabled,
                    }).send();
                } catch (err) {
                    if (err instanceof GCMSRestClientRequestError
                        && (
                            err.data?.responseInfo?.responseMessage === `Feature #${feature} has been already deactivated`
                            || err.data?.responseInfo?.responseMessage === `Feature #${feature} has been already activated`
                            // In case we want to (make sure) to have a feature deactivated, but it isn't licensed, then we can ignore it
                            || (
                                err.data?.responseInfo?.responseCode === ResponseCode.NOT_LICENSED
                                && !enabled
                            )
                        )
                    ) {
                        return;
                    }
                    throw err;
                }
            }

            // If it's a node specific feature
            if (NODE_FEATURES.includes(feature as any)) {
                for (const id of nodeIds) {
                    if (enabled) {
                        await this.client.node.activateFeature(id, feature as NodeFeature).send();
                    } else {
                        await this.client.node.deactivateFeature(id, feature as NodeFeature).send();
                    }
                }
            }
        }
    }

    public setApiContext(apiContext: APIRequestContext): void {
        this.apiContext = apiContext;

        if (this.client) {
            (this.client.driver as GCMSPlaywrightDriver).context = this.apiContext;
        }
    }

    public clearClient(): Promise<void> {
        this.client = null;
        return Promise.resolve();
    }

    public async syncTag(id: number | string, tagName: string): Promise<void> {
        await this.setupClient();

        await this.client.template.update(id, { forceSync: true, sync: [tagName], syncPages: true }).send();
    }

    public async setupClient(): Promise<void> {
        if (!this.client) {
            this.client = await createClient({
                context: this.apiContext,
                autoLogin: getDefaultSystemLogin(),
            });
        }
    }

    public async syncPackages(size: TestSize): Promise<void> {
        await this.setupClient();

        await this.syncTestPackages(size);
    }

    public async setupBinaryFiles(map: Record<string, FixtureFile>): Promise<void> {
        await Promise.all(Object.entries(map).map(([key, fixture]) => {
            if (this.options?.logImports) {
                console.log(`Importing binary fixture ${fixture.fixturePath}`);
            }
            return readFile(fixture.fixturePath).then(buffer => {
                this.binaryMap[key] = {
                    ...fixture,
                    buffer,
                };
            });
        }));
    }

    private async syncTestPackages(size: TestSize): Promise<void> {
        await this.setupClient();

        // First import all dev-tool packages from the FS
        const devtoolPackages = PACKAGE_IMPORTS[size] || [];
        for (const devtoolPkg of devtoolPackages) {
            await this.client.devTools.syncFromFileSystem(devtoolPkg).send();
        }

        // Just for safety
        if (!this.entityMap) {
            return;
        }
        const constructs = (await this.client.construct.list().send()).items;
        for (const con of constructs) {
            this.entityMap[con.globalId] = con;
        }
    }

    public get<T extends ImportType>(ref: ImportReference<T>): ImportEntityType<T> | null;
    public get(id: string): any;
    /**
     * Gets the resolved/imported entity based on the Import-ID.
     * Overloads are to get the correct CMS item type based on the import-data type.
     */
    public get(data: ImportData | string): any {
        // TODO: Load with compound ID (type + id) instead, to avoid potential id collission.
        const id = typeof data === 'object' ? data[IMPORT_ID] : data;
        return this.entityMap[id];
    }

    private getDependency<T extends ImportType>(ref: ImportReference<T>, optional?: boolean): ImportEntityType<T>;
    private getDependency<T extends ImportType>(ref: ImportReference<T>, optional: true): ImportEntityType<T> | null;
    private getDependency<T extends ImportType>(type: T, id: string, optional?: boolean): ImportEntityType<T>;
    private getDependency<T extends ImportType>(type: T, id: string, optional: true): ImportEntityType<T> | null;
    private getDependency<T extends ImportType>(
        typeOrRef: T | ImportReference<T>,
        idOrOptional?: string | boolean,
        optional: boolean = false,
    ): ImportEntityType<T> {
        let ref: ImportReference<T>;

        if (typeof typeOrRef === 'object') {
            ref = typeOrRef;
            optional = !!idOrOptional;
        } else {
            ref = {
                [IMPORT_TYPE]: typeOrRef,
                [IMPORT_ID]: idOrOptional as string,
            };
        }

        const item = this.get(ref);

        if (item == null && !optional) {
            const msg = `Missing item depdency ${ref[IMPORT_TYPE]}:${ref[IMPORT_ID]}!`;
            console.error(msg);
            throw new Error(msg);
        }

        return item;
    }

    private getBinaryDependency(id: string, optiona?: boolean): BufferedFixtureFile;
    private getBinaryDependency(id: string, optional: true): BufferedFixtureFile | null {
        const bin = this.binaryMap[id];

        if (bin) {
            return bin;
        }

        const msg = `Missing binary dependency ${id}!`;
        if (optional) {
            if (this.options?.logImports) {
                console.warn(msg);
            }
            return null;
        }

        console.error(msg);
        throw new Error(msg);
    }

    private getParentEntity(id: string, optional?: boolean): Node | Folder;
    private getParentEntity(id: string, optional: true): Node | Folder | null {
        const node = this.getDependency(IMPORT_TYPE_NODE, id, true);
        if (node != null) {
            return node;
        }
        return this.getDependency(ITEM_TYPE_FOLDER, id, optional);
    }

    private getParentId(parent: Node | Folder): number {
        return (parent as Node)?.folderId ?? parent.id;
    }

    private getScheduleTaskDependency(id: string): ScheduleTask {
        const internal = this.tasks[id];
        if (internal) {
            return internal;
        }

        return this.getDependency(IMPORT_TYPE_TASK, id);
    }

    private async importNode(
        pkgName: TestSize | null,
        data: NodeImportData,
    ): Promise<Node> {
        const {
            languages,
            templates,
            ...reqData
        } = data;

        const {masterId, ...req} = reqData.node;

        let actualMasterId = 0;
        if (masterId) {
            const master = this.getDependency(IMPORT_TYPE_NODE, masterId);
            actualMasterId = master.folderId;
        }

        const created = (await this.client.node.create({
            ...reqData,
            node: {
                ...req,
                masterId: actualMasterId,
            },
        }).send()).node;

        // Assign all the languages it has defined in the import data
        for (const lang of languages) {
            await this.client.node.assignLanguage(created.id, this.languages[lang]).send();
        }

        // Assign all core constructs, so that implementations properly work
        for (const construct of CORE_CONSTRUCTS) {
            await this.client.node.assignConstruct(created.id, construct).send();
        }

        // Assigns all Dev-Tool package elements to the node
        let packages: string[];
        if (pkgName) {
            packages = PACKAGE_IMPORTS[pkgName];
        } else {
            // If no package is provided, it'll aggregate all of them and assign those
            packages = Array.from(new Set(Object.values(PACKAGE_IMPORTS).flatMap(v => v)));
        }

        if (created.type !== 'channel') {
            for (const pkg of packages) {
                await this.client.devTools.assignToNode(pkg, created.id).send();
            }
        }

        if (Array.isArray(templates)) {
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
                        console.log(`Loaded node template ${tplId} -> ${tpl.id}`);
                    }
                    this.entityMap[tplId] = tpl;
                }
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

        const node = this.getDependency(IMPORT_TYPE_NODE, nodeId);
        const parentEntity = this.getParentEntity(motherId);
        const parentId = this.getParentId(parentEntity);

        const body: FolderCreateRequest = {
            ...req,

            motherId: parentId,
            nodeId: node.id,
        };

        const created = (await this.client.folder.create(body).send()).folder;
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

        const node = this.getDependency(IMPORT_TYPE_NODE, nodeId);
        const parentEntity = this.getParentEntity(folderId);
        const parentId = this.getParentId(parentEntity);

        const tplId = (this.entityMap[templateId] as Template).id;
        const body: PageCreateRequest = {
            ...req,

            folderId: parentId,
            nodeId: node.id,
            templateId: tplId,
        };

        let created = (await this.client.page.create(body).send()).page;

        const updateBody: PageSaveRequest = {
            unlock: true,
            page: {},
        };

        if (tags) {
            updateBody.page = {
                tags,
            };
        }

        await this.client.page.update(created.id, updateBody).send();

        // Reload the page data just to be sure
        created = (await this.client.page.get(created.id).send()).page;

        return created;
    }

    private async uploadFixtureFile(
        fixtureFile: BufferedFixtureFile,
        folderId: number | string,
        nodeId: number | string,
    ): Promise<File> {
        const fileName = fixtureFile.name ?? basename(fixtureFile.fixturePath);
        // URL has to be hardcoded like this.
        // CMS is actually only reachable under localhost in this case, no idea why.
        const res = await this.apiContext.post('http://localhost:8080/rest/file/create', {
            multipart: {
                fileBinaryData: {
                    name: fileName,
                    mimeType: fixtureFile.type,
                    buffer: fixtureFile.buffer,
                },
                fileName: fileName,
                folderId: folderId.toString(),
                nodeId: nodeId.toString(),
            },
            params: {
                sid: this.client.sid.toString(),
            },
        });
        const created = (await res.json() as FileUploadResponse).file;
        return created;
    }

    private async importFile(
        data: FileImportData,
    ): Promise<File | false | null> {
        const { folderId, nodeId, ...updateData } = data;

        const node = this.getDependency(IMPORT_TYPE_NODE, nodeId);
        const fixtureFile = this.getBinaryDependency(data[IMPORT_ID], true);
        const parentEntity = this.getParentEntity(folderId);
        const parentId = this.getParentId(parentEntity);

        if (!fixtureFile) {
            return false;
        }

        const created = await this.uploadFixtureFile(fixtureFile, parentId, node.id);
        await this.client.file.update(created.id, { file: updateData }).send();

        return created;
    }

    private async importImage(
        data: ImageImportData,
    ): Promise<File | false | null> {
        const { folderId, nodeId, ...updateData } = data;

        const node = this.getDependency(IMPORT_TYPE_NODE, nodeId);
        const fixtureFile = this.getBinaryDependency(data[IMPORT_ID], true);
        const parentEntity = this.getParentEntity(folderId);
        const parentId = this.getParentId(parentEntity);

        if (!fixtureFile) {
            return false;
        }

        const created = await this.uploadFixtureFile(fixtureFile, parentId, node.id);
        await this.client.image.update(created.id, { image: updateData }).send();

        return created;
    }

    private async importForm(
        data: FormImportData,
    ): Promise<Form | null> {
        const { nodeId, folderId, ...reqData } = data;

        const node = this.getDependency(IMPORT_TYPE_NODE, nodeId);
        const parentEntity = this.getParentEntity(folderId);
        const parentId = this.getParentId(parentEntity);

        const body: FormCreateRequest = {
            ...reqData,

            folderId: parentId,
            nodeId: node.id,
        };

        const created = (await this.client.form.create(body).send()).item;
        await this.client.form.unlock(created.id).send();
        return created;
    }

    private async importGroup(
        data: GroupImportData,
    ): Promise<Group | null> {
        const { parent, permissions, ...reqData } = data;

        let parentGroup: Group | null = null;
        let importedGroup: Group;

        if (parent === null) {
            parentGroup = this.cmsRootGroup;
        } else if (parent != null) {
            parentGroup = this.getDependency(parent, true);
        } else {
            parentGroup = this.testRootGroup;
        }

        try {
            importedGroup = (await this.client.group.create(parentGroup.id, reqData).send()).group;
        } catch (err) {
            // If a non-recoverable error occurs
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`${data[IMPORT_TYPE]}:${data[IMPORT_ID]} already exists`);
            }
            const foundGroups = (await this.client.group.list({ q: reqData.name }).send()).items || [];
            importedGroup = foundGroups.find(group => group.name === reqData.name);
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
        const { group, extraGroups, ...reqData } = data;

        let groupEntity: Group;
        let user: User;

        if (group === null) {
            groupEntity = this.cmsRootGroup;
        } else if (!group) {
            groupEntity = this.testRootGroup;
        } else {
            groupEntity = this.getDependency(group);
        }

        try {
            user = (await this.client.group.createUser(groupEntity.id, reqData).send()).user;
        } catch (err) {
            // If the user already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`${data[IMPORT_TYPE]}:${data[IMPORT_ID]} already exists`);
            }
            const foundUsers = (await this.client.user.list({ q: data.login }).send()).items || [];
            user = foundUsers.find(user => user.login === data.login);
        }

        if (extraGroups) {
            for (const extraGroupId of extraGroups) {
                const extraGroup = this.getDependency(extraGroupId);
                await this.client.user.assignToGroup(user.id, extraGroup.id).send();
            }
        }

        return user;
    }

    private async importTask(
        data: ScheduleTaskImportData,
    ): Promise<ScheduleTask | null> {
        const created = (await this.client.schedulerTask.create(data).send()).item;
        return created;
    }

    private async importSchedule(
        data: ScheduleImportData,
    ): Promise<Schedule | null> {
        const { taskId, ...reqData } = data;

        const task = this.getScheduleTaskDependency(taskId);

        try {
            const created = (await this.client.scheduler.create({
                ...reqData,
                taskId: task.id,
            }).send())?.item;

            return created;
        } catch (err) {
            // If the schedule already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`${data[IMPORT_TYPE]}:${data[IMPORT_ID]} already exists`);
            }

            const foundSchedules = (await this.client.scheduler.list().send()).items || [];
            const found = foundSchedules.find(schedule => schedule.name === data.name);

            return found;
        }
    }

    private async importConstructCategory(data: ConstructCategoryImportData): Promise<ConstructCategory | null> {
        const created = (await this.client.constructCategory.create(data).send()).constructCategory;
        return created;
    }

    private async importPageTranslation(data: PageTranslationImportData): Promise<Page | null> {
        const { pageId, language, ...req } = data;
        const page = this.getDependency(ITEM_TYPE_PAGE, pageId);

        let created = (await this.client.page.translate(page.id, {
            language,
        }).send()).page;

        await this.client.page.update(created.id, {
            page: {
                language,
                ...req,
            },
            unlock: true,
        }).send();

        // Reload the page as it has the tags now
        created = (await this.client.page.get(created.id).send()).page;

        return created;
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

    private async importEntity(
        pkgName: TestSize,
        type: string,
        entity: ImportData,
    ): Promise<any> {
        if (this.options?.logImports) {
            console.log(`Importing ${entity[IMPORT_TYPE]}:${entity[IMPORT_ID]} ...`);
        }

        let imported: any;

        switch (type) {
            case IMPORT_TYPE_NODE:
                imported = await this.importNode(pkgName, entity as NodeImportData);
                break;

            case ITEM_TYPE_FOLDER:
                imported = await this.importFolder(entity as FolderImportData);
                break;

            case ITEM_TYPE_PAGE:
                imported = await this.importPage(entity as PageImportData);
                break;

            case IMPORT_TYPE_PAGE_TRANSLATION:
                imported = await this.importPageTranslation(entity as PageTranslationImportData);
                break;

            case ITEM_TYPE_FILE:
                imported = await this.importFile(entity as FileImportData);
                break;

            case ITEM_TYPE_IMAGE:
                imported = await this.importImage(entity as ImageImportData);
                break;

            case ITEM_TYPE_FORM:
                imported = await this.importForm(entity as FormImportData);
                break;

            case IMPORT_TYPE_GROUP:
                imported = await this.importGroup(entity as GroupImportData);
                break;

            case IMPORT_TYPE_USER:
                imported = await this.importUser(entity as UserImportData);
                break;

            case IMPORT_TYPE_TASK:
                imported = await this.importTask(entity as ScheduleTaskImportData);
                break;

            case IMPORT_TYPE_SCHEDULE:
                imported = await this.importSchedule(entity as ScheduleImportData);
                break;

            case IMPORT_TYPE_CONSTRUCT_CATEGORY:
                imported = await this.importConstructCategory(entity as ConstructCategoryImportData);
                break;

            default:
                if (this.options?.logImports) {
                    console.log(`Could not find a importer for import type ${entity[IMPORT_TYPE]}!`);
                }
                return Promise.resolve(null);
        }

        if (this.options?.logImports) {
            if (imported != null && typeof imported === 'object') {
                console.log(`Imported ${entity[IMPORT_TYPE]}:${entity[IMPORT_ID]} -> ${imported.id}`);
            } else if (imported === false) {
                console.log(`Skipped import of ${entity[IMPORT_TYPE]}:${entity[IMPORT_ID]}`);
            } else {
                console.log(`Imported ${entity[IMPORT_TYPE]}:${entity[IMPORT_ID]}`);
            }
        }

        return imported;
    }

    private async cleanupEntities(): Promise<void> {
        await this.cleanupScheduleTasks();
        await this.cleanupSchedules();
        await this.cleanupContentRepositories();
        await this.cleanupConstructCategories();
        await this.cleanupObjectProperties();
        await this.cleanupPackages();
        await this.cleanupUsers();
        await this.cleanupGroups();
    }

     private async cleanupObjectProperties(): Promise<void> {
        const objectProperties = (await this.client.objectProperty.list().send()).items || [];
        for (const op of objectProperties) {
            if (CORE_OBJECT_PROPERTIES.includes(op.globalId)) {
                continue;
            }

            await this.client.objectProperty.delete(op.id).send();
        }
    }

    private async cleanupScheduleTasks(): Promise<void> {
        const tasks = (await this.client.schedulerTask.list().send()).items;
        for (const task of tasks) {
            // we remove the non-internal tasks
            if (task.internal) {
                continue;
            }
            await this.client.schedulerTask.delete(task.id).send();
        }
    }

    private async cleanupSchedules(): Promise<void> {
        const schedules = (await this.client.scheduler.list().send()).items;
        for (const schedule of schedules) {
            await this.client.scheduler.delete(schedule.id).send();
        }
    }

    private async cleanupContentRepositories(): Promise<void> {
        const crs = (await this.client.contentRepository.list().send()).items;
        for (const cr of crs) {
            await this.cleanupMeshData(cr);
            await this.client.contentRepository.delete(cr.id).send();
        }
    }

    public async createMeshClient(cr: ContentRepository): Promise<MeshRestClient> {
        const login = await this.client.contentRepository.proxyLogin(cr.id).send();

        const mesh: MeshRestClient = createMeshProxy(this.client, cr.id);
        mesh.driver = new MeshPlaywrightDriver(this.apiContext);
        mesh.apiKey = login.token;

        return mesh;
    }

    private async cleanupMeshData(cr: ContentRepository): Promise<void> {
        if (cr.crType !== ContentRepositoryType.MESH) {
            return;
        }
        const mesh = await this.createMeshClient(cr);

        const projects = (await mesh.projects.list().send()).data;
        for (const project of projects) {
            await mesh.projects.delete(project.uuid).send();
        }

        const micros = (await mesh.microschemas.list().send()).data;
        for (const micro of micros) {
            await mesh.microschemas.delete(micro.uuid).send();
        }

        const schemas = (await mesh.schemas.list().send()).data;
        for (const schema of schemas) {
            await mesh.schemas.delete(schema.uuid).send();
        }
    }

    private async cleanupConstructCategories(): Promise<void> {
        const categories = (await this.client.constructCategory.list().send()).items;
        for (const cat of categories) {
            // Don't delete the essential categories
            if (
                cat.globalId === CONSTRUCT_CATEGORY_CORE
                || cat.globalId === CONSTRUCT_CATEGORY_TESTS
            ) {
                continue;
            }
            await this.client.constructCategory.delete(cat.id).send();
        }
    }

    private async cleanupUsers(): Promise<void> {
        const users = (await this.client.user.list().send()).items;
        for (const user of users) {
            // Can't delete reserved/default users
            if (user.login === 'node' || user.login === 'gentics') {
                continue;
            }
            await this.client.user.delete(user.id).send();
        }
    }

    private async cleanupGroups(): Promise<void> {
        const groups = (await this.client.group.list().send()).items;
        for (const group of groups) {
            // Can't delete the root groups
            if (group.id <= 2) {
                continue;
            }
            try {
                await this.client.group.delete(group.id).send();
            } catch (err) {
                // Ignore deletion notices of groups which have already been deleted
                if (err instanceof GCMSRestClientRequestError && err.responseCode === 404) {
                    continue;
                }
                throw err;
            }
        }
    }

    private async setupContent(
        pkgName: TestSize,
    ): Promise<EntityMap> {
        const importList = PACKAGE_MAP[pkgName];
        if (!importList) {
            return {};
        }

        // Make sure root group is present
        this.testRootGroup = await this.importGroup(GROUP_ROOT);

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
            // TODO: Save with compound ID (type + id) instead, to avoid potential id collission.
            this.entityMap[importData[IMPORT_ID]] = entity;
        }

        return this.entityMap;
    }

    private async cleanupPackages(): Promise<void> {
        const packages = (await this.client.devTools.list().send()).items;
        const defaults = Object.values(TestSize);

        for (const pkg of packages) {
            if (!defaults.includes(pkg.name as TestSize)) {
                await this.client.devTools.delete(pkg.name).send();
            }
        }
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
        if (node.type === 'channel') {
            return;
        }

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

export async function createClient(options: ClientOptions): Promise<GCMSRestClient> {
    if (options.connection == null) {
        const baseUrl = new URL(process.env[ENV_E2E_CMS_URL]);

        options.connection = {
            absolute: true,
            ssl: baseUrl.protocol === 'https:',
            host:options.isPageContext ? baseUrl.hostname : 'localhost',
            port: parseInt(baseUrl.port, 10),
            basePath: join(baseUrl.pathname, '/rest').replaceAll('\\', '/'),
        };
    }

    // The baseUrl (aka. protocol/host/port) has to be already setup when started
    const client = new GCMSRestClient(
        new GCMSPlaywrightDriver(options.context),
        options as GCMSRestClientConfig,
    );

    if (!options?.autoLogin) {
        return client;
    }

    try {
        const res = await client.auth.login({
            login: options.autoLogin.username,
            password: options.autoLogin.password,
        }).send();
        // Set the SID for future requests
        client.sid = res.sid;
        return client;
    } catch (err) {
        return client;
    }
}

