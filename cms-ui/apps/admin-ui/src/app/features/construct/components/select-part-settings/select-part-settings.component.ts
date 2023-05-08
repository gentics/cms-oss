import { DataSourceDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { DataSource, IndexById, Raw, SelectSetting } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest, Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-select-part-settings',
    templateUrl: './select-part-settings.component.html',
    styleUrls: ['./select-part-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SelectPartSettingsComponent)],
})
export class SelectPartSettingsComponent extends BaseFormElementComponent<SelectSetting> implements OnInit, OnDestroy {

    public form: UntypedFormGroup;
    public dataSourceMap: IndexById<DataSource<Raw>> = {};

    private entriesSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        private dataSourceData: DataSourceDataService,
        private api: GcmsApi,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            // eslint-disable-next-line @typescript-eslint/unbound-method
            dataSourceId: new UntypedFormControl(null, Validators.required),
            template: new UntypedFormControl(''),
            options: new UntypedFormControl([]),
        });

        this.subscriptions.push(combineLatest([
            this.form.valueChanges,
            this.form.statusChanges,
        ]).pipe(
            map(([value, status]) => status === 'VALID' ? value : null),
            distinctUntilChanged(isEqual),
        ).subscribe(value => {
            this.triggerChange(value);
            this.changeDetector.markForCheck();
        }))

        this.subscriptions.push(this.dataSourceData.watchAllEntities().subscribe(dataSources => {
            this.dataSourceMap = {};
            dataSources.forEach((ds: any) => {
                // the dataSource-data service converts the ids to strings.
                // the select does strict checking however, so we have to convert them back to numbers
                ds.id = Number(ds.id);
                this.dataSourceMap[ds.id] = ds;
            });
        }));
    }

    ngOnDestroy(): void {
        if (this.entriesSubscription) {
            this.entriesSubscription.unsubscribe();
        }
    }

    protected onValueChange(): void {
        // If the dataSourceId has been changed, we need to reload the entries
        if (this.value?.datasourceId !== this.form.value?.dataSourceId) {
            this.loadEntries(this.value?.datasourceId);
        }

        this.form.setValue({
            dataSourceId: this.value?.datasourceId || null,
            template: this.value?.template || '',
            options: this.value?.options || [],
        }, { emitEvent: false });
    }

    loadEntries(dsId: string | number): void {
        if (this.entriesSubscription) {
            this.entriesSubscription.unsubscribe();
        }

        if (typeof dsId === 'number') {
            dsId = (dsId < 1) ? null : String(dsId);
        }

        if (dsId == null) {
            this.form.patchValue({ dataSourceId: null, options: [] });
            return;
        }

        /*
         * TODO: Move this to the `DataSourceEntriesOpertations`-Service?
         * Potentially abstract the APi again as EntityOperations should be for BOs only?
         */
        this.entriesSubscription = this.api.dataSource.getEntries(dsId).subscribe(res => {
            this.form.patchValue({ dataSourceId: dsId, options: res.items });
        });
    }

    setDisabledState(isDisabled: boolean): void {
        super.setDisabledState(isDisabled);

        if (isDisabled) {
            this.form.disable({ emitEvent: false });
        } else {
            this.form.enable({ emitEvent: false });
        }
    }
}
