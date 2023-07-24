import { AdminUIEntityDetailRoutes, ObjectPropertyBO } from '@admin-ui/common';
import { ObjectPropertyTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NormalizableEntityType, ObjectPropertiesObjectType, ObjectProperty } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateObjectPropertyModalComponent } from '../create-object-property-modal/create-object-property-modal.component';

@Component({
    selector: 'gtx-object-property-master',
    templateUrl: './object-property-master.component.html',
    styleUrls: ['./object-property-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyMasterComponent extends BaseTableMasterComponent<ObjectProperty, ObjectPropertyBO> {

    // tslint:disable-next-line: variable-name
    readonly ObjectPropertyMasterTabs = ObjectPropertiesObjectType;

    protected entityIdentifier: NormalizableEntityType = 'objectProperty';
    protected detailPath = AdminUIEntityDetailRoutes.OBJECT_PROPERTY;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: ObjectPropertyTableLoaderService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    async handleCreate(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            CreateObjectPropertyModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();
        if (created) {
            this.loader.reload();
        }
    }
}
