import { animate, state, style, transition, trigger } from '@angular/animations';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import {
    Form,
    FormDataListEntry,
    FormDataListResponse,
    FormElementLabelPropertyI18nValues,
    GcmsUiLanguage,
} from '@gentics/cms-models';
import { FormEditorService, FormReportService } from '@gentics/form-generator';
import { ModalService } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { PaginationInstance } from 'ngx-pagination';
import { BehaviorSubject, combineLatest, Observable, Subject, throwError } from 'rxjs';
import { catchError, finalize, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { API_BASE_URL } from '../../../common/utils/base-urls';
import { Api } from '../../../core/providers/api';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { ApplicationStateService } from '../../../state';
import { SimpleDeleteModalComponent } from '../simple-delete-modal/simple-delete-modal.component';

@Component({
    selector: 'form-reports-list',
    templateUrl: './form-reports-list.component.html',
    styleUrls: ['./form-reports-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
    trigger('fadeAnim', [
        state('in', style({
            opacity: 1,
        })),
        transition(':enter', [
            style({
                opacity: 0,
            }),
            animate(100),
        ]),
        transition(':leave',
            animate(100, style({
                opacity: 0,
                }),
            )),
        ]),
    ],
})
export class FormReportsListComponent implements OnInit {

    private _form: Form;

    get form(): Form {
        return this._form;
    }

    @Input() set form(value: Form) {
        this._form = value;
        this.formElementLabelPropertyI18nValues$ = this.formReportService.getFormElementLabelPropertyValues(value);
    }

    icon = 'list';
    selected: string[] = [];
    allSelected = false;

    loading$ = new BehaviorSubject<boolean>(false);

    loadingError$ = new BehaviorSubject<boolean>(false);

    currentPage$ = new BehaviorSubject<number>(1);

    paginationConfig: PaginationInstance = {
        itemsPerPage: 25,
        currentPage: 1,
    };

    pathForBinaries: string;
    pathForCSV: string;
    sid: number;

    isDisabledDownloadBinariesButton = true;
    isDisabledDownloadCsvButton = true;
    errorMessage: string;

    result: FormDataListResponse;

    entryKeys = [];
    entryValues = [];

    private reload$ = new Subject<void>();
    private destroyed$ = new Subject<void>();

    public formElementLabelPropertyI18nValues$: Observable<FormElementLabelPropertyI18nValues>;

    constructor(
        private api: Api,
        private appState: ApplicationStateService,
        private changeDetector: ChangeDetectorRef,
        private modalService: ModalService,
        private notification: I18nNotification,
        private formEditorService: FormEditorService,
        private formReportService: FormReportService,
        private entityResolver: EntityResolver,
        private translation: TranslateService,
    ) { }

    ngOnInit(): void {

        this.definePageSizeByClientHeight();

        this.appState.select(state => state.auth.sid).pipe(
            takeUntil(this.destroyed$),
        ).subscribe(sid => {
            this.sid = sid;
            this.pathForBinaries = `${API_BASE_URL}/form/${this.form.id}/binaries?sid=${sid}`;
            this.pathForCSV = `${API_BASE_URL}/form/${this.form.id}/export?sid=${sid}`;
        });

        combineLatest([
            this.currentPage$,
            this.reload$,
        ]).pipe(
            switchMap(([currentPage]) => this.getReports(currentPage)),
            takeUntil(this.destroyed$),
        ).subscribe((res: FormDataListResponse) => {
            this.result = res;

            this.entryValues = [];

            this.result.entries.forEach(entry => {
                delete entry.fields.errors;
                this.entryKeys = Object.keys(entry.fields);
                this.entryValues.push(Object.values(entry.fields));
            });

            this.paginationConfig.totalItems = this.result.totalCount;

            for (let index = 0; index < this.result.entries.length; index++) {
                this.selected[index] = null;
            }

            this.changeDetector.detectChanges();
        });

        this.appState.select(state => state.ui.language).subscribe((language: GcmsUiLanguage) => {
            /**
             * We need to set the language manually in the form editor service.
             * (This is normally done in the form editor component. However, it is not used here).
             */
            this.formEditorService.activeUiLanguageCode = language;
        });

        this.appState.select(state => state.folder.activeFormLanguage).pipe(
            map(languageId => this.entityResolver.getLanguage(languageId)),
        ).subscribe(language => {
            this.formEditorService.activeContentLanguageCode = language.code;
        });

        this.reload();
    }

    public isFile(entryValue: any): boolean {
        return entryValue.hasOwnProperty('fileName');
    }

    public getEntryValue(entryValue: any): string {
        if (entryValue == null) {
            return '';
        }

        if (entryValue.hasOwnProperty('fileName')) {
            return entryValue.fileName;
        }

        return entryValue;
    }

    public getFileHref(entryValue: any, entry: FormDataListEntry): string {
        const field = Object.entries(entry.fields).find(([key, value]) => {
            return Object.values(value).includes(entryValue.fileName);
        })[0];
        return `${API_BASE_URL}/form/${this.form.id}/data/${entry.uuid}/binary/${field}?sid=${this.sid}`;
    }

    getReports(pageIndex: number): Observable<FormDataListResponse> {
        this.loading$.next(true);
        return this.api.forms.getReports(
            this.form.id,
            {
                page: pageIndex,
                pageSize: this.paginationConfig.itemsPerPage,
            },
        ).pipe(
            tap((form: FormDataListResponse) => {
                if (form.entries.length > 0) {
                    this.isDisabledDownloadCsvButton = false;
                    if (form.elements && Object.values(form.elements).find(element => element.type === 'file')) {
                        this.isDisabledDownloadBinariesButton = false;
                    }
                }
            }),
            catchError(err => {
                if (err.statusCode === 403) {
                    this.errorMessage = this.translation.instant('editor.reports_loading_permission_error');
                } else {
                    this.errorMessage = this.translation.instant('editor.reports_loading_status_error');
                }
                this.loadingError$.next(true);
                this.paginationConfig.totalItems = 0;
                return throwError(err);
            }),
            finalize(() => this.loading$.next(false)),
        );
    }

    deleteSingleReport(index: number): void {
        this.modalService.fromComponent(SimpleDeleteModalComponent, null, {
            items: [this.result.entries[index]],
            itemType: 'editor.form_reports_label',
            idProperty: 'uuid',
            iconString: 'list_alt',
        })
            .then(modal => modal.open())
            .then(() => {
                this.loading$.next(true);
                return this.api.forms.deleteReport(this.form.id, this.result.entries[index].uuid).toPromise();
            })
        // display toast notification
            .then(() => this.notification.show({
                type: 'success',
                message: 'message.report_successfully_removed_singular',
            }))
            .catch(error => this.notification.show({
                type: 'alert',
                message: error,
            }))
        // clean up
            .finally(() => {
                this.reload();
                this.loading$.next(false);
            });
    }

    deleteMultipleReports(): void {
        const selected = this.selected.filter(s => !!s);
        const selectedItems: FormDataListEntry[] = this.result.entries.filter(s => this.selected.includes(s.uuid));
        this.modalService.fromComponent(SimpleDeleteModalComponent, null, {
            items: selectedItems,
            itemType: 'editor.form_reports_label',
            idProperty: 'uuid',
            iconString: 'list_alt',
        })
            .then(modal => modal.open())
            .then(() => {
                this.loading$.next(true);
                const requests = selected.map(id => this.api.forms.deleteReport(this.form.id, id)
                    .toPromise()
                    .catch(error => this.notification.show({
                        type: 'alert',
                        message: error,
                    })),
                );
                return Promise.all(requests);
            })
        // display toast notification
            .then(() => this.notification.show({
                type: 'success',
                message: 'message.report_successfully_removed_plural',
                translationParams: { count: selected.length },
            }))
        // clean up
            .finally(() => {
                this.reload();
                this.loading$.next(false);
                this.allSelected = false;
            });
    }

    isNothingSelected(): boolean {
        return this.selected.every(element => element === null);
    }

    isValidString(entry: any): boolean {
        return typeof(entry) === 'string' && entry.length > 0;
    }

    isEmptyString(entry: any): boolean {
        return entry === '';
    }

    hasErrors(): boolean {
        return this.result.entries.some(e => this.isValidString(e.fields.errors));
    }

    /**
     * Tracking function for ngFor for better performance.
     */
    identify(index: number, item: FormDataListEntry): string {
        return item.uuid;
    }

    pageChanged(newPageNumber: number): void {
        this.definePageSizeByClientHeight();
        this.allSelected = false;
        this.paginationConfig.currentPage = newPageNumber;
        this.currentPage$.next(newPageNumber);
    }

    toggleAllSelect(): void {
        this.allSelected = !this.allSelected;

        for (let index = 0; index < this.result.entries.length; index++) {
            if (this.allSelected) {
                this.selected[index] = this.result.entries[index].uuid;
            } else {
                this.selected[index] = null;
            }
        }

    }

    changeSelection(index: number): void {
        for (let i = 0; i < this.result.entries.length; i++) {
            if (i === index) {
                if (this.selected[index]) {
                    this.selected[index] = null;
                } else {
                    this.selected[index] = this.result.entries[index].uuid;
                }
            }
        }
    }

    private reload(): void {
        this.formElementLabelPropertyI18nValues$.pipe(
            takeUntil(this.destroyed$),
        ).subscribe(formElementLabelPropertyI18nValues => {
            if (formElementLabelPropertyI18nValues && Object.keys(formElementLabelPropertyI18nValues).length > 0) {
                this.reload$.next();
            }
        })
    }

    /**
     * Dynamically calculate pageSize by client height.
     */
    private definePageSizeByClientHeight(): void {
        this.paginationConfig.itemsPerPage = Math.floor(document.body.clientHeight / 60);
    }
}
