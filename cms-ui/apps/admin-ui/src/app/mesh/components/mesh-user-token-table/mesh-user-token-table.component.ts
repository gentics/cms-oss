import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { User, UserTokenData, UserTokenResponse } from '@gentics/mesh-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { map, Observable } from 'rxjs';
import { BaseEntityTableComponent } from '../../../shared';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { MeshUserTokenBO } from '../../common';
import { MeshUserHandlerService } from '../../providers/mesh-user-handler/mesh-user-handler.service';
import {
    MeshUserTokenTableLoaderOptions,
    MeshUserTokenTableLoaderService,
} from '../../providers/mesh-user-token-table-loader/mesh-user-token-table-loader.service';
import { CopyTokenModal } from '../copy-token-modal/copy-token-modal.component';
import { CreateMeshUserTokenModal } from '../create-user-token-modal/create-user-token-modal.component';

@Component({
    selector: 'gtx-mesh-user-token-table',
    templateUrl: './mesh-user-token-table.component.html',
    styleUrls: ['./mesh-user-token-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class MeshUserTokenTableComponent extends BaseEntityTableComponent<UserTokenData, MeshUserTokenBO, MeshUserTokenTableLoaderOptions> {

    @Input({ required: true })
    public user: User;

    protected rawColumns: TableColumn<MeshUserTokenBO>[] = [
        {
            id: 'name',
            fieldPath: 'name',
            label: 'common.name',
        },
        {
            id: 'expires',
            fieldPath: 'expires',
            label: 'mesh.token_expires',
        },
        {
            id: 'issued',
            fieldPath: 'issued',
            label: 'mesh.token_issued',
        },
        {
            id: 'valid',
            fieldPath: 'valid',
            label: 'mesh.token_valid',
        },
    ];

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'mesh_user_token' as any;

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

    protected override createAdditionalLoadOptions(): MeshUserTokenTableLoaderOptions {
        return {
            user: this.user.uuid,
        };
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshUserTokenBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshUserTokenBO>[] = [
                    {
                        id: 'delete',
                        enabled: true,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        multiple: true,
                        single: true,
                    },
                ];
                return actions;
            }),
        );
    }

    public handleCreateButton(): void {
        this.createApiToken();
    }

    async createApiToken(): Promise<void> {
        const modal = await this.modalService.fromComponent(CreateMeshUserTokenModal, {}, {
            user: this.user,
        });
        const res: UserTokenResponse = await modal.open();

        if (!res) {
            return;
        }

        this.reload();
        const copyModal = await this.modalService.fromComponent(CopyTokenModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            token: res.token,
        });
        await copyModal.open();
    }
}
