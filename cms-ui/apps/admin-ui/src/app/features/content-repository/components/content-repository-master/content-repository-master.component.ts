import { AdminUIEntityDetailRoutes, ContentRepositoryBO, EditableEntity } from '@admin-ui/common';
import { ContentRepositoryTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ContentRepository } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateContentRepositoryModalComponent } from '../create-content-repository-modal/create-content-repository-modal.component';
import { ManageContentRepositoryRolesModal } from '../manage-content-repository-roles-modal/manage-content-repository-roles-modal.component';

@Component({
    selector: 'gtx-content-repository-master',
    templateUrl: './content-repository-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryMasterComponent extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> {

    protected entityIdentifier = EditableEntity.CONTENT_REPOSITORY;
    protected detailPath = AdminUIEntityDetailRoutes.CONTENT_REPOSITORY;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected tableLoader: ContentRepositoryTableLoaderService,
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
            CreateContentRepositoryModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.tableLoader.reload();
    }

    async openRoleSync(contentRepository: ContentRepositoryBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            ManageContentRepositoryRolesModal,
            { closeOnOverlayClick: false, width: '50%' },
            { contentRepository },
        );
        await dialog.open();
    }
}
