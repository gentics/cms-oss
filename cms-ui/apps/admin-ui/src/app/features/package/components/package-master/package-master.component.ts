import { EditableEntity } from '@admin-ui/common';
import { DevToolPackageTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Package, PackageBO } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreatePackageModalComponent } from '../create-package-modal/create-package-modal.component';

@Component({
    selector: 'gtx-package-master',
    templateUrl: './package-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PackageMasterComponent extends BaseTableMasterComponent<Package, PackageBO> {

    protected entityIdentifier = EditableEntity.DEVTOOL_PACKAGE;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: DevToolPackageTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public async handleCreate(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreatePackageModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();
        if (created) {
            this.loader.reload();
        }
    }
}
