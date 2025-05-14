import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { DataSourceEntry, ModelType, Raw } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

const MAX_KEY_LENGTH = 50;
const MAX_VALUE_LENGTH = 16383;

@Component({
    selector: 'gtx-data-source-entry-properties',
    templateUrl: './data-source-entry-properties.component.html',
    styleUrls: ['./data-source-entry-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(DataSourceEntryPropertiesComponent),
        generateValidatorProvider(DataSourceEntryPropertiesComponent),
    ],
    standalone: false
})
export class DataSourceEntryPropertiesComponent extends BasePropertiesComponent<DataSourceEntry<Raw>> {

    public readonly MAX_KEY_LENGTH = MAX_KEY_LENGTH;
    public readonly MAX_VALUE_LENGTH = MAX_VALUE_LENGTH;

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            key: new UntypedFormControl(this.safeValue('key'), [
                Validators.maxLength(MAX_KEY_LENGTH),
                Validators.required,
            ]),
            value: new UntypedFormControl(this.safeValue('value'), [
                Validators.maxLength(MAX_VALUE_LENGTH),
                Validators.required,
            ]),
        });
    }

    protected configureForm(value: DataSourceEntry<ModelType.Raw>, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: DataSourceEntry<ModelType.Raw>): DataSourceEntry<ModelType.Raw> {
        return value;
    }
}
