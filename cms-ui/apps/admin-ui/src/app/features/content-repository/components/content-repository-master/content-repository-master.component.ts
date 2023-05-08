import { ContentRepositoryBO } from '@admin-ui/common';
import { ContentRepositoryTableLoaderService } from '@admin-ui/core';
import { OpenCRDetailEvent } from '@admin-ui/shared';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService, FocusEditor } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, ContentRepository, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateContentRepositoryModalComponent } from '../create-content-repository-modal/create-content-repository-modal.component';

@Component({
    selector: 'gtx-content-repository-master',
    templateUrl: './content-repository-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryMasterComponent extends BaseTableMasterComponent<ContentRepository, ContentRepositoryBO> {

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'contentRepository';
    protected detailPath = 'content-repository';

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

    async handleOpenDetail(event: OpenCRDetailEvent): Promise<void> {
        await this.router.navigate(
            [{ outlets: { detail: [this.detailPath || this.entityIdentifier, event.item.id, event.tab] } }],
            { relativeTo: this.route },
        );
        this.appState.dispatch(new FocusEditor());
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
}
