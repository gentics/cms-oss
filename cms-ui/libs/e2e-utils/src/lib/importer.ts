/* eslint-disable @typescript-eslint/no-use-before-define */
import {
    ConstructCategory,
    Feature,
    File,
    FileUploadOptions,
    Folder,
    FolderCreateRequest,
    Form,
    FormCreateRequest,
    Group,
    Image,
    Node,
    NodeFeature,
    Page,
    PageCreateRequest,
    PagingSortOrder,
    ResponseCode,
    Schedule,
    ScheduleTask,
    Template,
    User,
} from '@gentics/cms-models';
import { GCMSRestClient, GCMSRestClientConfig, GCMSRestClientRequestError, RequestMethod } from '@gentics/cms-rest-client';
import { APIRequestContext } from '@playwright/test';
import {
    BinaryMap,
    CONSTRUCT_CATEGORY_CORE,
    CONSTRUCT_CATEGORY_TESTS,
    ConstructCategoryImportData,
    CORE_CONSTRUCTS,
    EntityMap,
    FileImportData,
    FolderImportData,
    FormImportData,
    GroupImportData,
    ImageImportData,
    IMPORT_ID,
    IMPORT_TYPE,
    IMPORT_TYPE_CONSTRUCT_CATEGORY,
    IMPORT_TYPE_GROUP,
    IMPORT_TYPE_NODE,
    IMPORT_TYPE_SCHEDULE,
    IMPORT_TYPE_TASK,
    IMPORT_TYPE_USER,
    ImportData,
    ITEM_TYPE_FILE,
    ITEM_TYPE_FOLDER,
    ITEM_TYPE_FORM,
    ITEM_TYPE_IMAGE,
    ITEM_TYPE_PAGE,
    LoginInformation,
    NodeImportData,
    PageImportData,
    ScheduleImportData,
    ScheduleTaskImportData,
    TestSize,
    UserImportData,
} from './common';
import {
    emptyNode,
    PACKAGE_IMPORTS,
    PACKAGE_MAP,
    schedulePublisher,
} from './entities';
import { GCMSPlaywrightDriver } from './playwright-driver';
import { getDefaultSystemLogin, getItem } from './utils';

/**
 * Options to configure the behaviour of the importer
 */
export interface ImporterOptions {
    /**
     * If it sohuld log all requests from the `CypressDriver`.
     * @see ClientOptions.log
     */
    logRequests?: boolean;
    /**
     * If it should log out when a entity is getting imported and what the result of it is.
     * Useful for debugging the test-data import, as usually error messages/stack traces
     * are quite bad from cypress.
     */
    logImports?: boolean;
}

export interface ClientOptions extends Partial<GCMSRestClientConfig> {
    /**
     * If it sohuld log all requests from the `CypressDriver`.
     * Passes it along to `cy.request` -> `{ log: options.logRequests }`
     * @see https://docs.cypress.io/api/commands/request#Arguments
     */
    log?: boolean;
    context: APIRequestContext;
    autoLogin?: LoginInformation;
}

const DEFAULT_IMPORTER_OPTIONS: ImporterOptions = {
    logRequests: false,
    logImports: false,
}

const GLOBAL_FEATURES = Object.values(Feature);
const NODE_FEATURES = Object.values(NodeFeature);

