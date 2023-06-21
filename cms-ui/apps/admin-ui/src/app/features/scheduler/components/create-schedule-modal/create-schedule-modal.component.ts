import { I18nNotificationService, ScheduleOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { ScheduleBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { SchedulePropertiesMode } from '../schedule-properties/schedule-properties.component';

@Component({
    selector: 'gtx-create-schedule-modal',
    templateUrl: './create-schedule-modal.component.html',
    styleUrls: ['./create-schedule-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateScheduleModalComponent extends BaseModal<ScheduleBO | false> implements OnInit, OnDestroy {

    readonly SchedulePropertiesMode = SchedulePropertiesMode;

    public form: UntypedFormControl;
    public loading = false;

    protected subscription: Subscription;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected entityOperations: ScheduleOperations,
        protected notifications: I18nNotificationService,
    ) {
        super();
    }

    public ngOnInit(): void {
        this.form = new UntypedFormControl({
            name: null,
            description: null,
            taskId: null,
            scheduleData: null,
            active: true,
            parallel: false,
            notificationEmail: [],
        }, [createNestedControlValidator()]);
    }

    public ngOnDestroy(): void {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }
    }

    public createEntity(): void {
        if (this.form.disabled || this.form.invalid) {
            return;
        }

        if (this.subscription) {
            this.subscription.unsubscribe();
        }

        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscription = this.entityOperations.create(this.form.value).subscribe(created => {
            this.form.enable({ emitEvent: false });
            this.loading = false;
            this.changeDetector.markForCheck();
            this.closeFn(created);
        }, error => {
            this.notifications.show({
                message: 'scheduler.create_schedule_error',
                translationParams: {
                    errorMessage: error.message,
                },
            });
            this.form.enable({ emitEvent: false });
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }
}
