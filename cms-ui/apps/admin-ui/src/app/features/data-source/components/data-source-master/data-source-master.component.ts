import { AdminUIEntityDetailRoutes, DataSourceBO, EditableEntity } from '@admin-ui/common';
import { DataSourceTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DataSource } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateDataSourceModalComponent } from '../create-data-source-modal/create-data-source-modal.component';

@Component({
    selector: 'gtx-data-source-master',
    templateUrl: './data-source-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DataSourceMasterComponent extends BaseTableMasterComponent<DataSource, DataSourceBO> {
    protected entityIdentifier = EditableEntity.DATA_SOURCE;
    protected detailPath = AdminUIEntityDetailRoutes.DATA_SOURCE;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: DataSourceTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    async handleCreateClick(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateDataSourceModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }

}
