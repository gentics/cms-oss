import { LanguageOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Language, LanguageCreateRequest } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';

@Component({
    selector: 'gtx-create-language-modal',
    templateUrl: './create-language-modal.component.html',
    styleUrls: [ './create-language-modal.component.scss' ],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateLanguageModalComponent implements IModalDialog, OnInit {

    /** Current step (tab) of the entity creation wizzard */
    currentTab = String(1);

    /** form instance */
    form: UntypedFormGroup;

    constructor(
        private languages: LanguageOperations,
    ) {
    }

    ngOnInit(): void {
        // instantiate form
        this.form = new UntypedFormGroup({
            name: new UntypedFormControl(null),
            code: new UntypedFormControl(null),
        });
    }

    closeFn = (entityCreated: Language) => {};
    cancelFn = () => {};

    registerCloseFn(close: (val?: any) => void): void {
        this.closeFn = (entityCreated: Language) => {
            close(entityCreated);
        };
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        this.cancelFn = cancel;
    }

    /** Get form validity state */
    isValid(): boolean {
        return this.form.valid;
    }

    /** Programmatic tab set */
    setActiveTab(index: string): void {
        this.currentTab = String(index);
    }

    /**
     * Returns TRUE if parameter index is index of active tab
     */
    tabIndexIsActive(index: number): boolean {
        return this.currentTab === String(index);
    }

    /**
     * If user clicks to create a new language
     */
    buttonCreateEntityClicked(): void {
        this.createEntity()
            .then(languageCreated => this.closeFn(languageCreated));
    }

    private createEntity(): Promise<Language> {
        // assemble payload with conditional properties
        const language: LanguageCreateRequest = {
            name: this.form.value.name,
            code: this.form.value.code,
        };
        return this.languages.createLanguage(language).toPromise();
    }

}
