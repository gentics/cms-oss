import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, ContentRepositoryBO, EditableEntity } from '@admin-ui/common';
import { ContentRepositoryHandlerService } from '@admin-ui/core';
import { getUserDisplayName } from '@admin-ui/mesh';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { User } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { TableRow } from '@gentics/ui-core';


const MESH_ID_PARAM = 'meshId';

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    styleUrls: ['./mesh-browser-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> implements OnInit {

    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;
    protected detailPath = AdminUIEntityDetailRoutes.MESH_BROWSER;

    public selectedRepository: ContentRepository;
    public me: User;
    public meName: string;
    public loggedIn = false;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected handler: ContentRepositoryHandlerService,
        protected mesh: MeshRestClientService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public ngOnInit(): void {
        super.ngOnInit();
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
                this.mesh.auth.me().then(res => {
                    this.me = res;
                    this.meName = getUserDisplayName(this.me);
                    this.changeDetector.markForCheck();
                });
            }
        }
    }

}
