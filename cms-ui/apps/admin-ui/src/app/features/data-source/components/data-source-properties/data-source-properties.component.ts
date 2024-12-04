import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { AnyModelType, DataSource } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-data-source-properties',
    templateUrl: './data-source-properties.component.html',
    styleUrls: ['./data-source-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(DataSourcePropertiesComponent),
        generateValidatorProvider(DataSourcePropertiesComponent),
    ],
})
export class DataSourcePropertiesComponent extends BasePropertiesComponent<DataSource> {

    protected createForm(): FormGroup<any> {
        return new UntypedFormGroup({
            name: new UntypedFormControl(this.safeValue('name'), [Validators.required, Validators.maxLength(50)]),
        });
    }

    protected configureForm(value: DataSource<AnyModelType>, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: DataSource<AnyModelType>): DataSource<AnyModelType> {
        return value;
    }
}
