import { ContentPackageOperations, I18nNotificationService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ContentPackageBO, EditableContentPackage } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { ContentPackagePropertiesMode } from '../content-package-properties/content-package-properties.component';

@Component({
    selector: 'gtx-create-content-package-modal',
    templateUrl: './create-content-package-modal.component.html',
    styleUrls: ['./create-content-package-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class CreateContentPackageModalComponent extends BaseModal<ContentPackageBO> implements OnInit, OnDestroy {

    readonly ContentPackagePropertiesMode = ContentPackagePropertiesMode;

    public form: FormControl<EditableContentPackage>;
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
        this.form = new FormControl(null);
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

        this.subscription = this.entityOperations.create(this.form.value).subscribe({
            next: created => this.closeFn(created),
            error: error => {
                this.notification.show({
                    type: 'alert',
                    message: error.message,
                });
                this.loading = false;
                this.form.enable();
                this.changeDetector.markForCheck();
            },
        });
    }
}
