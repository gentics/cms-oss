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
import { Location } from '@angular/common';
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
import { MeshBrowserLoaderService, MeshBrowserNavigatorService } from '../../providers';


@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent
    extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> implements OnChanges {
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
    public currentBranchUuid: string;

    public currentBranch: BranchReference;

    @Input({ alias: ROUTE_MESH_PARENT_NODE_ID })
    public parentNodeUuid: string;

    public languages: Array<string> = [];

    @Input({ alias: ROUTE_MESH_LANGUAGE })
    public currentLanguage = FALLBACK_LANGUAGE;


    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected loader: MeshBrowserLoaderService,
        protected navigatorService: MeshBrowserNavigatorService,
        protected location: Location,
    ) {
        super(changeDetector, router, route, appState);
    }


    ngOnChanges(): void {
        const loadedRepository = this.route.snapshot.data[ROUTE_DATA_MESH_REPO_ITEM];
        if (loadedRepository != null) {
            this.selectedRepository = loadedRepository;
        }

        if (this.parentNodeUuid && this.loggedIn) {
            this.navigatorService.handleTopLevelBreadcrumbNavigation(
                this.currentProject,
                this.currentBranchUuid,
                this.parentNodeUuid,
            );
        }
    }

    private handleNavigation(): void {
        this.navigatorService.handleNavigation(
            this.route,
            this.selectedRepository.id,
            this.currentProject,
            this.currentBranchUuid,
            this.parentNodeUuid,
            this.currentLanguage,
        )
    }

    private handleBreadcrumbNavigation(currentNodeUuid: string) {
        this.navigatorService.handleTopLevelBreadcrumbNavigation(
            this.currentProject,
            this.currentBranchUuid,
            currentNodeUuid,
        );
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.parentNodeUuid = undefined;
        this.currentProject = undefined;
        this.navigatorService.navigateToModuleRoot(this.route);
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
                this.setCurrentProject(projects);
                const branches = await this.loader.getBranches(this.currentProject);
                this.setCurrentBranch(branches);
                this.setLanguageDetails();

                this.changeDetector.markForCheck();
                return Promise.resolve();
            }
        }
        return Promise.reject('Mesh client is unauthenticated');
    }

    private async setLanguageDetails(): Promise<void> {
        const languages = await this.loader.getProjectLanguages(this.currentProject);
        this.languages = languages.map(language => language.languageTag).sort((a, b) => a.localeCompare(b));
        this.currentLanguage = this.appState.now.ui.language;
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
        const currentBranch = branches.find((branch) => branch.uuid === this.currentBranchUuid);

        if (currentBranch) {
            this.currentBranch = currentBranch;
        } else {
            this.currentBranch = this.branches[0];
            this.currentBranchUuid = this.branches[0].uuid;
        }
    }

    public async projectChangeHandler(project: string): Promise<void> {
        this.currentProject = project;
        this.branches = await this.loader.getBranches(this.currentProject);
        this.parentNodeUuid = await this.loader.getProjectRootNodeUuid(this.currentProject);
        this.setCurrentBranch(this.branches);

        this.changeDetector.markForCheck();
        this.handleNavigation();
    }

    public branchChangeHandler(branch: BranchReference): void {
        this.currentBranch = branch;
        this.currentBranchUuid = this.currentBranch.uuid;
        this.handleNavigation();
    }

    public nodeChangeHandler(nodeId: string): void {
        if (this.parentNodeUuid !== nodeId) {
            this.parentNodeUuid = nodeId;
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
