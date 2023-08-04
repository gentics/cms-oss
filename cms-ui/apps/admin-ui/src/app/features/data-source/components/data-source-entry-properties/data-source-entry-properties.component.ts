import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { DataSourceEntry, ModelType, Raw } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-data-source-entry-properties',
    templateUrl: './data-source-entry-properties.component.html',
    styleUrls: ['./data-source-entry-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(DataSourceEntryPropertiesComponent)],
})
export class DataSourceEntryPropertiesComponent extends BasePropertiesComponent<DataSourceEntry<Raw>> {

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            key: new UntypedFormControl((this.value || {}).key, Validators.required),
            value: new UntypedFormControl((this.value || {}).value),
        });
    }

    protected configureForm(value: DataSourceEntry<ModelType.Raw>, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: DataSourceEntry<ModelType.Raw>): DataSourceEntry<ModelType.Raw> {
        return value;
    }
}
