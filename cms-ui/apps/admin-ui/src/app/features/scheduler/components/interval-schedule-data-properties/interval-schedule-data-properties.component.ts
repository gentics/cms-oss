import { ChangeDetectionStrategy, Component } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { IntervalScheduleData, IntervalUnit } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-interval-schedule-data-properties',
    templateUrl: './interval-schedule-data-properties.component.html',
    styleUrls: ['./interval-schedule-data-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(IntervalScheduleDataPropertiesComponent),
        generateValidatorProvider(IntervalScheduleDataPropertiesComponent),
    ],
    standalone: false
})
export class IntervalScheduleDataPropertiesComponent extends BasePropertiesComponent<IntervalScheduleData> {

    readonly IntervalUnit = IntervalUnit;

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            value: new UntypedFormControl(this.safeValue('interval')?.value, Validators.required),
            unit: new UntypedFormControl(this.safeValue('interval')?.unit, Validators.required),
        }, Validators.required);
    }

    protected override configureForm(value: IntervalScheduleData, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: IntervalScheduleData): IntervalScheduleData {
        return value;
    }

}
