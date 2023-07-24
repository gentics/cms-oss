import { AdminUIEntityDetailRoutes, ObjectPropertyCategoryBO } from '@admin-ui/common';
import { BaseTableMasterComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, NormalizableEntityTypesMap, ObjectPropertyCategory } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { ObjectPropertyCategoryTableLoaderService } from '../../providers';
import { CreateObjectPropertyCategoryModalComponent } from '../create-object-property-category-modal/create-object-property-category-modal.component';

@Component({
    selector: 'gtx-object-property-category-master',
    templateUrl: './object-property-category-master.component.html',
    styleUrls: ['./object-property-category-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyCategoryMasterComponent extends BaseTableMasterComponent<ObjectPropertyCategory, ObjectPropertyCategoryBO> {

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType>= 'objectPropertyCategory';
    protected detailPath = AdminUIEntityDetailRoutes.OBJECT_PROPERTY_CATEGORY;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: ObjectPropertyCategoryTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        )
    }

    async handleCreate(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateObjectPropertyCategoryModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();
        if (created) {
            this.loader.reload();
        }
    }
}
