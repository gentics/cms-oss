import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    UntypedFormArray,
    UntypedFormBuilder,
    UntypedFormGroup,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validator,
} from '@angular/forms';
import { CmsFormElementI18nValue, CmsFormElementKeyI18nValuePair } from '@gentics/cms-models';
import { ISortableEvent } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-key-i18n-value-list-input',
    templateUrl: './key-i18n-value-list-input.component.html',
    styleUrls: ['./key-i18n-value-list-input.component.scss'],
    providers: [{
        provide: NG_VALUE_ACCESSOR, // Is an InjectionToken required by the ControlValueAccessor interface to provide a form value
        useExisting: forwardRef(() => KeyI18nValueListInputComponent), // tells Angular to use the existing instance
        multi: true,
    }, {
        provide: NG_VALIDATORS, // Is an InjectionToken required by this class to be able to be used as an Validator
        useExisting: forwardRef(() => KeyI18nValueListInputComponent), // for now validation will be put into the component, but can be separated
        multi: true,
    }],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
})
export class KeyI18nValueListInputComponent implements ControlValueAccessor, Validator, OnInit, OnChanges, OnDestroy {

    @Input()
    label: string;

    @Input()
    keyLabel: string;

    @Input()
    valueLabel: string;

    @Input()
    requiredInCurrentLanguage: boolean;

    @Input()
    language: string;

    @Input()
    availableLanguages: string[];

    i18nArray = new UntypedFormArray([]);

    private cvaChange: (_: any) => void;
    public cvaTouch: any;
    private validationChange: () => void;

    // private i18nData: CmsFormElementKeyI18nValuePair[];
    private valueChangesSubscription: Subscription;

    isTranslated = true;
    duplicateKeys = [];

    constructor(
        private formBuilder: UntypedFormBuilder,
        protected changeDetector: ChangeDetectorRef,
    ) {}

    ngOnInit(): void {
        this.valueChangesSubscription = this.i18nArray.valueChanges.subscribe((value: CmsFormElementKeyI18nValuePair[]) => {
            if (this.cvaChange) {
                this.cvaChange(value);
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.language || changes.availableLanguages) {
            if (this.validationChange) {
                this.validationChange();
            }
        }
    }

    ngOnDestroy(): void {
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }

    writeValue(i18nData: CmsFormElementKeyI18nValuePair[]): void {
        const data = cloneDeep(i18nData ? i18nData : []);

        if (data.length === this.i18nArray.length) {
            this.i18nArray.setValue(data, { emitEvent: false });
            this.i18nArray.updateValueAndValidity();
            return;
        }

        this.i18nArray.clear();
        data.map((keyI18nValuePair: CmsFormElementKeyI18nValuePair) => {
            return this.formBuilder.group({
                key: [keyI18nValuePair.key],
                value_i18n: [keyI18nValuePair.value_i18n],
            });
        }).forEach((formGroup: UntypedFormGroup) => {
            this.i18nArray.push(formGroup);
        })
    }

    registerOnChange(fn: (_: any) => void): void {
        this.cvaChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.cvaTouch = fn;
    }

    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            this.i18nArray.disable({
                onlySelf: true,
                emitEvent: true,
            });
        } else {
            this.i18nArray.enable({
                onlySelf: true,
                emitEvent: true,
            });
        }
    }

    validate(control: AbstractControl): ValidationErrors {
        const requiredError: ValidationErrors = this.validateRequired(control);
        const translatedError: ValidationErrors = this.validateTranslated(control);
        const duplicatedKeyError: ValidationErrors = this.validateDuplicatedKeys(control);
        if (!requiredError && !translatedError && !duplicatedKeyError) {
            return null;
        }
        return Object.assign({}, requiredError, translatedError, duplicatedKeyError);
    }

    registerOnValidatorChange?(fn: () => void): void {
        this.validationChange = fn;
    }

    add(): void {
        this.i18nArray.push(this.formBuilder.group({
            key: [''],
            value_i18n: [{}],
        }));
    }

    removeRow(index: number): void {
        this.i18nArray.removeAt(index);
    }

    sortList(e: ISortableEvent): void {
        const currentValue = this.i18nArray.value;
        const newValue = e.sort([...currentValue], false);

        /*
         * We need to rebuild the form-array, because the sortable-list will scramble the DOM elements,
         * and therefore make the elements appear in the wrong order.
         * With this, we rebuild the form in the correct order and force the list to be rendered again,
         * but this time in the correct order.
         * With that, everything is correct again in the DOM and in the data model.
         */
        this.i18nArray.clear();
        newValue.map((keyI18nValuePair: CmsFormElementKeyI18nValuePair) => {
            return this.formBuilder.group({
                key: [keyI18nValuePair.key],
                value_i18n: [keyI18nValuePair.value_i18n],
            });
        }).forEach((formGroup: UntypedFormGroup) => {
            this.i18nArray.push(formGroup);
        });
        this.i18nArray.updateValueAndValidity();
        this.changeDetector.markForCheck();
    }

