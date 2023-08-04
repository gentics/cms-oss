import { ConstructHandlerService, I18nNotificationService, LanguageHandlerService } from '@admin-ui/core';
import { ConstructPropertiesMode } from '@admin-ui/features/construct/components';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Language } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';

@Component({
    selector: 'gtx-create-construct-modal',
    templateUrl: './create-construct-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateConstructModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    readonly ConstructPropertiesMode = ConstructPropertiesMode;

    public form: UntypedFormControl;
    public loading = false;

    public supportedLanguages$: Observable<Language[]>;

    private subscription = new Subscription();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: ConstructHandlerService,
        private languageHandler: LanguageHandlerService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl(null, createNestedControlValidator());

        // get available system languages for i18n-properties
        this.supportedLanguages$ = this.languageHandler.getSupportedLanguages();
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    buttonCreateEntityClicked(): void {
        if (this.form.invalid) {
            return;
        }

        this.loading = true;
        this.form.disable();
        this.changeDetector.markForCheck();

        const { nodeIds, ...value } = this.form.value;
        this.subscription.add(this.handler.create(value, nodeIds).subscribe({
            complete: () => {
                this.closeFn(true);
            },
            error: (err) => {
                this.loading = false;
                this.form.enable();
                this.changeDetector.markForCheck();

                console.error(err);
                this.notification.show({
                    type: 'alert',
                    message: 'construct.create_error',
                    translationParams: {
                        errorMessage: err.message,
                    },
                });
            },
        }));
    }
}
