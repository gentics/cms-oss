import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, ContentRepositoryBO, EditableEntity } from '@admin-ui/common';
import { ContentRepositoryHandlerService } from '@admin-ui/core';
import { getUserDisplayName } from '@admin-ui/mesh';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { User } from '@gentics/mesh-models';
import { TableRow } from '@gentics/ui-core';
import { MeshBrowserLoaderService } from '../../providers';


const MESH_ID_PARAM = 'meshId';

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> {

    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;

    protected detailPath = AdminUIEntityDetailRoutes.MESH_BROWSER;

    public selectedRepository: ContentRepository;

    public me: User;

    public meName: string;

    public loggedIn = false;

    public projects: Array<string> = [];

    public branches: Array<string> = [];

    public currentProject: string;


    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected handler: ContentRepositoryHandlerService,
        protected loader: MeshBrowserLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public handleRowClick(row: TableRow<ContentRepositoryBO>): void {
        this.selectedRepository = row.item;
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`, { [MESH_ID_PARAM]: row.item.id }], { relativeTo: this.route });
    }

    public navigateBack(): void {
        this.selectedRepository = null;
        this.router.navigate([`/${AdminUIModuleRoutes.MESH_BROWSER}`], { relativeTo: this.route });
    }

    public handleMeshLogin(event: { loggedIn: boolean, user?: User }): void {
        this.loggedIn = event.loggedIn;

        if (event.loggedIn) {
            if (event.user) {
                this.me = event.user;
                this.meName = getUserDisplayName(this.me);
            } else {
                this.loader.authMe().then(res => {
                    this.me = res;
                    this.meName = getUserDisplayName(this.me);
                    this.changeDetector.markForCheck();
                });
            }

            this.loadProjects()
        }
    }

    protected async loadProjects(): Promise<void> {
        this.projects = await this.loader.getProjects();

        if (this.projects.length > 0) {
            this.currentProject = this.projects[0];
            this.branches = await this.loader.getBranches(this.currentProject);

            this.changeDetector.markForCheck();
        }
    }

    public projectChanged(project: string): void {
        this.currentProject = project;
    }

}
