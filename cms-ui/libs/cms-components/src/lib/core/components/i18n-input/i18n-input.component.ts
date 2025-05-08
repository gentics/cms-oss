import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Input,
    OnChanges,
    SimpleChanges,
    ViewEncapsulation,
} from '@angular/core';
import {
    AbstractControl,
    ValidationErrors,
    Validator,
} from '@angular/forms';
import { CmsFormElementI18nValue } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';

@Component({
    selector: 'gtx-i18n-input',
    templateUrl: './i18n-input.component.html',
    styleUrls: ['./i18n-input.component.scss'],
    providers: [
        generateFormProvider(I18nInputComponent),
        generateValidatorProvider(I18nInputComponent),
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class I18nInputComponent
    extends BaseFormElementComponent<CmsFormElementI18nValue<string | number | null>>
    implements Validator, OnChanges {

    @Input()
    public type: 'text' | 'number' | 'password' | 'tel' | 'email' | 'url' = 'text';

    @Input()
    public requiredInCurrentLanguage: boolean;

    @Input()
    public language: string;

    @Input()
    public availableLanguages: string[];

    @Input()
    public useRichEditor = false;

    private validatorChange: () => void = () => { /* no op until assigned */ };

    public isTranslated = true;
    public translationSuggestions: { language: string, value: string | number | null }[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
    ) {
        super(changeDetector);
        this.booleanInputs.push('requiredInCurrentLanguage');
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.language || changes.availableLanguages) {
            this.validatorChange();
        }
    }

    protected onValueChange(): void {
        /* No op */
    }

    public handleInputChange(changeValue: string | number | null): void {
        if (this.value && this.language) {
            // Has to be a clone. Since the value is passed as ref, we'd edit it
            // in here and the change detection above would not detect any changes,
            // since the value is already present/updated.
            const tmp = cloneDeep(this.value || {});
            tmp[this.language] = changeValue;
            this.triggerChange(tmp);
            this.validatorChange();
        } else {
            const tmp = {};
            if (this.language) {
                tmp[this.language] = changeValue;
            }
            this.triggerChange(tmp);
            this.validatorChange();
        }
    }

    validate(control: AbstractControl): ValidationErrors {
        const requiredError: ValidationErrors = this.validateRequired(control);
        const translatedError: ValidationErrors = this.validateTranslated(control);
        this.changeDetector.markForCheck();
        if (!requiredError && !translatedError) {
            return null;
        }
        return Object.assign({}, requiredError, translatedError);
    }

    registerOnValidatorChange(fn: () => void): void {
        this.validatorChange = fn;
    }

    private validateRequired(control: AbstractControl): ValidationErrors {
        /**
         * if there is no i18n data, then there is a required error,
         * if the value is required in the current language
         */
        if (!this.value) {
            if (this.requiredInCurrentLanguage) {
                return { requiredInCurrentLanguage: true };
            }
        }

        /**
         * if there is no value in the current language, then there is a required error,
         * if the value is required in the current language
         */
        if (!this.valuePresent()) {
            if (this.requiredInCurrentLanguage) {
                return { requiredInCurrentLanguage: true };
            }
        }

        return null;
    }

    private validateTranslated(control: AbstractControl): ValidationErrors {

        /**
         * if there is no i18n data, then there cannot be a translation error
         */
        if (!this.value) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there are no available languages, then there cannot be a translation error
         */
        if (!this.availableLanguages) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there is a value in the current language, then there is no translation error
         */
        if (this.valuePresent()) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there there are no other available languages, then there is no translation error
         */
        if (this.availableLanguages.length < 1) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if any other language has this value set (and the current one does not),
         * then there is translation error
         */
        const notCurrentLanguages = this.availableLanguages.filter(language => language !== this.language);
        const languagesOfAvailableTranslations = notCurrentLanguages.filter(language => this.valuePresent(language));
        if (languagesOfAvailableTranslations.length > 0) {
            this.isTranslated = false;
            this.translationSuggestions = languagesOfAvailableTranslations.map(language => ({
                language: language,
                value: this.value[language],
            }));
            return { untranslated: true };
        }

        this.isTranslated = true;
        return null;
    }

    /**
     * checks whether a value is present in the given language
     */
    private valuePresent(language: string = this.language): boolean {
        if (this.value && language) {
            const value = this.value[language];
            if (this.type === 'number') {
                if (typeof value === 'number') {
                    return true;
                }
            } else {
                if (typeof value === 'string' && value.length > 0) {
                    return true;
                }
            }
        }
        return false;
    }

}
