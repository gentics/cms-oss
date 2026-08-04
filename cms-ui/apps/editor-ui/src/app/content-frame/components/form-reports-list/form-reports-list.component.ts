import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnChanges, OnDestroy, OnInit, SimpleChanges, input, signal } from '@angular/core';
import { API_BASE_URL, I18nNotificationService, I18nService, downloadFromBlob } from '@gentics/cms-components';
import {
    Form,
    FormDataListElement,
    FormDataListEntry,
    FormDownloadInfo,
    FormElement,
    FormTypeConfiguration,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { ModalService, TableAction, TableActionClickEvent, TableColumn, TableRow } from '@gentics/ui-core';
import { BehaviorSubject, Subscription, combineLatest, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { dateToFileSystemString } from '../../../common/utils/date-to-string';
import { ApplicationStateService } from '../../../state';
import { SimpleDeleteModalComponent } from '../simple-delete-modal/simple-delete-modal.component';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';

const STATUS_POLL_INTERVAL_MS = 2_000;

enum ValueType {
    STRING = 'string',
    NUMBER = 'number',
    BOOLEAN = 'boolean',
    BINARY = 'binary',
}

interface StringValue {
    type: ValueType.STRING;
    value: string;
}

interface NumberValue {
    type: ValueType.NUMBER;
    value: number;
}

interface BooleanValue {
    type: ValueType.BOOLEAN;
    value: boolean;
}

interface BinaryValue {
    type: ValueType.BINARY;
    fileName: string;
    link: string;
}

type FormValue = StringValue | NumberValue | BooleanValue | BinaryValue;

interface DisplayItem {
    uuid: string;
    values: Record<string, FormValue | FormValue[]>;
}

const ACTION_DELETE = 'delete';

@Component({
    selector: 'form-reports-list',
    templateUrl: './form-reports-list.component.html',
    styleUrls: ['./form-reports-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormReportsListComponent implements OnInit, OnChanges, OnDestroy {

    public readonly ValueType = ValueType;

    public readonly form = input.required<Form>();
    public readonly formConfig = input.required<FormTypeConfiguration>();

    public readonly page = signal<number>(1);
    public readonly pageSize = signal<number>(15);
    public readonly totalCount = signal<number>(0);

    public readonly columns = signal<TableColumn<DisplayItem>[]>([]);
    public readonly rows = signal<TableRow<DisplayItem>[]>([]);
    public readonly actions = signal<TableAction<DisplayItem>[]>([]);

    public readonly selected = signal<Set<string>>(new Set());

    public binaryStatus: FormDownloadInfo;
    public exportStatus: FormDownloadInfo;

    public readonly loadingStatus = signal<boolean>(false);
    public readonly loadingReport = signal<boolean>(false);
    public readonly loadingAction = signal<boolean>(false);

    public tableError: string | null = null;

    protected subscriptions: Subscription[] = [];

    private formElementMap = new Map<string, FormElement>();

    constructor(
        private client: GCMSRestClientService,
        private appState: ApplicationStateService,
        private changeDetector: ChangeDetectorRef,
        private modalService: ModalService,
        private notification: I18nNotificationService,
        private i18n: I18nService,
        private errorHandler: ErrorHandler,
    ) { }

    public ngOnInit(): void {
        const statusLoader$ = new BehaviorSubject<void>(null);

        this.subscriptions.push(statusLoader$.pipe(
            switchMap(() => combineLatest([
                this.client.form.binariesStatus(this.form().id),
                this.client.form.exportStatus(this.form().id),
            ])),
        ).subscribe(([binaryStatus, exportStatus]) => {
            this.binaryStatus = binaryStatus;
            this.exportStatus = exportStatus;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(interval(30_000).subscribe(() => statusLoader$.next()));
        statusLoader$.next();

        this.actions.set([
            {
                id: ACTION_DELETE,
                enabled: true,
                icon: 'delete',
                label: this.i18n.instant('common.delete_button'),
                multiple: true,
                single: true,
                type: 'alert',
            },
        ]);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.form) {
            this.rebuildMaps();
        }
        if (changes.form || changes.formConfig) {
            this.reload();
        }
    }

    public ngOnDestroy(): void {
        this.subscriptions.forEach((s) => s.unsubscribe());
    }

    public rebuildMaps(): void {
        this.formElementMap.clear();

        if (this.form() == null) {
            return;
        }

        (this.form().data['ui-schema']?.pages || []).forEach((page) => {
            this.addElementsToMap(page.elements);
        });
    }

    protected addElementsToMap(elements: FormElement[]): void {
        for (const el of elements) {
            this.formElementMap.set(el.id, el);

            if (Array.isArray(el.elements)) {
                this.addElementsToMap(el.elements);
            }
        }
    }

    public handleActionClick(event: TableActionClickEvent<DisplayItem>): void {
        if (event.actionId !== ACTION_DELETE) {
            return;
        }

        if (event.selection) {
            this.deleteMultipleReports();
        } else {
            this.deleteSingleReport(event.item.uuid);
        }
    }

    public handlePageChange(page: number): void {
        this.loadPage(page);
    }

    public handleSelectedChange(newSelection: string[]): void {
        this.selected.set(new Set(newSelection));
    }

    protected loadPage(page: number): void {
        if (this.loadingReport() || !this.form() || !this.formConfig()) {
            return;
        }

        this.loadingReport.set(true);
        this.page.set(page);

        this.subscriptions.push(this.client.form.listData(
            this.form().id,
            {
                page: this.page(),
                pageSize: this.pageSize(),
            },
        ).subscribe({
            next: (res) => {
                this.tableError = null;

                // Make sure everything for paging is in sync
                this.totalCount.set(res.totalCount);
                this.page.set(res.currentPage);
                this.pageSize.set(res.perPage);

                // We only want to create the columns once on the first load.
                if (this.columns().length === 0 && Object.keys(res.elements).length > 0) {
                    this.createColumns(res.elements);
                }

                const items = res.entries.map((entry) => this.mapToDisplayItem(entry));
                this.rows.set(items.map((item) => {
                    return {
                        id: item.uuid,
                        item,
                    };
                }));
                this.changeDetector.markForCheck();
            },
            error: (err) => {
                if (err.statusCode === 403) {
                    this.tableError = this.i18n.instant('editor.reports_loading_permission_error');
                } else {
                    this.tableError = this.i18n.instant('editor.reports_loading_status_error');
                }
                this.changeDetector.markForCheck();
            },
            complete: () => {
                this.loadingReport.set(false);
                this.changeDetector.markForCheck();
            },
        }));
    }

    protected createColumns(elements: Record<string, FormDataListElement>): void {
        const cols: TableColumn<DisplayItem>[] = Object.entries(elements).map(([id, el]) => {
            const formEl = this.formElementMap.get(id);
            const controlConf = this.formConfig().controls[el.type];
            const labelObj = formEl?.label ?? controlConf?.labelI18n;

            if (!labelObj) {
                console.warn(`Could not determine the label for column with ID "${id}"!`);
            }

            return {
                id,
                label: this.i18n.fromObject(labelObj ?? {}),
                fieldPath: ['values', id],
            };
        });

        this.columns.set(cols);
    }

    protected mapToDisplayItem(entry: FormDataListEntry): DisplayItem {
        const values: Record<string, FormValue | FormValue[]> = {};

        for (const [id, data] of Object.entries(entry.fields)) {
            values[id] = this.mapToValue(entry.uuid, id, data);
        }

        return {
            uuid: entry.uuid,
            values,
        };
    }

    protected mapToValue(entryUuid: string, id: string, data: any): FormValue | FormValue[];
    protected mapToValue(entryUuid: string, id: string, data: any, index: number): FormValue;
    protected mapToValue(entryUuid: string, id: string, data: any, index?: number): FormValue | FormValue[] {
        if (data == null) {
            return null;
        }

        const dataType = typeof data;
        switch (dataType) {
            case 'string':
            case 'number':
            case 'boolean':
                return {
                    type: dataType as any,
                    value: data,
                };
            case 'object':
                break;
            default:
                return null;
        }

        if (Array.isArray(data)) {
            return data.map((value, idx) => this.mapToValue(entryUuid, id, value, idx));
        }

        let fileName: string | null = null;
        if ('fileName' in data) {
            fileName = data.fileName;
        }
        if ('fields' in data) {
            fileName = data.fields?.binary?.fileName;
        }

        if (fileName) {
            let link = `${API_BASE_URL}/form/${this.form().id}/data/${entryUuid}/binary/${id}`;

            if (index != null) {
                link += `/${index}`;
            }

            return {
                type: ValueType.BINARY,
                fileName,
                link: `${link}`,
            };
        }

        return null;
    }

    public generateBinaryDownload(): void {
        this.notification.show({
            message: 'editor.form_reports_binary_generate_started',
            type: 'secondary',
        });

        this.subscriptions.push(
            this.client.form.createBinaries(this.form().id).pipe(
                switchMap(() => interval(STATUS_POLL_INTERVAL_MS).pipe(
                    switchMap(() => this.client.form.binariesStatus(this.form().id)),
                    takeWhile((status) => status.requestPending, true),
                )),
            ).subscribe((status) => {
                if (status.downloadReady && !status.requestPending) {
                    this.notification.show({
                        message: 'editor.form_reports_binary_generate_finished',
                        type: 'success',
                    });
                } else if (!status.requestPending && status.error) {
                    this.notification.show({
                        message: 'editor.form_reports_binary_generate_failed',
                        type: 'alert',
                    });
                }

                this.binaryStatus = status;
                this.changeDetector.markForCheck();
            }));
    }

    public downloadBinaries(): void {
        if (!this.binaryStatus?.downloadReady) {
            return;
        }
        this.notification.show({
            message: 'editor.form_reports_download_started',
            type: 'success',
        });

        this.subscriptions.push(this.client.form.downloadData(this.form().id, this.binaryStatus.downloadUuid).subscribe((blob) => {
            const time = new Date(this.binaryStatus.downloadTimestamp);
            downloadFromBlob(blob, `form_${this.form().id}_binaries_${dateToFileSystemString(time)}`);
        }));
    }

    public generateExportDownload(): void {
        this.notification.show({
            message: 'editor.form_reports_csv_generate_started',
            type: 'secondary',
        });

        this.subscriptions.push(
            this.client.form.createExport(this.form().id, { lang: this.i18n.getCurrentLanguage() }).pipe(
                switchMap(() => interval(STATUS_POLL_INTERVAL_MS).pipe(
                    switchMap(() => this.client.form.exportStatus(this.form().id)),
                    takeWhile((status) => status.requestPending, true),
                )),
            ).subscribe((status) => {
                if (status.downloadReady && !status.requestPending) {
                    this.notification.show({
                        message: 'editor.form_reports_csv_generate_finished',
                        type: 'success',
                    });
                } else if (!status.requestPending && status.error) {
                    this.notification.show({
                        message: 'editor.form_reports_csv_generate_failed',
                        type: 'alert',
                    });
                }

                this.exportStatus = status;
                this.changeDetector.markForCheck();
            }),
        );
    }

    public downloadExport(): void {
        if (!this.exportStatus?.downloadReady) {
            return;
        }
        this.notification.show({
            message: 'editor.form_reports_download_started',
            type: 'success',
        });

        this.subscriptions.push(this.client.form.downloadData(this.form().id, this.exportStatus.downloadUuid).subscribe((blob) => {
            const time = new Date(this.exportStatus.downloadTimestamp);
            downloadFromBlob(blob, `form_${this.form().id}_export_${dateToFileSystemString(time)}`);
        }));
    }

    deleteSingleReport(uuid: string): void {
        this.modalService.fromComponent(SimpleDeleteModalComponent, null, {
            items: [{ uuid: uuid }],
            itemType: 'form_report',
            idProperty: 'uuid',
            iconString: 'list_alt',
        })
            .then((modal) => modal.open())
            .then(() => {
                this.loadingAction.set(true);
                return this.client.form.deleteData(this.form().id, uuid).toPromise();
            })
        // display toast notification
            .then(() => this.notification.show({
                type: 'success',
                message: 'message.report_successfully_removed_singular',
            }))
            .catch((error) => this.errorHandler.catch(error, { notification: true }))
        // clean up
            .finally(() => {
                this.loadingAction.set(false);
                this.reload();
            });
    }

    deleteMultipleReports(): void {
        if (this.selected().size === 0) {
            return;
        }

        const selected = Array.from(this.selected());
        const selectedItems = selected.map((uuid) => ({ uuid }));

        this.modalService.fromComponent(SimpleDeleteModalComponent, null, {
            items: selectedItems,
            itemType: 'form_report',
            idProperty: 'uuid',
            iconString: 'list_alt',
        })
            .then((modal) => modal.open())
            .then(() => {
                this.loadingAction.set(true);
                const requests = selected.map((id) => this.client.form.deleteData(this.form().id, id)
                    .toPromise()
                    .catch((error) => this.errorHandler.catch(error, { notification: true })),
                );
                return Promise.all(requests);
            })
        // display toast notification
            .then(() => this.notification.show({
                type: 'success',
                message: 'message.report_successfully_removed_plural',
                translationParams: { count: selected.length },
            }))
            .catch((error) => this.errorHandler.catch(error, { notification: true }))
        // clean up
            .finally(() => {
                this.loadingAction.set(false);
                this.reload();
                this.selected.set(new Set());
            });
    }

    private reload(): void {
        this.loadPage(this.page());
    }
}
