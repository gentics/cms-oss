import {
    ContentRepositoryBO,
    EditableEntity,
    FALLBACK_LANGUAGE,
    ROUTE_DATA_MESH_REPO_ITEM,
    ROUTE_MESH_BRANCH_ID,
    ROUTE_MESH_LANGUAGE,
    ROUTE_MESH_PARENT_NODE_ID,
    ROUTE_MESH_PROJECT_ID,
} from '@admin-ui/common';
import { getUserDisplayName } from '@admin-ui/mesh';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { BranchReference, ProjectResponse, User } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { ChangesOf } from '@gentics/ui-core';
import { ResolvedParentNode } from '../../models/mesh-browser-models';
import { ListRouteParameters, MeshBrowserLoaderService, MeshBrowserNavigatorService } from '../../providers';

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshBrowserMasterComponent
    extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> implements OnChanges {

    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;

    @Input({ alias: ROUTE_MESH_PROJECT_ID })
    public project: string;

    @Input({ alias: ROUTE_MESH_BRANCH_ID })
    public branch: string;

    @Input({ alias: ROUTE_MESH_PARENT_NODE_ID })
    public node: string;

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public language = FALLBACK_LANGUAGE;

    // User Info
    public me: User;
    public meName: string;
    public loggedIn = false;

    // Loaded root elements which are only loaded once on start/login
    public loadedProjects: ProjectResponse[] | null = null;
    public availableProjects: Array<string> = [];
    public selectedRepository: ContentRepositoryBO;

    // Info from current Project
    /** The currently loaded project. Used to determine if the loaded state is in sync. */
    public resolvedProject: ProjectResponse | null = null;
    public projectBranches: Array<BranchReference> = [];
    public projectLanguages: Array<string> = [];
    public projectSchemas: string[] = [];

    // Loading error states
    /** If no root-node could be resolved. Usually due to permissions */
    public noRootNode = false;

    // Additionally loaded elements
    public loadedBranch: BranchReference;
    public loadedNode: ResolvedParentNode;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected mesh: MeshRestClientService,
        protected loader: MeshBrowserLoaderService,
        protected navigatorService: MeshBrowserNavigatorService,
    ) {
        super(changeDetector, router, route, appState);
    }

    ngOnChanges(changes: ChangesOf<this>): void {
        const loadedRepository = this.route.snapshot.data[ROUTE_DATA_MESH_REPO_ITEM];
        if (loadedRepository != null) {
            this.selectedRepository = loadedRepository;
        }

        if (this.loggedIn) {
            if (changes.project && this.project !== this.resolvedProject?.name) {
                this.handleProjectChange();
            } else if (changes.branch) {
                this.handleBranchChange();
            } else if (changes.node || changes.language) {
                this.handleElementChange();
            }
        }
    }

    private handleNavigation(params?: Partial<ListRouteParameters>, cr?: ContentRepositoryBO): void {
        params = {
            crId: this.selectedRepository?.id,
            project: this.project,
            branch: this.branch,
            node: this.node,
            language: this.language,
            ...(params || {}),
        };

        this.navigatorService.handleNavigation(
            this.route,
            params as ListRouteParameters,
            cr !== undefined ? cr : this.selectedRepository,
        );
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.node = undefined;
        this.project = undefined;
        this.navigatorService.navigateToModuleRoot(this.route);
    }

    public meshLoginHandler(event: { loggedIn: boolean; user?: User }): void {
        this.loggedIn = event.loggedIn;

        if (!event.loggedIn) {
            return;
        }

        if (event.user) {
            this.me = event.user;
            this.meName = getUserDisplayName(this.me);
        } else {
            this.mesh.auth.me().send().then((res) => {
                this.me = res;
                this.meName = getUserDisplayName(this.me);
                this.changeDetector.markForCheck();
            });
        }

        if (!this.project || this.project !== this.resolvedProject?.name) {
            this.handleProjectChange();
        }
    }

    protected async handleProjectChange(): Promise<void> {
        if (!this.loggedIn) {
            console.error('Mesh client is unauthenticated');
            return;
        }

        if (this.loadedProjects == null) {
            this.loadedProjects = (await this.mesh.projects.list().send())?.data ?? [];
            // Store the names separately
            this.availableProjects = this.loadedProjects.map(project => project.name);
        }

        // If we don't have any projects, we can't really do anything to begin with.
        if (this.loadedProjects.length === 0) {
            this.changeDetector.markForCheck();
            return;
        }

        // If the route has a project set, then see if it's an actually valid one
        const foundProject = this.loadedProjects.find(project => project.name === this.project);

        // If none was found (or set), then set this current project as the first one we find.
        // This will call this function again, but with the correct context set.
        if (!foundProject) {
            this.handleNavigation({
                project: this.loadedProjects[0].name,
            });
            return;
        }

        // Nothing to do, as we have already resolved everything.
        if (this.project === this.resolvedProject?.name) {
            this.changeDetector.markForCheck();
            return;
        }

        // Reload branches and languages
        this.projectBranches = (await this.mesh.branches.list(this.project).send())?.data ?? [];
        this.projectLanguages = ((await this.mesh.language.list(this.project).send())?.data ?? [])
            .map(lang => lang.languageTag);

        // Clear loaded/resolved elements, as we have to reload them
        this.loadedBranch = null;
        this.loadedNode = null;

        // If a language is set, sort the current languages based
        if (this.language && this.projectLanguages.includes(this.language)) {
            this.reorderLanguages(this.language);
        }
        this.resolvedProject = foundProject;
        this.changeDetector.markForCheck();

        // This happens when the user has permission to read the project, but doesn't have permissions
        // to read the root node. Therefore none of the branches will be available, which would break later on.
        if (this.projectBranches.length === 0) {
            this.noRootNode = true;
            this.changeDetector.markForCheck();
            return;
        }

        await this.handleBranchChange();
    }

    private async handleBranchChange(): Promise<void> {
        const foundBranch = this.projectBranches.find(branch => branch.uuid === this.branch);

        // If the branch wasn't found or provided, we navigate to the root of the project
        // in the first branch we find.
        if (!foundBranch) {
            if (this.projectBranches.length === 0) {
                this.loadedBranch = null;
                this.loadedNode = null;
                this.noRootNode = true;
                this.changeDetector.markForCheck();
                return;
            }

            this.loadedBranch = this.projectBranches[0];
            this.loadedNode = null;
            this.changeDetector.markForCheck();

            this.handleNavigation({
                branch: this.loadedBranch.uuid,
                node: this.resolvedProject.rootNode.uuid,
                language: this.projectLanguages[0],
            });
            return;
        }

        // Update the branch reference
        this.loadedBranch = foundBranch;

        // Reload the schemas, as they can differ between branches
        const schemas = (await this.mesh.projects.listSchemas(this.project, {
            branch: this.branch,
        }).send())?.data ?? [];
        this.projectSchemas = schemas.map(schema => schema.name);
        this.changeDetector.markForCheck();

        await this.handleElementChange();
    }

    private async handleElementChange(): Promise<void> {
        // If no node was provided, we set it back to the root node
        if (!this.node) {
            this.handleNavigation({
                node: this.resolvedProject.rootNode.uuid,
                language: this.projectLanguages[0],
            });
            return;
        }

        // And if no language was provided or is invalid, provide a default
        if (!this.language || !this.projectLanguages.includes(this.language)) {
            this.handleNavigation({
                language: this.projectLanguages[0],
            });
            return;
        }

        // Node is already loaded, nothing to do
        if (this.node === this.loadedNode?.uuid && this.language === this.loadedNode?.language) {
            return;
        }

        this.reorderLanguages(this.language);
        this.changeDetector.markForCheck();

        // Attempt to load the node based on the info we got
        try {
            this.loadedNode = await this.loader.getParentNode(this.project, {
                nodeUuid: this.node,
                lang: this.projectLanguages,
            }, this.branch);
        } catch (err) {
            console.error('Error while loading parent node', err);
            this.loadedNode = null;
        }

        this.changeDetector.markForCheck();

        // If the node couldn't be loaded, then try to load the root-node instead.
        if (!this.loadedNode) {
            // If this is already the root-node, give up and mark as error.
            if (this.node === this.resolvedProject.rootNode.uuid) {
                this.noRootNode = true;
                this.changeDetector.markForCheck();
                return;
            }

            this.handleNavigation({
                node: this.resolvedProject.rootNode.uuid,
                language: this.projectLanguages[0],
            });
            return;
        }
    }

    private reorderLanguages(baseLang: string): void {
        this.projectLanguages = this.projectLanguages.slice().sort(lang => lang === baseLang ? -1 : 1);
    }

    public navigateToProject(project: string): void {
        this.handleNavigation({
            project,
            branch: null,
            node: null,
            language: null,
        });
    }

    public navigateToBranch(branch: BranchReference): void {
        this.loadedBranch = branch;
        this.handleNavigation({
            branch: branch.uuid,
        });
    }

    public navigateToNode(nodeId: string): void {
        if (this.node !== nodeId) {
            this.handleNavigation({
                node: nodeId,
            });
        }
    }

    public navigateToLanguage(language: string): void {
        this.handleNavigation({
            language,
        });
    }
}
