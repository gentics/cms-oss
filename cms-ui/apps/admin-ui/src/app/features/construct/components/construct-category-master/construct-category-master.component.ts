import { AdminUIEntityDetailRoutes, ConstructCategoryBO, EditableEntity } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConstructCategory } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ConstructCategoryTableLoaderService } from '../../providers';
import { CreateConstructCategoryModalComponent } from '../create-construct-category-modal/create-construct-category-modal.component';

@Component({
    selector: 'gtx-construct-category-master',
    templateUrl: './construct-category-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructCategoryMasterComponent extends BaseTableMasterComponent<ConstructCategory, ConstructCategoryBO> implements OnInit {

    protected entityIdentifier = EditableEntity.CONSTRUCT_CATEGORY;
    protected detailPath = AdminUIEntityDetailRoutes.CONSTRUCT_CATEGORY;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected loader: ConstructCategoryTableLoaderService,
        protected modalService: ModalService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    public async createNewConstructCategory(): Promise<void> {
        const dialog = await this.modalService.fromComponent(CreateConstructCategoryModalComponent, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
            width: '80%',
        });
        const didCreate = await dialog.open();

        // If a new construct has been created, we need to reload the list
        // to make it appear in it.
        if (didCreate) {
            this.loader.reload();
        }
    }
}
