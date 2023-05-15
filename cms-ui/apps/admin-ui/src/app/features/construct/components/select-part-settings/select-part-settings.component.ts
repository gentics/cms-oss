import { DataSourceDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { DataSource, IndexById, Raw, SelectSetting } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { generateFormProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-select-part-settings',
    templateUrl: './select-part-settings.component.html',
    styleUrls: ['./select-part-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(SelectPartSettingsComponent)],
})
export class SelectPartSettingsComponent extends BasePropertiesComponent<SelectSetting> implements OnInit, OnDestroy {

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
        super.ngOnInit();

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

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            // eslint-disable-next-line @typescript-eslint/unbound-method
            datasourceId: new UntypedFormControl(null, Validators.required),
            template: new UntypedFormControl(''),
            options: new UntypedFormControl([]),
        });
    }

    protected configureForm(value: SelectSetting, loud?: boolean): void {
        // Nothing
    }

    protected assembleValue(value: SelectSetting): SelectSetting {
        // Nothing
        return value;
    }

    protected override onValueChange(): void {
        if (!this.form || (this.value as any) === CONTROL_INVALID_VALUE) {
            return;
        }
        // If the datasourceId has been changed, we need to reload the entries
        if (this.value?.datasourceId !== this.form.value?.datasourceId) {
            this.loadEntries(this.value?.datasourceId);
        }

        this.form.setValue({
            ...this.form.value,
            // datasourceId: this.value?.datasourceId || null,
            template: this.value?.template || '',
            // options: this.value?.options || [],
        }, { emitEvent: false });
    }

    loadEntries(dsId: string | number): void {
        if (this.entriesSubscription) {
            this.entriesSubscription.unsubscribe();
        }

        if (typeof dsId !== 'number') {
            dsId = parseInt(dsId, 10);
            if (!Number.isInteger(dsId)) {
                dsId = null;
            }
        }

        if (dsId == null) {
            this.form.patchValue({ datasourceId: null, options: [] });
            return;
        }

        /*
         * TODO: Move this to the `DataSourceEntriesOpertations`-Service?
         * Potentially abstract the APi again as EntityOperations should be for BOs only?
         */
        this.entriesSubscription = this.api.dataSource.getEntries(dsId).subscribe(res => {
            this.form.setValue({
                ...this.form.value,
                datasourceId: dsId,
                options: res.items,
            });
            this.form.updateValueAndValidity();
            this.changeDetector.markForCheck();
        });
    }
}
