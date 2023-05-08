import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, createNestedControlValidator } from '@gentics/cms-components';
import { FollowUpScheduleData, IntervalScheduleData, ScheduleData, ScheduleType } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { pick } from 'lodash';

@Component({
    selector: 'gtx-schedule-data-properties',
    templateUrl: './schedule-data-properties.component.html',
    styleUrls: ['./schedule-data-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ScheduleDataPropertiesComponent)],
})
export class ScheduleDataPropertiesComponent extends BasePropertiesComponent<ScheduleData> implements OnInit {

    readonly ScheduleType = ScheduleType;

    @Input()
    public scheduleBlacklist: number[] = [];

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            type: new UntypedFormControl(this.value?.type, Validators.required),
            startTimestamp: new UntypedFormControl(this.value?.startTimestamp),
            endTimestamp: new UntypedFormControl(this.value?.endTimestamp),
            interval: new UntypedFormControl((this.value as IntervalScheduleData)?.interval, [Validators.required, createNestedControlValidator()]),
            follow: new UntypedFormControl((this.value as FollowUpScheduleData)?.follow, [Validators.required, createNestedControlValidator()]),
        });
    }

    protected override configureForm(value: ScheduleData, loud: boolean = false): void {
        const options = { emitEvent: !!loud };

        const startCtl = this.form.get('startTimestamp');
        const endCtl = this.form.get('endTimestamp');
        const intervalCtl = this.form.get('interval');
        const followCtl = this.form.get('follow');

        switch (value?.type) {
            case ScheduleType.INTERVAL:
                startCtl.enable(options);
                endCtl.enable(options);
                intervalCtl.enable(options);
                followCtl.disable(options);
                break;

            case ScheduleType.FOLLOW_UP:
                startCtl.enable(options);
                endCtl.enable(options);
                followCtl.enable(options);
                intervalCtl.disable(options);
                break;

            case ScheduleType.ONCE:
                startCtl.enable(options);
                endCtl.disable(options);
                intervalCtl.disable(options);
                followCtl.disable(options);
                break;

            case ScheduleType.MANUAL:
                startCtl.disable(options);
                endCtl.disable(options);
                intervalCtl.disable(options);
                followCtl.disable(options);
                break;
        }
    }

    protected override assembleValue(value: ScheduleData): ScheduleData {
        switch (value.type) {
            case ScheduleType.INTERVAL:
                return pick(value, ['type', 'startTimestamp', 'endTimestamp', 'interval']);

            case ScheduleType.FOLLOW_UP:
                return pick(value, ['type', 'startTimestamp', 'endTimestamp', 'follow']);

            case ScheduleType.MANUAL:
                return pick(value, ['type']);

            case ScheduleType.ONCE:
                return pick(value, ['type', 'startTimestamp']);

            default:
                return value;
        }
    }

    protected override onValueChange(): void {
        if (this.form) {
            this.form.setValue({
                type: null,
                startTimestamp: null,
                endTimestamp: null,
                interval: null,
                follow: null,
                ...this.value,
            });
        }
    }
}
