import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { UserTokenData } from '@gentics/mesh-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { BaseEntityTableComponent } from '../../../shared';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { MeshUserTokenBO } from '../../common';
import { MeshUserHandlerService } from '../../providers/mesh-user-handler/mesh-user-handler.service';
import { MeshUserTokenTableLoaderService } from '../../providers/mesh-user-token-table-loader/mesh-user-token-table-loader.service';
import { NormalizableEntityTypesMap, AnyModelType } from '@gentics/cms-models';
import { CopyTokenModal } from '../copy-token-modal/copy-token-modal.component';

@Component({
    selector: 'gtx-mesh-user-token-table',
    templateUrl: './mesh-user-token-table.component.html',
    styleUrls: ['./mesh-user-token-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MeshUserTokenTableComponent extends BaseEntityTableComponent<UserTokenData, MeshUserTokenBO> {

    @Input({ required: true })
    public user: string;

    protected rawColumns: TableColumn<MeshUserTokenBO>[] = [];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType>;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: MeshUserTokenTableLoaderService,
        modalService: ModalService,
        protected handler: MeshUserHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public handleCreateButton(): void {

    }

    async createApiToken(): Promise<void> {
        const shouldProceed = false;
        if (!shouldProceed) {
            return;
        }

        const res = await this.handler.createToken(this.user, {
            name: 'FIXME: Yo',
        });
        const copyModal = await this.modalService.fromComponent(CopyTokenModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            token: res.token,
        });
        await copyModal.open();
    }
}
