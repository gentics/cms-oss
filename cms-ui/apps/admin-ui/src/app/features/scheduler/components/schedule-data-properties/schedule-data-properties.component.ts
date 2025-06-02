import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { ScheduleData, ScheduleType } from '@gentics/cms-models';
import { FormProperties, dateInYears, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { pick } from 'lodash-es';

@Component({
    selector: 'gtx-schedule-data-properties',
    templateUrl: './schedule-data-properties.component.html',
    styleUrls: ['./schedule-data-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ScheduleDataPropertiesComponent),
        generateValidatorProvider(ScheduleDataPropertiesComponent),
    ],
    standalone: false
})
export class ScheduleDataPropertiesComponent extends BasePropertiesComponent<ScheduleData> implements OnInit {

    readonly ScheduleType = ScheduleType;
    public dateMin: Date;
    public dateMax: Date;

    public override ngOnInit(): void {
        this.dateMin = dateInYears(-1);
        this.dateMax = dateInYears(2);
        super.ngOnInit();
    }

    @Input()
    public scheduleBlacklist: number[] = [];

    protected override createForm(): FormGroup<FormProperties<ScheduleData>> {
        return new FormGroup<FormProperties<ScheduleData>>({
            type: new FormControl(this.value?.type as any, Validators.required),
            startTimestamp: new FormControl(this.safeValue('startTimestamp')),
            endTimestamp: new FormControl(this.safeValue('endTimestamp')),
            interval: new FormControl(this.safeValue('interval' as any), Validators.required),
            follow: new FormControl(this.safeValue('follow' as any), Validators.required),
        });
    }

    protected override configureForm(value: ScheduleData, loud: boolean = false): void {
        const options = { emitEvent: !!loud };

        const startCtl = this.form.get('startTimestamp');
        startCtl.clearValidators();
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
                startCtl.setValidators(Validators.required);
                endCtl.disable(options);
                intervalCtl.disable(options);
                followCtl.disable(options);
                break;

            default:
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
}
