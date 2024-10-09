import { ChangeDetectionStrategy, Component, forwardRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, UntypedFormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CmsFormElementI18nValue } from '@gentics/cms-models';
import { Subscription } from 'rxjs';


@Component({
    selector: 'gtx-i18n-checkbox',
    templateUrl: './i18n-checkbox.component.html',
    styleUrls: ['./i18n-checkbox.component.scss'],
    providers: [{
        provide: NG_VALUE_ACCESSOR, // Is an InjectionToken required by the ControlValueAccessor interface to provide a form value
        useExisting: forwardRef(() => I18nCheckboxComponent), // tells Angular to use the existing instance
        multi: true,
    }],
    changeDetection: ChangeDetectionStrategy.OnPush,
    encapsulation: ViewEncapsulation.None,
})
export class I18nCheckboxComponent implements ControlValueAccessor, OnInit, OnChanges, OnDestroy {

    @Input() label: string;
    @Input() language: string;
    @Input() availableLanguages: string[];

    i18nCheckbox = new UntypedFormControl();

    private _onChange: (_: any) => void;
    _onTouched: any;
    private _onValidatorChange: () => void;

    private i18nData: CmsFormElementI18nValue<boolean | null>;
    private valueChangesSubscription: Subscription;

    ngOnInit(): void {
        this.valueChangesSubscription = this.i18nCheckbox.valueChanges.subscribe((value: boolean | null) => {
            if (this.i18nData) {
                this.i18nData[this.language] = value;
                if (this._onChange) {
                    this._onChange(this.i18nData);
                }
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (this.i18nData) {
            if (changes.language) {
                this.i18nCheckbox.setValue(
                    this.i18nData[this.language],
                    {
                        onlySelf: true,
                        emitEvent: true,
                    },
                );
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

    writeValue(i18nData: CmsFormElementI18nValue<boolean | null>): void {
        this.i18nData = i18nData ? i18nData : {};
        this.i18nCheckbox.setValue(
            this.i18nData[this.language],
            {
                onlySelf: true,
                emitEvent: true,
            },
        );
    }

    registerOnChange(fn: (_: any) => void): void {
        this._onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this._onTouched = fn;
    }

    setDisabledState?(isDisabled: boolean): void {
        if (isDisabled) {
            this.i18nCheckbox.disable({
                onlySelf: true,
                emitEvent: true,
            });
        } else {
            this.i18nCheckbox.enable({
                onlySelf: true,
                emitEvent: true,
            });
        }
    }

}
