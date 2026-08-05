import {Component, Input, OnInit, signal} from '@angular/core';
import {ModalService, TableAction, TableActionClickEvent, TableColumn, TableRow, TableSelectAllType} from '@gentics/ui-core';

import { ApiTokensCreateModalComponent, ApiTokensDeleteModalComponent, I18nService } from '@gentics/cms-components';
import { map } from 'rxjs';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ApiTokenBo } from '@gentics/cms-models';

const ACTION_DELETE = 'delete';

@Component({
    selector: 'gtx-manage-api-tokens-table',
    templateUrl: './api-tokens-table.tpl.html',
    styleUrls: ['./api-tokens-table.component.scss'],
    standalone: false
})
export class ApiTokensTableComponent implements OnInit {
    public readonly TableSelectAllType = TableSelectAllType;

    @Input()
    isDisabled: boolean;

    public readonly apiTokenRows = signal<TableRow<ApiTokenBo>[]>([]);
    
    protected apiTokenColumns: TableColumn<ApiTokenBo>[] = [];
    
    public apiTokenActions: TableAction<ApiTokenBo>[] = [
        {
            id: ACTION_DELETE,
            enabled: true,
            icon: 'delete',
            label: this.i18n.instant('editor.tagtype_delete_label'),
            type: 'alert',
            single: true,
            multiple: true,
        }
    ];
    
    public apiTokenSelection: string[] = [];


    constructor(
        private client: GCMSRestClientService,
        private modalService: ModalService,
        private i18n: I18nService,
    ) {}

    ngOnInit(): void {
        this.apiTokenColumns = [
            {
                id: 'name',
                label: this.i18n.instant('role.name'),
                fieldPath: 'name',
            },
            {
                id: 'cdate',
                label: this.i18n.instant('common.cdate'),
                fieldPath: 'cdate',
            },
            {
                id: 'date',
                label: this.i18n.instant('api_token.expires_at'),
                fieldPath: 'expires',
            },
            {
                id: 'lastUsed',
                label: this.i18n.instant('api_token.last_used'),
                fieldPath: 'lastUsed',
            },
            {
                id: 'valid',
                label: this.i18n.instant('license.status_VALID'),
                fieldPath: 'valid',
            },
        ];

        this.refreshData();
    }

    private refreshData(): void {
        this.client.admin.getApiTokens()
            .pipe(
                map(tokens => {
                    return tokens.items.map(token => ({
                        id: `${token.id}`,
                        item: {
                            tmpId: `${token.id}`,
                            name: token.name,
                            cdate: this.toDateString(token.cdate),
                            expires: this.toDateString(token.expires),
                            lastUsed: this.toDateString(token.lastUsed),
                            valid: token.valid
                        }
                    } as TableRow<ApiTokenBo>))
                })
            )
            .subscribe({
                next: (tokens) => {
                    this.apiTokenRows.set(tokens);
                }
            });
    }

    public handleCreateButton() {
        this.onShowApiTokenCreateModal();
    }

    public handleAction(event: TableActionClickEvent<ApiTokenBo>): void {
        if(event.actionId !== ACTION_DELETE || (!event.selection && !event.item)) {
            return;
        }

        const tokenIds = event.selection ?
            this.apiTokenSelection :
            [this.apiTokenRows().find(e => e.item.tmpId === event.item.tmpId)?.id];

        this.onShowApiTokenDeleteModal(tokenIds);
    }

    private removeTokenFromList(ids: Array<string>): Array<TableRow<ApiTokenBo>> {
        return this.apiTokenRows().filter(e => !ids.includes(e.item.tmpId));
    }

    private onShowApiTokenCreateModal(): void {
        this.modalService.fromComponent(ApiTokensCreateModalComponent, {
            onClose: (reason) => {
                this.refreshData();
            },
        }, {
            apiTokenNames: this.apiTokenRows().map(e => e.item.name),
        })
        .then(modal => modal.open())
        .catch((error) => console.log(error));
    }

     private onShowApiTokenDeleteModal(ids: Array<string>): void {
        this.modalService.fromComponent(ApiTokensDeleteModalComponent, {
            onClose: (reason) => {
                const updatedTokesn = this.removeTokenFromList(ids);
                this.apiTokenRows.set(updatedTokesn);
            },
        }, {
            apiTokenIds: ids,
        })
        .then(modal => modal.open())
        .catch((error) => console.log(error));
    }

    public updateSelection(selections: Array<string>) {
        this.apiTokenSelection = selections;
    }

    private toDateString(dateNumber: number): string {
        if(dateNumber === 0) {
            return "-";
        }

        return new Intl.DateTimeFormat('de-DE').format(
            new Date(dateNumber * 1000)
        );
    }

    public getApiTokenNames() : Array<String> {
        return this.apiTokenRows().map(e => e.item.name);
    }
}
