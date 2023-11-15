import {
    AdminUIModuleRoutes,
    ContentRepositoryBO,
    EditableEntity,
    ROUTE_DATA_MESH_REPO_ITEM,
    ROUTE_MESH_BRANCH_ID,
    ROUTE_MESH_BROWSER_OUTLET,
    ROUTE_MESH_LANGUAGE,
    ROUTE_MESH_PARENT_NODE_ID,
    ROUTE_MESH_PROJECT_ID,
} from '@admin-ui/common';
import {
    BreadcrumbsService,
    ContentRepositoryHandlerService,
} from '@admin-ui/core';
import { getUserDisplayName } from '@admin-ui/mesh';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { BranchReference, ProjectResponse, User } from '@gentics/mesh-models';
import { IBreadcrumbRouterLink } from '@gentics/ui-core';
import { MeshBrowserLoaderService } from '../../providers';

const DEFAULT_LANGUAGE = 'de';  // todo: take from api: GPU-1249

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent
    extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> implements OnChanges
{
    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;

    public selectedRepository: ContentRepository;

    public me: User;

    public meName: string;

    public loggedIn = false;

    public projects: Array<string> = [];

    @Input({ alias: ROUTE_MESH_PROJECT_ID })
    public currentProject: string;

    public branches: Array<BranchReference> = [];

    @Input({ alias: ROUTE_MESH_BRANCH_ID })
    public currentBranchId: string;

    public currentBranch: BranchReference;

    @Input({ alias: ROUTE_MESH_PARENT_NODE_ID })
    public parentNodeUuid: string;

    public languages: Array<string> = ['de', 'en']; // todo: take from api: GPU-1249

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public currentLanguage = DEFAULT_LANGUAGE; // todo: take from api: GPU-1249


    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected handler: ContentRepositoryHandlerService,
        protected loader: MeshBrowserLoaderService,
        protected breadcrumbService: BreadcrumbsService,
        protected contentRepository: ContentRepositoryHandlerService,
    ) {
        super(changeDetector, router, route, appState);
    }


    ngOnChanges(changes: SimpleChanges): void {
        const loadedRepository = this.route.snapshot.data[ROUTE_DATA_MESH_REPO_ITEM];
        if (loadedRepository != null) {
            this.selectedRepository = loadedRepository;
        }

        if (this.parentNodeUuid) {
            this.handleBreadcrumbNavigation(this.parentNodeUuid);
        }
    }

    private handleNavigation(): void {
        this.router.navigate(
            [
                '/' + AdminUIModuleRoutes.MESH_BROWSER,
                this.selectedRepository.id,
                {
                    outlets: {
                        [ROUTE_MESH_BROWSER_OUTLET]: [
                            'list',
                            this.currentProject,
                            this.currentBranchId,
                            this.parentNodeUuid ?? 'undefined',
                            this.currentLanguage ?? DEFAULT_LANGUAGE,
                        ],
                    },
                },
            ],
            { relativeTo: this.route },
        );
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`], {
            relativeTo: this.route,
        });

        this.parentNodeUuid = undefined;
        this.currentProject = undefined;
    }

    public meshLoginHandler(event: { loggedIn: boolean; user?: User }): void {
        this.loggedIn = event.loggedIn;

        if (event.loggedIn) {
            if (event.user) {
                this.me = event.user;
                this.meName = getUserDisplayName(this.me);
            } else {
                this.loader.authMe().then((res) => {
                    this.me = res;
                    this.meName = getUserDisplayName(this.me);
                    this.changeDetector.markForCheck();
                });
            }

            if (!this.currentProject || this.projects?.length === 0) {
                this.loadProjectDetails().then(() => {
                    // this.handleNavigation();
                    this.handleBreadcrumbNavigation(this.parentNodeUuid);
                });
            }
        }
    }

    protected async loadProjectDetails(): Promise<void> {
        if (this.loggedIn) {
            const projects = await this.loader.getProjects();

            if (projects.length > 0) {
                this.projects = projects.map(project => project.name);
                this.setCurrentProject(projects)
                const branches = await this.loader.getBranches(this.currentProject);
                this.setCurrentBranch(branches)

                this.changeDetector.markForCheck();
                return Promise.resolve();
            }
        }
        return Promise.reject('Mesh client is unauthenticated');
    }

    private setCurrentProject(projects: ProjectResponse[]): void {
        const currentProject = projects.find(project => project.name === this.currentProject);

        if (currentProject) {
            this.currentProject = currentProject.name;
        } else {
            this.currentProject = projects[0].name;
            this.parentNodeUuid = projects[0].rootNode.uuid;
        }
    }

    private setCurrentBranch(branches: BranchReference[]): void {
        this.branches = branches;
        const currentBranch = branches.find((branch) => branch.uuid === this.currentBranchId);

        if (currentBranch) {
            this.currentBranch = currentBranch;
        } else {
            this.currentBranch = this.branches[0];
            this.currentBranchId = this.branches[0].uuid;
        }
    }

    public async projectChangeHandler(project: string): Promise<void> {
        this.parentNodeUuid = undefined;
        this.currentProject = project;
        this.branches = await this.loader.getBranches(this.currentProject);
        this.handleNavigation();
    }

    public branchChangeHandler(branch: BranchReference): void {
        this.currentBranch = branch;
        this.currentBranchId = this.currentBranch.uuid;
        this.handleNavigation();
    }

    private async handleBreadcrumbNavigation(nodeUuid: string): Promise<void> {
        const breadcrumbEntries = await this.loader.getBreadcrumbNavigation(
            this.currentProject,
            { nodeUuid },
            this.currentBranchId,
        );

        const breadcrumbPath: IBreadcrumbRouterLink[] = [
            {
                route: ['/'],
                text: 'Dashboard',
            },
            {
                route: ['/' + AdminUIModuleRoutes.MESH_BROWSER],
                text: 'Mesh Browser',
            },
        ];

        for (const breadcrumbEntry of breadcrumbEntries) {
            if (!breadcrumbEntry.parent) {
                continue;
            }

            const navigationEntry: IBreadcrumbRouterLink = {
                route: [
                    '/' + AdminUIModuleRoutes.MESH_BROWSER,
                    this.selectedRepository.id,
                    {
                        outlets: {
                            [ROUTE_MESH_BROWSER_OUTLET]: [
                                'list',
                                this.currentProject,
                                this.currentBranchId,
                                breadcrumbEntry.parent.node.uuid,
                                this.currentLanguage ?? DEFAULT_LANGUAGE,
                            ],
                        },
                    },
                ],
                text:
                    breadcrumbEntry.node.displayName ??
                    breadcrumbEntry.node.uuid,
            };

            breadcrumbPath.push(navigationEntry);
        }

        this.breadcrumbService.setBreadcrumbs(breadcrumbPath);
    }

    public nodeChangeHandler(nodeId: string): void {
        if (this.parentNodeUuid !== nodeId) {
            this.parentNodeUuid = nodeId;
            this.handleBreadcrumbNavigation(nodeId);
            this.handleNavigation();
        }
    }

    public languageChangeHandler(language: string): void {
        this.currentLanguage = language;
        this.languages = this.languages.sort((a, _b) =>
            a === this.currentLanguage ? -1 : 1,
        );
        this.handleNavigation();
    }
}
