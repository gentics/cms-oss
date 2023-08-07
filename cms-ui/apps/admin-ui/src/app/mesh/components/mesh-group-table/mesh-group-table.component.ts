import { BusinessObject } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MeshGroupBO } from '@admin-ui/mesh/common';
import { MeshGroupTableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { NormalizableEntityTypesMap, AnyModelType } from '@gentics/cms-models';
import { GroupResponse } from '@gentics/mesh-models';
import { ModalService, TableColumn } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-group-table',
    templateUrl: './mesh-group-table.component.html',
    styleUrls: ['./mesh-group-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshGroupTableComponent extends BaseEntityTableComponent<MeshGroupBO> {

    protected rawColumns: TableColumn<GroupResponse & BusinessObject>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'group';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshGroupTableLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

}
