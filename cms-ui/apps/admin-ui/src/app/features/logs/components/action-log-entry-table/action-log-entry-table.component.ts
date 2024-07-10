import { ActionLogEntryBO } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActionLogEntry, AnyModelType, LogsListRequest, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableColumn } from '@gentics/ui-core';
import { ActionLogEntryLoaderService } from '../../providers';

@Component({
    selector: 'gtx-action-log-entry-table',
    templateUrl: './action-log-entry-table.component.html',
    styleUrls: ['./action-log-entry-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LogsTableComponent extends BaseEntityTableComponent<ActionLogEntry, ActionLogEntryBO, LogsListRequest> implements OnInit {

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

    public logTypes = [];

    public logActions = [];

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

    ngOnInit(): void {
        super.ngOnInit();
        this.setDefaultTimeFilter();
        this.init();
    }

    async init(): Promise<void> {
        const [logTypes, logActions] = await Promise.all([
            (this.loader as ActionLogEntryLoaderService).getActionLogTypes(),
            (this.loader as ActionLogEntryLoaderService).getActions(),
        ])
        this.logTypes = logTypes;
        this.logActions = logActions;
        this.changeDetector.markForCheck();
    }

    public valueClearedHandler(): void {
        this.clear();
    }

    public getMinFilterDate(): Date {
        if (this.filters.start) {
            return new Date(this.filters.start * 1000);
        } else {
            return null;
        }
    }

    public getMaxFilterDate(): Date {
        if (this.filters.end) {
            return new Date(this.filters.end * 1000);
        } else {
            return null;
        }
    }

    private clear(): void {
        this.setDefaultTimeFilter();
        this.reload();
    }

    private setDefaultTimeFilter(): void {
        const end = Math.floor(Date.now() / 1000);
        const start = end - 5 * 60;
        this.filters = {
            start,
            end,
        };
    }
}
