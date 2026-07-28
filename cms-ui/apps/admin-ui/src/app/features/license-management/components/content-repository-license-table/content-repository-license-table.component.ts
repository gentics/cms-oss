import { ContentRepositoryLicenseBO } from '@admin-ui/common';
import { ErrorHandler } from '@admin-ui/core';
import { BaseEntityTableComponent } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { License, PushLicenseRequest } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ChangesOf, ModalService, TableAction, TableActionClickEvent, TableColumn, TableRow } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
import { map, Observable } from 'rxjs';
import { ContentRepositoryLicenseTableLoaderService } from '../../providers';
import { ContentRepositoryLicenseInfoModal } from '../content-repository-license-info-modal/content-repository-license-info-modal.component';

const ACTION_PUSH = 'push';

@Component({
    selector: 'gtx-content-repository-license-table',
    templateUrl: './content-repository-license-table.component.html',
    styleUrls: ['./content-repository-license-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class ContentRepositoryLicenseTableComponent
    extends BaseEntityTableComponent<ContentRepositoryLicenseBO>
    implements OnChanges {

    @Input()
    public compareLicense = false;

    @Input()
    public license: License | null = null;

    protected rawColumns: TableColumn<ContentRepositoryLicenseBO>[] = [
        {
            id: 'name',
            label: 'common.name',
            fieldPath: 'name',
            sortable: true,
            clickable: true,
        },
        {
            id: 'url',
            label: 'content_repository.url',
            fieldPath: 'url',
            sortable: true,
            clickable: true,
        },
        {
            id: 'openSource',
            label: 'license.openSource',
            fieldPath: 'openSource',
            sortable: false,
            clickable: true,
        },
        {
            id: 'status',
            label: 'license.status',
            sortable: false,
            clickable: true,
        },
    ];

    protected entityIdentifier = null;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ContentRepositoryLicenseTableLoaderService,
        modalService: ModalService,
        private client: GCMSRestClientService,
        private notifications: I18nNotificationService,
        private errorHandler: ErrorHandler,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);
        if (changes.license) {
            this.actionRebuildTrigger.next();
        }
    }

    public handlePushAll(): void {
        this.pushLicense({ all: true });
    }

    public override handleRowClick(row: TableRow<ContentRepositoryLicenseBO>): void {
        // If the CR has no license, then we can't display it either
        if (!row.item.license) {
            return;
        }

        this.modalService.fromComponent(ContentRepositoryLicenseInfoModal, {}, {
            info: row.item,
        }).then((modal) => modal.open());
    }

    public override handleAction(event: TableActionClickEvent<ContentRepositoryLicenseBO>): void {
        switch (event.actionId) {
            case ACTION_PUSH:
                this.pushLicense({
                    crIds: this.getAffectedEntityIds(event)
                        .map(Number)
                        .filter((id) => Number.isInteger(id)),
                }, true);
                break;

            default:
                super.handleAction(event);
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<ContentRepositoryLicenseBO>[]> {
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<ContentRepositoryLicenseBO>[] = [
                    {
                        id: ACTION_PUSH,
                        type: 'warning',
                        icon: 'publish',
                        label: this.i18n.instant('license.push_license_to_cr_button'),
                        enabled: (cr) => this.license != null && (cr == null || !cr.openSource),
                        single: true,
                        multiple: true,
                    },
                ];

                return actions;
            }),
        );
    }

    protected pushLicense(req: PushLicenseRequest, clearSelection: boolean = false): void {
        this.subscriptions.push(this.client.license.push(req).subscribe({
            next: (res) => {
                if (clearSelection) {
                    this.updateSelection([]);
                }
                this.reload();

                this.notifications.show({
                    message: res.messages?.[0]?.message || res?.responseInfo?.responseMessage,
                    type: 'success',
                });
            },
            error: (err) => {
                this.errorHandler.catch(err);
            },
        }));
    }
}
