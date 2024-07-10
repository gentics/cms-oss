import { I18nNotificationService, ScheduleTaskOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { ScheduleTaskBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { ScheduleTaskPropertiesMode } from '../schedule-task-properties/schedule-task-properties.component';

@Component({
    selector: 'gtx-create-schedule-task-modal',
    templateUrl: './create-schedule-task-modal.component.html',
    styleUrls: ['./create-schedule-task-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateScheduleTaskModalComponent extends BaseModal<ScheduleTaskBO | false> implements OnInit, OnDestroy {

    readonly ScheduleTaskPropertiesMode = ScheduleTaskPropertiesMode;

    public form: UntypedFormControl;
    public loading = false;

    protected subscription: Subscription;

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected entityOperations: ScheduleTaskOperations,
        protected notifications: I18nNotificationService,
    ) {
        super();
    }

    public ngOnInit(): void {
        this.form = new UntypedFormControl({
            name: null,
            description: null,
            command: null,
            internal: false,
        }, Validators.required);
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
            // No need to enable the form, as we close the modal anyways
            this.closeFn(created);
        }, () => {
            // Error is already handled in the operations-service
            this.form.enable({ emitEvent: false });
            this.loading = false;
            this.changeDetector.markForCheck();
        });
    }
}
