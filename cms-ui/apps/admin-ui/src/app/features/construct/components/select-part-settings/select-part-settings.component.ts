import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { DataSource, DataSourceEntry, IndexById, Raw, SelectOption, SelectSetting } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-select-part-settings',
    templateUrl: './select-part-settings.component.html',
    styleUrls: ['./select-part-settings.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(SelectPartSettingsComponent),
        generateValidatorProvider(SelectPartSettingsComponent),
    ],
    standalone: false
})
export class SelectPartSettingsComponent extends BasePropertiesComponent<SelectSetting> implements OnChanges, OnDestroy {

    @Input()
    public dataSources: DataSource<Raw>[] = [];

    public dataSourceMap: IndexById<DataSource<Raw>> = {};
    public entryMap: Record<number, DataSourceEntry> = {};

    private entriesSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        private api: GcmsApi,
    ) {
        super(changeDetector);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.dataSources) {
            this.dataSourceMap = {};
            (this.dataSources || []).forEach(ds => {
                this.dataSourceMap[ds.id] = ds;
            });
        }
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
        if (!this.form) {
            return;
        }
        // If the datasourceId has been changed, we need to reload the entries
        if (this.value?.datasourceId !== this.form.value?.datasourceId) {
            this.loadEntries(this.value?.datasourceId);
        }

        this.form.patchValue({
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
            this.entryMap = {};
            const options: SelectOption[] = [];

            res.items.forEach(entry => {
                this.entryMap[entry.dsId] = entry;
                options.push({
                    // "id" in the response context is the ID in the global CMS, while dsId is the one inside the Datasource
                    id: entry.dsId,
                    key: entry.key,
                    value: entry.value,
                })
            });

            this.form.patchValue({
                datasourceId: dsId as number,
                options: options,
            }, { emitEvent: false });
            this.form.controls.datasourceId.markAsDirty();
            this.form.controls.options.markAsDirty();

            this.form.updateValueAndValidity();
            this.changeDetector.markForCheck();
        });
    }
}
