import { ScheduleTaskDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { AnyModelType, Schedule, ScheduleTaskBO } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export enum SchedulePropertiesMode {
    UPDATE = 'update',
    CREATE = 'create',
}

@Component({
    selector: 'gtx-schedule-properties',
    templateUrl: './schedule-properties.component.html',
    styleUrls: ['./schedule-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(SchedulePropertiesComponent),
        generateValidatorProvider(SchedulePropertiesComponent),
    ],
})
export class SchedulePropertiesComponent extends BasePropertiesComponent<Schedule> implements OnInit {

    @Input()
    public mode: SchedulePropertiesMode = SchedulePropertiesMode.UPDATE;

    @Input()
    public scheduleBlacklist: number[] = [];

    public tasks$: Observable<ScheduleTaskBO[]>;

    constructor(
        changeDetector: ChangeDetectorRef,
        protected taskData: ScheduleTaskDataService,
    ) {
        super(changeDetector);
    }

    public override ngOnInit(): void {
        super.ngOnInit();

        this.tasks$ = this.taskData.watchAllEntities().pipe(
            map(tasks => tasks.map(task => {
                (task as any).id = Number(task.id);
                return task;
            })),
        );
    }

    protected override createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            name: new UntypedFormControl(this.safeValue('name') || '', Validators.required),
            description: new UntypedFormControl(this.safeValue('description') || ''),
            taskId: new UntypedFormControl(this.safeValue('taskId') ?? 0, Validators.required),
            scheduleData: new UntypedFormControl(this.safeValue('scheduleData') || {}, Validators.required),
            active: new UntypedFormControl(this.safeValue('active') ?? true),
            parallel: new UntypedFormControl(this.safeValue('parallel') ?? false),
            notificationEmail: new UntypedFormControl(this.safeValue('notificationEmail') || []),
        });
    }

    protected override configureForm(value: Schedule<AnyModelType>, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: Schedule<AnyModelType>): Schedule<AnyModelType> {
        return value;
    }
}
