import { ScheduleDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { FollowUpScheduleData, ScheduleBO } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Component({
    selector: 'gtx-follow-up-schedule-data-properties',
    templateUrl: './follow-up-schedule-data-properties.component.html',
    styleUrls: ['./follow-up-schedule-data-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(FollowUpScheduleDataPropertiesComponent),
        generateValidatorProvider(FollowUpScheduleDataPropertiesComponent),
    ],
})
export class FollowUpScheduleDataPropertiesComponent
    extends BasePropertiesComponent<FollowUpScheduleData>
    implements OnInit, OnChanges {

    public schedules$: Observable<ScheduleBO[]>;

    @Input()
    public scheduleBlacklist: number[] = [];

    protected blacklist$ = new BehaviorSubject<number[]>([]);

    constructor(
        changeDetector: ChangeDetectorRef,
        protected scheduleData: ScheduleDataService,
    ) {
        super(changeDetector);
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.scheduleBlacklist) {
            this.blacklist$.next((this.scheduleBlacklist || [])
                .map(val => Number(val))
                .filter(num => Number.isInteger(num)),
            );
        }
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.schedules$ = combineLatest([
            this.scheduleData.watchAllEntities().pipe(
                map(schedules => schedules.map(singleSchedule => ({
                    ...singleSchedule,
                    id: Number(singleSchedule.id),
                }))),
            ),
            this.blacklist$.pipe(
                distinctUntilChanged(isEqual),
            ),
        ]).pipe(
            map(([schedules, blacklist]: [any[], number[]]) => schedules.filter(singleSchedule => !blacklist.includes(singleSchedule.id))),
        ) as any;
    }

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            scheduleId: new UntypedFormControl((this.value )?.follow?.scheduleId, [Validators.required, Validators.minLength(1)]),
            onlyAfterSuccess: new UntypedFormControl((this.value )?.follow?.onlyAfterSuccess),
        });
    }

    protected override configureForm(value: FollowUpScheduleData, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: FollowUpScheduleData): FollowUpScheduleData {
        return value;
    }
}
