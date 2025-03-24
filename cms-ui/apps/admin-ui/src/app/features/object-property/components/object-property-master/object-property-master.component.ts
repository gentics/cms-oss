import { AdminUIEntityDetailRoutes, EditableEntity, ObjectPropertyBO } from '@admin-ui/common';
import { BaseTableMasterComponent, ObjectPropertyTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ObjectPropertiesObjectType, ObjectProperty } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateObjectPropertyModalComponent } from '../create-object-property-modal/create-object-property-modal.component';

@Component({
    selector: 'gtx-object-property-master',
    templateUrl: './object-property-master.component.html',
    styleUrls: ['./object-property-master.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ObjectPropertyMasterComponent extends BaseTableMasterComponent<ObjectProperty, ObjectPropertyBO> {

    public readonly ObjectPropertyMasterTabs = ObjectPropertiesObjectType;

    protected entityIdentifier = EditableEntity.OBJECT_PROPERTY;
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
