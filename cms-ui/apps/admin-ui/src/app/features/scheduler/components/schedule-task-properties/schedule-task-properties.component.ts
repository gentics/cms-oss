import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { AnyModelType, ScheduleTask } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum ScheduleTaskPropertiesMode {
    UPDATE = 'update',
    CREATE = 'create',
}

@Component({
    selector: 'gtx-schedule-task-properties',
    templateUrl: './schedule-task-properties.component.html',
    styleUrls: ['./schedule-task-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ScheduleTaskPropertiesComponent),
        generateValidatorProvider(ScheduleTaskPropertiesComponent),
    ],
})
export class ScheduleTaskPropertiesComponent extends BasePropertiesComponent<ScheduleTask> {

    public readonly ScheduleTaskPropertiesMode = ScheduleTaskPropertiesMode;

    @Input()
    public mode: ScheduleTaskPropertiesMode = ScheduleTaskPropertiesMode.UPDATE;

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            name: new UntypedFormControl(this.value?.name || '', Validators.required),
            description: new UntypedFormControl(this.value?.description || ''),
            command: new UntypedFormControl(this.value?.command || '', Validators.required),
        });
    }

    protected override configureForm(value: ScheduleTask<AnyModelType>, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: ScheduleTask<AnyModelType>): ScheduleTask<AnyModelType> {
        return value;
    }
}
