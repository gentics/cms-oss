import { I18nNotificationService, LanguageHandlerService } from '@admin-ui/core';
import { ConstructCategoryHandlerService } from '@admin-ui/core/providers/construct-category-handler/construct-category-handler.service';
import { ConstructCategoryPropertiesMode } from '@admin-ui/features/construct/components';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { createNestedControlValidator } from '@gentics/cms-components';
import { Language } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';

@Component({
    selector: 'gtx-create-construct-category-modal',
    templateUrl: './create-construct-category-modal.component.html',
    styleUrls: ['./create-construct-category-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateConstructCategoryModalComponent
    extends BaseModal<boolean>
    implements OnInit, OnDestroy {

    // tslint:disable-next-line: variable-name
    readonly ConstructCategoryPropertiesMode = ConstructCategoryPropertiesMode;

    public form: UntypedFormControl;
    public loading = false;

    public supportedLanguages$: Observable<Language[]>;

    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected handler: ConstructCategoryHandlerService,
        private languageHandler: LanguageHandlerService,
        protected notifications: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new UntypedFormControl(null, createNestedControlValidator());

        // get available system languages for i18n-properties
        this.supportedLanguages$ = this.languageHandler.watchSupportedLanguages();
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    buttonCreateEntityClicked(): void {
        if (this.form.invalid) {
            return;
        }

        this.loading = true;
        this.form.disable();
        this.changeDetector.markForCheck();

        this.subscriptions.push(this.handler.create(this.form.value).subscribe({
            complete: () => {
                this.closeFn(true);
            },
            error: (err) => {
                this.loading = false;
                this.form.enable();
                this.changeDetector.markForCheck();

                console.error(err);
                this.notifications.show({
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
