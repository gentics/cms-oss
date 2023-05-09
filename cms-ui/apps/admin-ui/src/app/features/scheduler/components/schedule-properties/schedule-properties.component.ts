import { ScheduleTaskDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE, createNestedControlValidator } from '@gentics/cms-components';
import { AnyModelType, Schedule, ScheduleTaskBO } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
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
    providers: [generateFormProvider(SchedulePropertiesComponent)],
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
            name: new UntypedFormControl(this.value?.name || '', Validators.required),
            description: new UntypedFormControl(this.value?.description || ''),
            taskId: new UntypedFormControl(this.value?.taskId ?? 0, Validators.required),
            scheduleData: new UntypedFormControl(this.value?.scheduleData || {}, [Validators.required, createNestedControlValidator()]),
            active: new UntypedFormControl(this.value?.active ?? true),
            parallel: new UntypedFormControl(this.value?.parallel ?? false),
            notificationEmail: new UntypedFormControl(this.value?.notificationEmail || []),
        });
    }

    protected override configureForm(value: Schedule<AnyModelType>, loud?: boolean): void {
        // Nothing to do
    }

    protected override assembleValue(value: Schedule<AnyModelType>): Schedule<AnyModelType> {
        return value;
    }

    protected override onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                name: this.value?.name || '',
                description: this.value?.description || '',
                taskId: this.value?.taskId || null,
                scheduleData: this.value?.scheduleData || {},
                active: this.value?.active ?? true,
                parallel: this.value?.parallel ?? false,
                notificationEmail: this.value?.notificationEmail || [],
            });
        }
    }
}
