import { Component, OnInit, signal } from '@angular/core';
import { ModalService, TableAction, TableActionClickEvent, TableColumn, TableRow, TableSelectAllType } from '@gentics/ui-core';
import { forkJoin, map } from 'rxjs';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { I18nNotificationService, I18nService } from '../../providers';
import { ApiTokensCreateModalComponent } from '../api-tokens-create-modal/api-tokens-create-modal.component';
import { CopyTokenModal } from '../copy-token-modal/copy-token-modal.component';

interface ApiTokenBo {
    tmpId: string;
    name: string;
    cdate: Date;
    expires: Date;
    lastUsed: Date;
    valid: boolean;
}

const ACTION_DELETE = 'delete';

@Component({
    selector: 'gtx-manage-api-tokens-table',
    templateUrl: './api-tokens-table.component.html',
    styleUrls: ['./api-tokens-table.component.scss'],
    standalone: false,
})
export class ApiTokensTableComponent implements OnInit {
    public readonly TableSelectAllType = TableSelectAllType;
    public readonly rows = signal<TableRow<ApiTokenBo>[]>([]);
    public readonly page = signal<number>(1);
    public readonly totalCount = signal<number>(999);
    public readonly sortBy = signal<string>('cdate');
    public readonly sortOrder = signal<string>('desc');
    public perPage = 10;
    public columns = [];
    public actions: TableAction<ApiTokenBo>[] = [
        {
            id: ACTION_DELETE,
            enabled: true,
            icon: 'delete',
            label: this.i18n.instant('editor.tagtype_delete_label'),
            type: 'alert',
            single: true,
            multiple: true,
        },
    ];

    public apiTokenSelection: string[] = [];

    constructor(
        private client: GCMSRestClientService,
        private modalService: ModalService,
        private i18n: I18nService,
        protected api: GCMSRestClientService,
        protected notification: I18nNotificationService,
    ) {}

    ngOnInit(): void {
        this.columns = [
            {
                id: 'name',
                label: this.i18n.instant('role.name'),
                fieldPath: 'name',
                sortable: true,
            },
            {
                id: 'cdate',
                label: this.i18n.instant('common.cdate'),
                fieldPath: 'cdate',
                sortable: true,
            },
            {
                id: 'expires',
                label: this.i18n.instant('api_token.expires_at'),
                fieldPath: 'expires',
                sortable: true,
            },
            {
                id: 'lastUsed',
                label: this.i18n.instant('api_token.last_used'),
                fieldPath: 'lastUsed',
                sortable: true,
            },
            {
                id: 'valid',
                label: this.i18n.instant('license.status_VALID'),
                fieldPath: 'valid',
            },
        ];

        this.refreshData();
    }

    public changePage(page: number): void {
        this.page.set(page);
        this.refreshData();
    }

    private refreshData(): void {
        const sort = this.sortOrder() === 'asc' ? `+${this.sortBy()}` : `-${this.sortBy()}`;
        this.client.admin.getApiTokens(this.perPage, this.page(), sort)
            .pipe(
                map((res) => {
                    this.totalCount.set(res.numItems);
                    return res.items.map((token) => ({
                        id: `${token.id}`,
                        item: {
                            tmpId: `${token.id}`,
                            name: token.name,
                            cdate: new Date(token.cdate * 1000),
                            expires: token.expires === 0 ? '' : new Date(token.expires * 1000),
                            lastUsed: token.lastUsed === 0 ? '' : new Date(token.lastUsed * 1000),
                            valid: token.valid,
                        },
                    } as TableRow<ApiTokenBo>));
                }),
            )
            .subscribe({
                next: (tokens) => {
                    this.rows.set(tokens);
                },
            });
    }

    public handleCreateButton(): void {
        this.onShowApiTokenCreateModal();
    }

    public handleAction(event: TableActionClickEvent<ApiTokenBo>): void {
        if (event.actionId !== ACTION_DELETE || (!event.selection && !event.item)) {
            return;
        }

        const tokenIds = event.selection
            ? this.apiTokenSelection
            : [this.rows().find((e) => e.item.tmpId === event.item.tmpId)?.id];

        this.onShowApiTokenDeleteModal(tokenIds);
    }

    private async onShowApiTokenCreateModal(): Promise<void> {
        const modal = await this.modalService.fromComponent(ApiTokensCreateModalComponent, {}, {
            apiTokenNames: this.rows().map((e) => e.item.name),
        });

        const response = await modal.open();

        if (!response) {
            return;
        }

        this.refreshData();

        const copyModal = await this.modalService.fromComponent(CopyTokenModal, {}, {
            token: response.token,
            title: this.i18n.instant('api_token.token_created'),
            successMessage: this.i18n.instant('mesh.copy_token_success'),
            errorMessage: this.i18n.instant('mesh.copy_token_error'),
        });

        await copyModal.open();
    }

    private async onShowApiTokenDeleteModal(ids: Array<string>): Promise<void> {
        const dialog = await this.modalService.dialog({
            title: this.i18n.instant('api_token.delete'),
            body: this.i18n.instant('api_token.delete_warning'),
            buttons: [
                {
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
                {
                    label: this.i18n.instant('shared.confirm_button'),
                    id: 'api-token-delete',
                    type: 'alert',
                    returnValue: true,
                },
            ],
        });
        const doDelete = await dialog.open();

        if (doDelete) {
            this.deleteApiTokens(ids);
        }
    }

    public updateSelection(selections: Array<string>): void {
        this.apiTokenSelection = selections;
    }

    public updateSortOrder(order: string): void {
        this.sortOrder.set(order);
        this.refreshData();
    }

    public updateSortBy(sortBy: string): void {
        this.sortBy.set(sortBy);
        this.refreshData();
    }

    private deleteApiTokens(ids: Array<string>): void {
        forkJoin(
            ids.map((id) => this.api.admin.deleteApiTokens(id)),
        ).subscribe({
            next: () => {
                const name = this.rows()
                    .filter((row) => ids.includes(row.item.tmpId))
                    .map((row) => row.item.name)
                    .join(', ');

                this.notification.show({
                    type: 'success',
                    message: 'shared.item_singular_deleted',
                    id: 'api-token-delete-success',
                    translationParams: {
                        name,
                    },
                });

                this.refreshData();
            },
            error: (err) => {
                console.error(err);
            },
        });
    }
}
