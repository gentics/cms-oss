
import { createI18nRequiredValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import { ConstructCategoryBO, GtxI18nProperty, Language } from '@gentics/cms-models';
import { BaseFormElementComponent, generateFormProvider } from '@gentics/ui-core';
import { isEqual } from 'lodash';
import { combineLatest } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

export enum ConstructCategoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-construct-category-properties',
    templateUrl: './construct-category-properties.component.html',
    styleUrls: ['./construct-category-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ConstructCategoryPropertiesComponent)],
})
export class ConstructCategoryPropertiesComponent
    extends BaseFormElementComponent<ConstructCategoryBO>
    implements OnInit, OnChanges {

    @Input()
    public mode: ConstructCategoryPropertiesMode = ConstructCategoryPropertiesMode.UPDATE;

    @Input()
    public supportedLanguages: Language[];

    public form: UntypedFormGroup;
    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    ngOnInit(): void {
        this.form = new UntypedFormGroup({
            nameI18n: new UntypedFormControl((this.value || {}).nameI18n, this.createNameValidator()),
        }, { updateOn: 'change' });

        // Update the control to force a validation to happen to update the `invalidLanguages` properly
        this.form.get('nameI18n').patchValue((this.value || {}));
        this.form.updateValueAndValidity();

        this.subscriptions.push(combineLatest([
            this.form.valueChanges,
            this.form.statusChanges,
        ]).pipe(
            map(([value, status]) => status === 'VALID' ? value : null),
            distinctUntilChanged(isEqual),
        ).subscribe(newValue => {
            this.triggerChange(newValue);
        }));
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.supportedLanguages && this.form) {
            const defaultLanguage = this.supportedLanguages?.[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }

            this.form.get('nameI18n').setValidators(this.createNameValidator());
        }
    }

    createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator((this.supportedLanguages || []).map(l => l.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }

    protected onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                nameI18n: this.value?.nameI18n || {},
            });
        }
    }

    setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.supportedLanguages.find(l => l.id === languageId);
    }

    activeI18nTabValueExists(languageCode: string): boolean {
        return [
            this.form.get('nameI18n')?.value as GtxI18nProperty,
        ].some(data => !!data?.[languageCode]);
    }
}
