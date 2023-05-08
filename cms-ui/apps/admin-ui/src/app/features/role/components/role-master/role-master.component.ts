import { RoleBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, NormalizableEntityTypesMap, Role } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { RoleTableLoaderService } from '../../providers';
import { CreateRoleModalComponent } from '../create-role-modal/create-role-modal.component';

@Component({
    selector: 'gtx-role-master',
    templateUrl: './role-master.component.html',
    styleUrls: ['role-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleMasterComponent extends BaseTableMasterComponent<Role, RoleBO> {
    entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'role';

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected loader: RoleTableLoaderService,
        protected modalService: ModalService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public async handleCreateClick(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateRoleModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }
}
