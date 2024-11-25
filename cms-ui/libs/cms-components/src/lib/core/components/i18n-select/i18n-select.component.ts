import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ContentChildren,
    forwardRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    QueryList,
    SimpleChanges,
    ViewEncapsulation,
} from '@angular/core';
import { AbstractControl, ControlValueAccessor, UntypedFormControl, NG_VALIDATORS, NG_VALUE_ACCESSOR, ValidationErrors, Validator } from '@angular/forms';
import { CmsFormElementI18nValue } from '@gentics/cms-models';
import { SelectOptionDirective } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-i18n-select',
    templateUrl: './i18n-select.component.html',
    styleUrls: ['./i18n-select.component.scss'],
    providers: [{
        provide: NG_VALUE_ACCESSOR, // Is an InjectionToken required by the ControlValueAccessor interface to provide a form value
        useExisting: forwardRef(() => I18nSelectComponent), // tells Angular to use the existing instance
        multi: true,
    }, {
        provide: NG_VALIDATORS, // Is an InjectionToken required by this class to be able to be used as an Validator
        useExisting: forwardRef(() => I18nSelectComponent), // for now validation will be put into the component, but can be separated
        multi: true,
    }],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
})
export class I18nSelectComponent implements ControlValueAccessor, Validator, OnInit, AfterViewInit, OnChanges, OnDestroy {

    @Input()
    public label: string;

    @Input()
    public requiredInCurrentLanguage: boolean;

    @Input()
    public language: string;

    @Input()
    public disabled: boolean;

    @Input()
    public multiple = false;

    @Input()
    public clearable = false;

    @Input()
    public availableLanguages: string[];

    @ContentChildren(SelectOptionDirective, { descendants: false }) selectOptions: QueryList<SelectOptionDirective>;
    private selectOptionsSubscription: Subscription;

    i18nSelect = new UntypedFormControl();

    private cvaChange: (_: any) => void;
    cvaTouch: any;
    private validatorChange: () => void;

    private i18nData: CmsFormElementI18nValue<string | null>;
    private valueChangesSubscription: Subscription;

    isTranslated = true;
    translationSuggestions: { language: string, value: string | null }[] = [];

    invalidSelection = false;
    private ngAfterViewInitWasCalled = false;

    constructor(private changeDetector: ChangeDetectorRef) {}

    ngOnInit(): void {
        this.valueChangesSubscription = this.i18nSelect.valueChanges.subscribe((value: string | null) => {
            if (this.i18nData) {
                const tmp = cloneDeep(this.i18nData || {});
                tmp[this.language] = value;
                this.i18nData = tmp;
                if (this.cvaChange) {
                    this.cvaChange(tmp);
                }
            }
        });
    }

    ngAfterViewInit(): void {
        this.ngAfterViewInitWasCalled = true;
        const handleSelectOptionsChange = () => {
            /**
             * Validating whether the selected option is valid depends on the current select options.
             * Thus, whenever they change, reevaluation is required.
             */
            if (this.validatorChange) {
                this.validatorChange();
            }

            /**
             * The validateTranslated function also causes the reevaluation for the translationSuggestions.
             * Since they rely on the selectOptions ContentChildren to be available, there can
             * be situations where we need to fallback on keys instead of view values for translationSuggestions.
             *
             * To deal with these situations, we reevaluate the translationSuggestions as soon as the
             * ContentChildren are available or change, by explicitly calling this evaluation function.
             */
            this.validateTranslated(this.i18nSelect);
            this.changeDetector.markForCheck();
        };
        handleSelectOptionsChange();
        this.selectOptionsSubscription = this.selectOptions.changes.subscribe(handleSelectOptionsChange);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.i18nData) {
            if (changes.language) {
                this.i18nSelect.setValue(
                    this.i18nData[this.language],
                    {
                        onlySelf: true,
                        emitEvent: true,
                    },
                );
            }
        }
        if (changes.language || changes.availableLanguages) {
            if (this.validatorChange) {
                this.validatorChange();
            }
        }
    }

    ngOnDestroy(): void {
        if (this.selectOptionsSubscription) {
            this.selectOptionsSubscription.unsubscribe();
        }
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }

    writeValue(i18nData: CmsFormElementI18nValue<string | null>): void {
        this.i18nData = i18nData ? i18nData : {};
        this.i18nSelect.setValue(
            this.i18nData[this.language],
            {
                onlySelf: true,
                emitEvent: true,
            },
        );
    }

    registerOnChange(fn: (_: any) => void): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.cvaTouch = fn;
    }

    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            this.i18nSelect.disable({
                onlySelf: true,
                emitEvent: true,
            });
        } else {
            this.i18nSelect.enable({
                onlySelf: true,
                emitEvent: true,
            });
        }
    }

    validate(control: AbstractControl): ValidationErrors {
        const requiredError: ValidationErrors = this.validateRequired(control);
        const translatedError: ValidationErrors = this.validateTranslated(control);
        const invalidSelectionError: ValidationErrors = this.validateSelection(control);
        if (!requiredError && !translatedError && !invalidSelectionError) {
            return null;
        }
        return Object.assign({}, requiredError, translatedError, invalidSelectionError);
    }

    registerOnValidatorChange?(fn: () => void): void {
        this.validatorChange = fn;
    }

    private validateRequired(control: AbstractControl): ValidationErrors {
        /**
         * if there is no i18n data, then there is a required error,
         * if the value is required in the current language
         */
        if (!this.i18nData) {
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
        if (!this.i18nData) {
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
            this.translationSuggestions = languagesOfAvailableTranslations.map(language => {
                let value = this.i18nData[language];
                if (this.selectOptions) {
                    const viewValues = this.selectOptions
                        .filter(option => option.value === value)
                        .map(option => option.viewValue);
                    if (viewValues.length > 0) {
                        value = viewValues[0];
                    }
                }
                return {
                    language: language,
                    value: value,
                }
            });
            return { untranslated: true };
        }

        this.isTranslated = true;
        return null;
    }

    private validateSelection(control: AbstractControl): ValidationErrors {

        /**
         * if there is no value in the current language, then there cannot be an invalid selection
         */
        if (!this.valuePresent()) {
            this.invalidSelection = false;
            return null;
        }

        /**
         * Take note of this exception: In case ngAfterViewInit was not called yet,
         * we do not really know which selection options will be there. Without this exception,
         * we would assume there to be none, although they will probably be available shortly after.
         * In order to avoid a brief moment when the input is highlighted red, we introduced this exception.
         */
        if (!this.ngAfterViewInitWasCalled) {
            this.invalidSelection = false;
            return null;
        }

        /** if there are selection options, then there is no invalid selection if the selection is found in the options
         */
        if (this.selectOptions) {
            const value = this.i18nData[this.language];
            if (this.selectOptions.filter(option => option.value === value).length > 0) {
                this.invalidSelection = false;
                return null;
            }
        }

        /** if there are no selection options, then all selections are invalid
         */
        this.invalidSelection = true;
        return { invalidSelection: true };
    }

    /**
     * checks whether a value is present in the given language
     */
    private valuePresent(language: string = this.language): boolean {
        if (this.i18nData && language) {
            const value = this.i18nData[language];
            if (typeof value === 'string' && value.length > 0) {
                return true;
            }
        }
        return false;
    }


}
