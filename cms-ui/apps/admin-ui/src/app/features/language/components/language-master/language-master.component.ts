import { EditableEntity, LanguageBO } from '@admin-ui/common';
import { LanguageTableLoaderService } from '@admin-ui/core';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Language } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { CreateLanguageModalComponent } from '../create-language-modal/create-language-modal.component';

@Component({
    selector: 'gtx-language-master',
    templateUrl: './language-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LanguageMasterComponent extends BaseTableMasterComponent<Language, LanguageBO> {
    protected entityIdentifier = EditableEntity.LANGUAGE;

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected modalService: ModalService,
        protected loader: LanguageTableLoaderService,
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
            CreateLanguageModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
        );
        const created = await dialog.open();

        if (!created) {
            return;
        }

        this.loader.reload();
    }

}
