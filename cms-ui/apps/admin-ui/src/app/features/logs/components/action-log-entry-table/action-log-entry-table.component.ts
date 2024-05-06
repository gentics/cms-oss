import { ActionLogEntryBO } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActionLogEntry, AnyModelType, LogsListRequest, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { ActionLogEntryLoaderService } from '../../providers';

@Component({
    selector: 'gtx-action-log-entry-table',
    templateUrl: './action-log-entry-table.component.html',
    styleUrls: ['./action-log-entry-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogsTableComponent extends BaseEntityTableComponent<ActionLogEntry, ActionLogEntryBO, LogsListRequest> {

    protected rawColumns: TableColumn<ActionLogEntryBO>[] = [
        {
            id: 'object',
            label: 'logs.log_object',
            fieldPath: 'type.label',
        },
        {
            id: 'objId',
            label: 'logs.log_id',
            fieldPath: 'objId',
        },
        {
            id: 'action',
            label: 'logs.log_action',
            fieldPath: 'action.label',
        },
        {
            id: 'info',
            label: 'logs.log_info',
            fieldPath: 'info',
        },
        {
            id: 'user',
            label: 'logs.log_user',
            fieldPath: 'user',
        },
        {
            id: 'timestamp',
            label: 'logs.log_date',
            fieldPath: 'timestamp',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'logs';

    private logTypes = [];

    private logActions = [];

    private filterFormControl = new FormControl();

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ActionLogEntryLoaderService,
        modalService: ModalService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
        this.init();
    }

    async init(): Promise<void> {
        this.logTypes = await (this.loader as ActionLogEntryLoaderService).getActionLogTypes();
        this.logActions = await (this.loader as ActionLogEntryLoaderService).getActions();
        this.changeDetector.markForCheck();
    }

    public clearControl(): void {
        this.actions = [];
        this.filters = [];
        this.filterFormControl.reset();
        this.reload();
    }

}