/**
 * This Importer imports entities defined in [`./entities.ts`](./entities.ts) into a running CMS
 * instance, by using a dedicated GCMS REST Client.
 * The client uses the [`cypress-driver`](./cypress-driver.ts), as requests are being sent from the
 * cypress `node` process in the background, rather than the page where the e2e tests are executed.
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
     * Map of binaries which should be applied when importing a file/image.
     * If not provided, the file/image will not be imported/skipped.
     * `{ [IMPORT_ID]: File }`
     */
    public binaryMap: BinaryMap = {};
    /** The ID of the dummy-node, if present/checked. */
    public dummyNode: number | null = null;
    /** Mapping of language-code to language-id. */
    public languages: Record<string, number> = {};
    /** Mapping of template global-id to template instance. */
    public templates: Record<string, Template> = {};
    /** Mapping of schedule-task command to task instance. Only contains internal commands. */
    public tasks: Record<string, ScheduleTask> = {};
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

        // Store all Tasks in the entity map
        const tasks = (await this.client.schedulerTask.list().send()).items || [];
        for (const singleTask of tasks) {
            if (singleTask.internal) {
                this.tasks[singleTask.command] = singleTask;
            }
        }

        // Import default schedules
        await this.importSchedule(schedulePublisher);

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

        return map;
    }

    public async executeSchedule(idOrData: string | number | ScheduleImportData): Promise<void> {
        let id = idOrData;
        if (typeof id === 'object') {
            id = this.get(id)?.id;
        }

        await this.client.scheduler.execute(id).send();
        let schedule: Schedule;

        for (let i = 0; i < 100; i++) {
            schedule = (await this.client.scheduler.get(id).send()).item;
            if (!schedule.lastExecution?.running) {
                break;
            }

            if (this.options?.logImports) {
                console.log(`Waiting for the schedule "${schedule.name}" execution to finish`);
            }
            await new Promise(resolve => setTimeout(resolve, 1_000));
        }
    }

    /**
     * Delete all mesh projects for all contentrepositories
     */
    public async deleteMeshProjects(): Promise<void> {
        await this.setupClient();

        const crListResponse = await this.client.contentRepository.list().send();
        for (const cr of crListResponse.items) {
            await this.client.contentRepository.proxyLogin(cr.id).send();
            const projectsList = await this.client.executeMappedJsonRequest(RequestMethod.GET, `/contentrepositories/${cr.id}/proxy/api/v2/projects`).send();
            for (const project of projectsList.data) {
                await this.client.executeMappedJsonRequest(RequestMethod.DELETE, `/contentrepositories/${cr.id}/proxy/api/v2/projects/${project.uuid}`).send();
            }
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
                .map(data => data[IMPORT_TYPE] === IMPORT_TYPE_NODE ? (this.get(data as NodeImportData))?.id : null)
                .filter(id => id != null);
        }

        for (const entry of Object.entries(features || {})) {
            const [feature, enabled] = entry;

            // If it's a global feature
            if (GLOBAL_FEATURES.includes(feature as any)) {
                // Cypress.log({
                //     type: 'parent',
                //     name: `${enabled ? 'enable' : 'disable'} global feature`,
                //     message: feature,
                // });

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
                    // Cypress.log({
                    //     type: 'parent',
                    //     name: `${enabled ? 'enable' : 'disable'} feature`,
                    //     message: `${feature} on ${id}`,
                    // });

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
                log: this.options?.logRequests,
                context: this.apiContext,
                autoLogin: getDefaultSystemLogin(),
            });
        }
    }

    public async syncPackages(size: TestSize): Promise<void> {
        await this.setupClient();

        await this.syncTestPackages(size);
    }

    private async syncTestPackages(size: TestSize): Promise<void> {
        await this.setupClient();

        // First import all dev-tool packages from the FS
        const devtoolPackages = PACKAGE_IMPORTS[size] || [];
        for (const devtoolPkg of devtoolPackages) {
            await this.client.devTools.syncFromFileSystem(devtoolPkg).send();
        }
    }

    public get(data: NodeImportData): Node | null;
    public get(data: FolderImportData): Folder | null;
    public get(data: PageImportData): Page | null;
    public get(data: ImageImportData): Image | null;
    public get(data: FileImportData): File | null;
    public get(data: FormImportData): Form | null;
    public get(data: GroupImportData): Group | null;
    public get(data: UserImportData): User | null;
    public get(data: ScheduleTaskImportData): ScheduleTask | null;
    public get(data: ScheduleImportData): Schedule | null;
    public get(data: ConstructCategoryImportData): ConstructCategory | null;
    public get(id: string): any;
    /**
     * Gets the resolved/imported entity based on the Import-ID.
     * Overloads are to get the correct CMS item type based on the import-data type.
     */
    public get(data: ImportData | string): any {
        return getItem(data as any, this.entityMap);
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

        if (this.options?.logImports) {
            console.log(`Importing node ${data[IMPORT_ID]}`, req);
        }
        const created = (await this.client.node.create({
            ...reqData,
            node: {
                ...req,
                masterId: masterId ?  (this.entityMap[masterId] as Node).folderId : 0,
            }
        }).send()).node;
        if (this.options?.logImports) {
            console.log(`Imported node ${data[IMPORT_ID]} -> ${created.id} (${created.folderId})`);
        }

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
            console.log(`Importing folder ${data[IMPORT_ID]}`, body);
        }
        const created = (await this.client.folder.create(body).send()).folder;
        if (this.options?.logImports) {
            console.log(`Imported folder ${data[IMPORT_ID]} -> ${created.id}`);
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
            translations,
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
            console.log(`Importing page ${data[IMPORT_ID]}`, body);
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
            console.log(`Imported page ${data[IMPORT_ID]} -> ${created.id}`);
        }
        const languages = translations || []
        for (const language of languages) {
            await this.client.page.translate(created.id, {
                language: language,
            }).send();
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
                console.log(`No binary for ${data[IMPORT_ID]} defined!`);
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
            console.log(`Importing file ${data[IMPORT_ID]}`, body);
        }
        const created = (await this.client.file.upload(new Blob([bin]), body).send())?.file;
        if (this.options?.logImports) {
            console.log(`Imported file ${data[IMPORT_ID]} ->`, created);
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
                console.log(`No binary for ${data[IMPORT_ID]} defined!`);
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
            console.log(`Importing image ${data[IMPORT_ID]}`, data);
        }
        const created = (await this.client.file.upload(new Blob([bin]), body).send()).file;
        if (this.options?.logImports) {
            console.log(`Imported image ${data[IMPORT_ID]} -> ${created.id}`);
        }

        await this.client.image.update(created.id, { image: updateData }).send();

        return created;
    }

    private async importForm(
        data: FormImportData,
    ): Promise<Form | null> {
        const { nodeId, folderId, ...reqData } = data;

        const parentEntity = this.entityMap[folderId];
        const parentId = (parentEntity as Node).folderId ?? (parentEntity as (Node | Folder)).id;
        const body: FormCreateRequest = {
            ...reqData,

            folderId: parentId,
            nodeId: (this.entityMap[nodeId] as Node).id,
        };

        if (this.options?.logImports) {
            console.log(`Importing form ${data[IMPORT_ID]}`, data);
        }
        const created = (await this.client.form.create(body).send()).item;
        if (this.options?.logImports) {
            console.log(`Imported form ${data[IMPORT_ID]} -> ${created.id}`);
        }

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
                console.log(`Importing group ${data[IMPORT_ID]}`, data);
            }
            importedGroup = (await this.client.group.create(parentId, reqData).send()).group;
            if (this.options?.logImports) {
                console.log(`Imported group ${data[IMPORT_ID]} -> ${importedGroup.id}`);
            }
        } catch (err) {
            // If the group already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`Group ${data[IMPORT_ID]} already exists`);
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
        const { group, ...reqData } = data;

        try {
            if (this.options?.logImports) {
                console.log(`Importing user ${data[IMPORT_ID]}`, data);
            }
            const created = (await this.client.group.createUser((this.entityMap[group] as Group).id, reqData).send()).user;
            if (this.options?.logImports) {
                console.log(`Imported user ${data[IMPORT_ID]} -> ${created.id}`);
            }

            return created;
        } catch (err) {
            // If the user already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`User ${data[IMPORT_ID]} already exists`);
            }
            const foundUsers = (await this.client.user.list({ q: data.login }).send()).items || [];
            const found = foundUsers.find(user => user.login === data.login);

            return found;
        }
    }

    private async importTask(
        data: ScheduleTaskImportData,
    ): Promise<ScheduleTask | null> {
        const { ...reqData } = data;

        if (this.options?.logImports) {
            console.log(`Importing scheduler task ${data[IMPORT_ID]}`, data);
        }
        const created = (await this.client.schedulerTask.create(reqData).send()).item;
        if (this.options?.logImports) {
            console.log(`Imported scheduler task ${data[IMPORT_ID]} -> ${created.id}`);
        }

        return created;
    }

    private async importSchedule(
        data: ScheduleImportData,
    ): Promise<Schedule | null> {
        const { task, ...reqData } = data;

        try {
            if (this.options?.logImports) {
                console.log(`Importing schedule ${data[IMPORT_ID]}`, data);
            }

            const taskId = this.tasks[task]?.id;
            const created = (await this.client.scheduler.create({
                ...reqData,
                taskId,
            }).send())?.item;

            if (this.options?.logImports) {
                console.log(`Imported schedule ${data[IMPORT_ID]} -> ${created.id}`);
            }

            return created;
        } catch (err) {
            // If the schedule already exists, ignore it
            if (!(err instanceof GCMSRestClientRequestError && err.responseCode === 409)) {
                throw err;
            }

            if (this.options?.logImports) {
                console.log(`Schedule ${data[IMPORT_ID]} already exists`);
            }

            const foundSchedules = (await this.client.scheduler.list().send()).items || [];
            const found = foundSchedules.find(schedule => schedule.name === data.name);

            return found;
        }
    }

    private async importConstructCategory(data: ConstructCategoryImportData): Promise<ConstructCategory | null> {
        if (this.options?.logImports) {
            console.log(`Importing construct-category task ${data[IMPORT_ID]}`, data);
        }
        const created = (await this.client.constructCategory.create(data).send()).constructCategory;
        if (this.options?.logImports) {
            console.log(`Imported construct-category task ${data[IMPORT_ID]} -> ${created.id}`);
        }

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

            case ITEM_TYPE_FORM:
                return this.importForm(entity as FormImportData);

            case IMPORT_TYPE_GROUP:
                return this.importGroup(entity as GroupImportData);

            case IMPORT_TYPE_USER:
                return this.importUser(entity as UserImportData);

            case IMPORT_TYPE_TASK:
                return this.importTask(entity as ScheduleTaskImportData);

            case IMPORT_TYPE_SCHEDULE:
                return this.importSchedule(entity as ScheduleImportData);

            case IMPORT_TYPE_CONSTRUCT_CATEGORY:
                return this.importConstructCategory(entity as ConstructCategoryImportData);

            default:
                return Promise.resolve(null);
        }
    }

    private async cleanupEntities(): Promise<void> {
        await this.cleanupScheduleTasks();
        await this.cleanupSchedules();
        await this.cleanupContentRepositories();
        await this.cleanupConstructCategories();
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
            await this.client.contentRepository.delete(cr.id).send();
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
        options.connection = {
            absolute: true,
            ssl: false,
            host: 'localhost',
            port: 8080,
            basePath: '/rest',
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
