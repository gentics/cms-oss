import { ContentPackageOperations, I18nNotificationService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { ContentPackageBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { ContentPackagePropertiesMode } from '../content-package-properties/content-package-properties.component';

@Component({
    selector: 'gtx-create-content-package-modal',
    templateUrl: './create-content-package-modal.component.html',
    styleUrls: ['./create-content-package-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateContentPackageModalComponent extends BaseModal<ContentPackageBO> implements OnInit, OnDestroy {

    readonly ContentPackagePropertiesMode = ContentPackagePropertiesMode;

    public form: UntypedFormControl;
    public loading = false;

    private subscription: Subscription;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private entityOperations: ContentPackageOperations,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl(null, createNestedControlValidator());
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    createEntity(): void {
        if (this.loading) {
            return;
        }

        if (this.subscription) {
            this.subscription.unsubscribe();
        }

        this.loading = true;
        this.form.disable();
        this.changeDetector.markForCheck();

        this.subscription = this.entityOperations.create(this.form.value).subscribe(
            created => this.closeFn(created),
            error => {
                this.notification.show({
                    type: 'alert',
                    message: error.message,
                });
            },
            () => {
                this.loading = false;
                this.form.enable();
                this.changeDetector.markForCheck();
            },
        );
    }
}
