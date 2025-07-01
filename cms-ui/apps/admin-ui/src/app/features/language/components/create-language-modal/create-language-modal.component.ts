import { LanguageHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Language } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-language-modal',
    templateUrl: './create-language-modal.component.html',
    styleUrls: [ './create-language-modal.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateLanguageModalComponent extends BaseModal<Language> implements OnInit {

    form: UntypedFormControl;

    /** Will be set when the create call is sent */
    loading = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: LanguageHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormControl({
            name: '',
            code: '',
        });
    }

    /**
     * If user clicks to create a new language
     */
    buttonCreateEntityClicked(): void {
        this.form.disable({ emitEvent: false });
        this.loading = true;
        this.changeDetector.markForCheck();

        this.createEntity()
            .then(languageCreated => {
                this.closeFn(languageCreated);
            }, () => {
                this.form.enable({ emitEvent: false });
                this.loading = false;
                this.changeDetector.markForCheck();
            });
    }

    private createEntity(): Promise<Language> {
        return this.handler.createMapped(this.form.value).toPromise();
    }

}
