import { AdminUIEntityDetailRoutes, EditableEntity, PortalImplementationBO } from '@admin-ui/common';
import { PortalImplementationTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PortalImplementation } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreatePortalImplementationModalComponent } from '../create-portal-implementation-modal/create-portal-implementation-modal.component';

@Component({
    selector: 'gtx-portal-implementation-master',
    templateUrl: './portal-implementation-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PortalImplementationMasterComponent extends BaseTableMasterComponent<PortalImplementation, PortalImplementationBO> {
    protected entityIdentifier = EditableEntity.PORTAL_IMPLEMENTATION;
    protected detailPath = AdminUIEntityDetailRoutes.PORTAL_IMPLEMENTATION;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: PortalImplementationTableLoaderService,
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
            CreatePortalImplementationModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }
}
