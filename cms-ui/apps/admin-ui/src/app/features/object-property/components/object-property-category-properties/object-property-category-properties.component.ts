import { createI18nRequiredValidator } from '@admin-ui/common';
import { LanguageHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { BasePropertiesComponent, CONTROL_INVALID_VALUE } from '@gentics/cms-components';
import {
    CmsI18nValue,
    Language,
    ModelType,
    Normalized,
    ObjectPropertyCategoryBO,
} from '@gentics/cms-models';
import { generateFormProvider } from '@gentics/ui-core';
import { Observable } from 'rxjs';

export interface ObjectPropertyCategoryPropertiesFormData {
    nameI18n?: CmsI18nValue;
}

export enum ObjectPropertyCategoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

/**
 * Defines the data editable by the `ObjectPropertyCategoryPropertiesComponent`.
 *
 * To convey the validity state of the user's input, the onChange callback will
 * be called with `null` if the form data is currently invalid.
 */
@Component({
    selector: 'gtx-object-property-category-properties',
    templateUrl: './object-property-category-properties.component.html',
    styleUrls: ['./object-property-category-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(ObjectPropertyCategoryPropertiesComponent)],
})
export class ObjectPropertyCategoryPropertiesComponent
    extends BasePropertiesComponent<ObjectPropertyCategoryBO<Normalized>>
    implements OnInit {

    @Input()
    public mode: ObjectPropertyCategoryPropertiesMode;

    public languages$: Observable<Language[]>;

    public languages: Language[];
    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        private languageHandler: LanguageHandlerService,
    ) {
        super(changeDetector);
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.languages$ = this.languageHandler.getSupportedLanguages();

        this.subscriptions.push(this.languages$.subscribe(languages => {
            this.languages = languages;
            const defaultLanguage = languages[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
            if (this.form) {
                this.form.get('nameI18n').setValidators(this.createNameValidator());
            }
            this.changeDetector.markForCheck();
        }));
    }

    protected configureForm(value: ObjectPropertyCategoryBO<ModelType.Normalized>, loud?: boolean): void {
        // Nothing to do
    }

    protected createForm(): UntypedFormGroup {
        return new UntypedFormGroup({
            nameI18n: new UntypedFormControl(this.value?.nameI18n, this.createNameValidator()),
        }, { updateOn: 'change' });
    }

    protected assembleValue(formData: ObjectPropertyCategoryBO<Normalized>): ObjectPropertyCategoryBO<Normalized> {
        if (this.mode === ObjectPropertyCategoryPropertiesMode.UPDATE) {
            return {
                ...formData,
                id: this.value?.id,
                globalId: this.value?.globalId,
            };
        } else {
            return formData;
        }
    }

    protected override onValueChange(): void {
        if (this.form && this.value && (this.value as any) !== CONTROL_INVALID_VALUE) {
            this.form.setValue({
                nameI18n: this.value?.nameI18n || {},
            });
        }
    }

    protected createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator((this.languages || []).map(lang => lang.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }

    public setActiveI18nTab(languageId: number): void {
        this.activeTabI18nLanguage = this.languages.find(l => l.id === languageId);
    }
}
