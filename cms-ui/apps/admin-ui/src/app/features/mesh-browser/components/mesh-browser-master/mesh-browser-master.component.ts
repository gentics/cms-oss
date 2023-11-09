import {
    AdminUIModuleRoutes,
    ContentRepositoryBO,
    EditableEntity,
    ROUTE_MESH_BRANCH_ID,
    ROUTE_MESH_LANGUAGE,
    ROUTE_MESH_PARENT_NODE_ID,
    ROUTE_MESH_PROJECT_ID,
    ROUTE_MESH_REPOSITORY_ID,
} from '@admin-ui/common';
import { ContentRepositoryHandlerService } from '@admin-ui/core';
import { getUserDisplayName } from '@admin-ui/mesh';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnInit,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { BranchReference, ProjectResponse, User } from '@gentics/mesh-models';
import { TableRow, toValidNumber } from '@gentics/ui-core';
import { MeshBrowserLoaderService } from '../../providers';

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent
    extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> implements OnInit {
    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;

    public selectedRepository: ContentRepository;

    @Input({ alias: ROUTE_MESH_REPOSITORY_ID, transform: toValidNumber })
    public currentRepositoryId: number;

    public me: User;

    public meName: string;

    public loggedIn = false;

    public projects: Array<string> = [];

    @Input({ alias: ROUTE_MESH_PROJECT_ID })
    public currentProject: string;

    public branches: Array<BranchReference> = [];

    @Input({ alias: ROUTE_MESH_BRANCH_ID})
    public currentBranchId: string;

    public currentBranch: BranchReference;

    @Input({ alias: ROUTE_MESH_PARENT_NODE_ID })
    public currentNodeId: string;

    public languages: Array<string> = ['de', 'en']; // todo: take from api: GPU-1249

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public currentLanguage = 'de'; // todo: take from api: GPU-1249


    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected handler: ContentRepositoryHandlerService,
        protected loader: MeshBrowserLoaderService,
    ) {
        super(changeDetector, router, route, appState);
    }

    ngOnInit(): void {
        super.ngOnInit()

        // this.route.params.subscribe((params) => {
        //     if (params.language) {
        //         this.currentLanguage = params.language;
        //     }
        //     if (params.project) {
        //         this.currentProject = params.project;
        //     }
        //     if (params.branch) {
        //         this.currentBranchId = params.branch;
        //     }
        //     if (params.parent) {
        //         this.currentNodeId = params.parent;
        //     }
        //     if (params.language) {
        //         this.currentLanguage = params.language;
        //     }
        //     if (params.repository) {
        //         this.currentRepositoryId = params.repository;
        //         this.handler
        //             .get(this.currentRepositoryId)
        //             .toPromise()
        //             .then((repository) => {
        //                 this.selectedRepository = repository.contentRepository;
        //             })
        //     }
        // });
    }

    public rowClickHandler(row: TableRow<ContentRepositoryBO>): void {
        this.selectedRepository = row.item;
        this.currentRepositoryId = this.selectedRepository.id;
    }

    private handleNavigation(): void {
        // this.router.navigate([
        //     '/'+AdminUIModuleRoutes.MESH_BROWSER,
        //     this.currentRepositoryId,
        //     this.currentProject,
        //     this.currentBranchId,
        //     this.currentNodeId,
        //     this.currentLanguage,
        // ],
        // { relativeTo: this.route });
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`], {
            relativeTo: this.route,
        });

        this.currentNodeId = undefined;
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

            if (!this.currentProject) {
                this.loadProjectDetails().then(() => {
                    this.handleNavigation()
                });
            }
        }
    }

    protected async loadProjectDetails(): Promise<void> {
        if (this.loggedIn) {
            const projects = await this.loader.getProjects()

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
        }
        else {
            this.currentProject = projects[0].name;
        }
        this.currentNodeId = 'undefined';
    }

    private setCurrentBranch(branches: BranchReference[]): void {
        this.branches = branches;
        const currentBranch = branches.find(branch => branch.uuid === this.currentBranchId);

        if (currentBranch) {
            this.currentBranch = currentBranch;
        }
        else {
            this.currentBranch = this.branches[0];
            this.currentBranchId = this.branches[0].uuid;
        }
    }

    public async projectChangeHandler(project: string): Promise<void> {
        this.currentNodeId = undefined;
        this.currentProject = project;
        this.branches = await this.loader.getBranches(this.currentProject);
        this.handleNavigation()
    }

    public branchChangeHandler(branch: BranchReference): void {
        this.currentBranch = branch;
        this.currentBranchId = this.currentBranch.uuid;
        this.handleNavigation()
    }

    public nodeChangeHandler(nodeId: string): void {
        if (this.currentNodeId !== nodeId) {
            this.currentNodeId = nodeId;
            this.handleNavigation()
        }
    }

    public languageChangeHandler(language: string): void {
        this.currentLanguage = language;
        this.languages = this.languages.sort((a, _b) =>
            a === this.currentLanguage ? -1 : 1,
        );
        this.handleNavigation()
    }
}
