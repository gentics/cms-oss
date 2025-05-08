import { AdminUIEntityDetailRoutes, ContentRepositoryBO, ContentRepositoryDetailTabs } from '@admin-ui/common';
import {
    ContentRepositoryHandlerService,
    I18nService,
    PermissionsService,
} from '@admin-ui/core';
import {
    MeshBrowserContentRepositoryTableLoaderService,
    MeshContentRepositoryTableLoaderOptions,
} from '@admin-ui/features/mesh-browser/providers/mesh-browser-repository-table-loader.service';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, ContentRepository, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';


@Component({
    selector: 'gtx-mesh-browser-repository-table',
    templateUrl: './mesh-browser-repository-table.component.html',
    styleUrls: ['./mesh-browser-repository-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ContentRepositoryTableComponent
    extends BaseEntityTableComponent<ContentRepository, ContentRepositoryBO, MeshContentRepositoryTableLoaderOptions>
    implements OnChanges {

    public readonly ContentRepositoryDetailTabs = ContentRepositoryDetailTabs;
    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public linkDetails = false;

    protected rawColumns: TableColumn<ContentRepositoryBO>[] = [
        {
            id: 'name',
            label: 'contentRepository.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'url',
            label: 'contentRepository.url',
            fieldPath: 'url',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'contentRepository';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshBrowserContentRepositoryTableLoaderService,
        modalService: ModalService,
        protected router: Router,
        protected route: ActivatedRoute,
        protected permissions: PermissionsService,
        protected handler: ContentRepositoryHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader as any,
            modalService,
        );
    }

}
