import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    forwardRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewEncapsulation,
} from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    UntypedFormControl,
    NG_VALIDATORS,
    ValidationErrors,
    Validator,
} from '@angular/forms';
import { CmsFormElementI18nValue } from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { clone } from 'lodash';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-i18n-input',
    templateUrl: './i18n-input.component.html',
    styleUrls: ['./i18n-input.component.scss'],
    providers: [
        generateFormProvider(I18nInputComponent),
        {
            provide: NG_VALIDATORS, // Is an InjectionToken required by this class to be able to be used as an Validator
            useExisting: forwardRef(() => I18nInputComponent), // for now validation will be put into the component, but can be separated
            multi: true,
        },
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
})
export class I18nInputComponent implements ControlValueAccessor, Validator, OnInit, OnChanges, OnDestroy {

    @Input()
    label: string;

    @Input()
    type: 'text' | 'number' | 'password' | 'tel' | 'email' | 'url' = 'text';

    @Input()
    requiredInCurrentLanguage: boolean;

    @Input()
    language: string;

    @Input()
    availableLanguages: string[];

    @Output()
    blur: EventEmitter<FocusEvent> = new EventEmitter<FocusEvent>();

    i18nInput = new UntypedFormControl();

    private cvaChange: (value: any) => void;
    cvaTouch: () => void;
    private _onValidatorChange: () => void;

    private i18nData: CmsFormElementI18nValue<string | number | null> = {};
    private valueChangesSubscription: Subscription;

    isTranslated = true;
    translationSuggestions: { language: string, value: string | number | null }[] = [];

    ngOnInit(): void {
        this.valueChangesSubscription = this.i18nInput.valueChanges.subscribe((value: string | number | null) => {
            if (this.i18nData && this.language) {
                try {
                    this.i18nData[this.language] = value;
                } catch (e) { }
                this.triggerChange(this.i18nData);
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.i18nData) {
            if (changes.language) {
                this.i18nData ??= {};
                this.i18nInput.setValue(this.i18nData[this.language] || '', {
                    emitEvent: false,
                });
            }
        }
        if (changes.language || changes.availableLanguages) {
            if (this._onValidatorChange) {
                this._onValidatorChange();
            }
        }
    }

    ngOnDestroy(): void {
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }

    writeValue(i18nData: CmsFormElementI18nValue<string | number | null>): void {
        // Create a copy, since the original is usually readonly for whatever reason
        this.i18nData = clone(i18nData || {});
        this.i18nInput.setValue(this.i18nData[this.language] || '', {
            emitEvent: false,
        });
    }

    registerOnChange(fn: (value: any) => void): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.cvaTouch = fn;
    }

    triggerChange(newValue: any): void {
        if (typeof this.cvaChange === 'function') {
            this.cvaChange(newValue);
        }
    }

    triggerTouch(event: FocusEvent): void {
        this.blur.emit(event);
        if (typeof this.cvaTouch === 'function') {
            this.cvaTouch();
        }
    }

    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            this.i18nInput.disable({
                onlySelf: true,
                emitEvent: true,
            });
        } else {
            this.i18nInput.enable({
                onlySelf: true,
                emitEvent: true,
            });
        }
    }

    validate(control: AbstractControl): ValidationErrors {
        const requiredError: ValidationErrors = this.validateRequired(control);
        const translatedError: ValidationErrors = this.validateTranslated(control);
        if (!requiredError && !translatedError) {
            return null;
        }
        return Object.assign({}, requiredError, translatedError);
    }

    registerOnValidatorChange?(fn: () => void): void {
        this._onValidatorChange = fn;
    }

    private validateRequired(control: AbstractControl): ValidationErrors {
        /**
         * if there is no i18n data, then
         *  - there is a required error, iff the value is required in the current language
         */
        if (!this.i18nData) {
            if (this.requiredInCurrentLanguage) {
                return { requiredInCurrentLanguage: true };
            }
        }

        /**
         * if there is no value in the current language, then
         *  - there is a required error, iff the value is required in the current language
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
         * if there is no i18n data, then
         *  - there cannot be a translation error
         */
        if (!this.i18nData) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there are no available languages, then
         *  - there cannot be a translation error
         */
        if (!this.availableLanguages) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there is a value in the current language, then
         *  - there is no translation error
         */
        if (this.valuePresent()) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if there there are no other available languages, then
         *  - there is no translation error
         */
        if (this.availableLanguages.length < 1) {
            this.isTranslated = true;
            return null;
        }

        /**
         * if any other language has this value set (and the current one does not), then
         *  - there is translation error
         */
        const notCurrentLanguages = this.availableLanguages.filter(language => language !== this.language);
        const languagesOfAvailableTranslations = notCurrentLanguages.filter(language => this.valuePresent(language));
        if (languagesOfAvailableTranslations.length > 0) {
            this.isTranslated = false;
            this.translationSuggestions = languagesOfAvailableTranslations.map(language => ({
                language: language,
                value: this.i18nData[language],
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
        if (this.i18nData && language) {
            const value = this.i18nData[language];
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
