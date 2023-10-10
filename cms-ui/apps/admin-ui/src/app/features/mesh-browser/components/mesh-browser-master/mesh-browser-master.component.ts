import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, ContentRepositoryBO, EditableEntity } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { ModalService, TableRow } from '@gentics/ui-core';
import { MeshBrowserContentRepositoryTableLoaderService } from '../../providers/mesh-browser-repository-table-loader.service';

@Component({
    selector: 'gtx-mesh-browser-master',
    templateUrl: './mesh-browser-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshBrowserMasterComponent extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> {

    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;
    protected detailPath = AdminUIEntityDetailRoutes.CONTENT_REPOSITORY;


    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: MeshBrowserContentRepositoryTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public handleRowClick(row: TableRow<ContentRepositoryBO>): void {
        // todo: check auth / perform login
    }

}
