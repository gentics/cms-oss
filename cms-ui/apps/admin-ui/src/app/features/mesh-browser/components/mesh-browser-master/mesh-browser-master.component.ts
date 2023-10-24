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
import { BranchReference, User } from '@gentics/mesh-models';
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

    public languages: Array<string> = ['de', 'en'];

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public currentLanguage = 'de';


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

        this.route.params.subscribe((params) => {
            if (params.language) {
                this.currentLanguage = params.language;
            }
            if (params.project) {
                this.currentProject = params.project;
            }
            if (params.branch) {
                this.currentBranchId = params.branch;
            }
            if (params.parent) {
                this.currentNodeId = params.parent;
            }
            if (params.language) {
                this.currentLanguage = params.language;
            }
            if (params.repository) {
                this.currentRepositoryId = params.repository;
                this.handler
                    .get(this.currentRepositoryId)
                    .toPromise()
                    .then((repository) => {
                        this.selectedRepository = repository.contentRepository;
                        this.loadProjectDetails().then(() =>
                            this.handleNavigation(),
                        )
                    });
            }
        });
    }

    public rowClickHandler(row: TableRow<ContentRepositoryBO>): void {
        this.selectedRepository = row.item;
        this.currentRepositoryId = this.selectedRepository.id;
        this.loadProjectDetails().then(() =>
            this.handleNavigation(),
        )
    }

    private handleNavigation(): void {
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`, {
            [ROUTE_MESH_REPOSITORY_ID]: this.currentRepositoryId,
            [ROUTE_MESH_PROJECT_ID]: this.currentProject,
            [ROUTE_MESH_BRANCH_ID]: this.currentBranchId,
            [ROUTE_MESH_PARENT_NODE_ID]: this.currentNodeId,
            [ROUTE_MESH_LANGUAGE]: this.currentLanguage},
        ], { relativeTo: this.route });
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`], {
            relativeTo: this.route,
        });
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

            this.loadProjectDetails();
        }
    }

    protected async loadProjectDetails(): Promise<void> {
        this.projects = await this.loader.getProjects();

        if (this.projects.length > 0) {
            this.setCurrentProject()
            this.branches = await this.loader.getBranches(this.currentProject);
            this.setCurrentBranch()

            this.changeDetector.markForCheck();
        }
    }

    private setCurrentProject(): void {
        const currentProject = this.projects.find(project => project === this.currentProject);
        this.currentProject = currentProject ?? this.projects[0];
    }

    private setCurrentBranch(): void {
        const currentBranch = this.branches.find(branch => branch.uuid === this.currentBranchId);
        this.currentBranch = currentBranch ?? this.branches[0];
        this.currentBranchId = this.currentBranch.uuid
    }

    public async projectChangeHandler(project: string): Promise<void> {
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