    private validateRequired(control: AbstractControl): ValidationErrors {

        /**
         * if there is no i18n array, then there is a required error,
         * if the value is required in the current language
         */
        if (!this.i18nArray) {
            if (this.requiredInCurrentLanguage) {
                return { requiredInCurrentLanguage: true };
            }
        }

        /**
         * if the i18n array has length 0, then there is a required error,
         * if the value is required in the current language
         */
        if (this.i18nArray.length === 0) {
            if (this.requiredInCurrentLanguage) {
                return { requiredInCurrentLanguage: true };
            }
        }

        const value: CmsFormElementKeyI18nValuePair[] = this.i18nArray.value;

        // relying on errors on children is not possible, since error state will not sync up
        for (const keyI18nValuePair of value) {

            /**
             * if there is no key in the current language, then there is a required error,
             * if the value is required in the current language
             */
            if (typeof keyI18nValuePair.key === 'string' && keyI18nValuePair.key.length === 0) {
                if (this.requiredInCurrentLanguage) {
                    return { requiredInCurrentLanguage: true };
                }
            }

            /**
             * if there is no value in the current language, then there is a required error,
             * if the value is required in the current language
             */
            if (!this.valuePresent(keyI18nValuePair.value_i18n)) {
                if (this.requiredInCurrentLanguage) {
                    return { requiredInCurrentLanguage: true };
                }
            }
        }

        return null;
    }

    private validateTranslated(control: AbstractControl): ValidationErrors {
        const value: CmsFormElementKeyI18nValuePair[] = this.i18nArray.value;

        // relying on errors on children is not possible, since error state will not sync up
        for (const keyI18nValuePair of value) {
            if (this.i18nValueIsUntranslated(keyI18nValuePair.value_i18n)) {
                this.isTranslated = false;
                return { untranslated: true };
            }
        }

        this.isTranslated = true;
        return null;
    }

    private i18nValueIsUntranslated(i18nValue: CmsFormElementI18nValue<string>): boolean {

        /**
         * if there is no i18n value, then there cannot be a translation error
         */
        if (!i18nValue) {
            return false;
        }

        /**
         * if there are no available languages, then there cannot be a translation error
         */
        if (!this.availableLanguages) {
            return false;
        }

        /**
         * if there is a value in the current language, then there is no translation error
         */
        if (this.valuePresent(i18nValue)) {
            return false;
        }

        /**
         * if there there are no other available languages, then there is no translation error
         */
        if (this.availableLanguages.length < 1) {
            return false;
        }

        /**
         * if any other language has this value set (and the current one does not), then there is translation error
         */
        const notCurrentLanguages = this.availableLanguages.filter(language => language !== this.language);
        const languagesOfAvailableTranslations = notCurrentLanguages.filter(language => this.valuePresent(i18nValue, language));
        if (languagesOfAvailableTranslations.length > 0) {
            return true;
        }

        return false;
    }

    stopPropagation(event: Event): void {
        event.stopPropagation();
    }

    private validateDuplicatedKeys(control: AbstractControl): ValidationErrors {
        const value: CmsFormElementKeyI18nValuePair[] = this.i18nArray.value;
        const keys = value.map((keyI18nValuePair: CmsFormElementKeyI18nValuePair) => keyI18nValuePair.key);
        const duplicateKeys: string[] = [];

        this.duplicateKeys = [];

        /**
         * if there are two keys with the same name, then there is a duplicated keys error
         */
        for (let i = 0; i < keys.length; i++) {
            for (let j = i + 1; j < keys.length; j++) {
                if (keys[i] === keys[j]) {
                    duplicateKeys.push(keys[i]);
                    this.duplicateKeys.push(keys[i]);
                }
            }
        }

        if (duplicateKeys.length > 0) {
            return { duplicateKeys: duplicateKeys };
        } else {
            return null;
        }
    }

    /**
     * checks whether a value is present in the given language
     */
    private valuePresent(i18nValue: CmsFormElementI18nValue<string>, language: string = this.language): boolean {
        if (i18nValue && language) {
            const value = i18nValue[language];
            if (typeof value === 'string' && value.length > 0) {
                return true;
            }
        }
        return false;
    }

}
