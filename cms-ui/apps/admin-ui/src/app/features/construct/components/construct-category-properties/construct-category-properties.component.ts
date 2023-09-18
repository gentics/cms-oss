
import { createI18nRequiredValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormGroup, UntypedFormControl, UntypedFormGroup, ValidatorFn } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { AnyModelType, ConstructCategoryBO, Language } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

export enum ConstructCategoryPropertiesMode {
    CREATE = 'create',
    UPDATE = 'update',
}

@Component({
    selector: 'gtx-construct-category-properties',
    templateUrl: './construct-category-properties.component.html',
    styleUrls: ['./construct-category-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(ConstructCategoryPropertiesComponent),
        generateValidatorProvider(ConstructCategoryPropertiesComponent),
    ],
})
export class ConstructCategoryPropertiesComponent
    extends BasePropertiesComponent<ConstructCategoryBO>
    implements OnChanges {

    @Input()
    public mode: ConstructCategoryPropertiesMode = ConstructCategoryPropertiesMode.UPDATE;

    @Input()
    public supportedLanguages: Language[];

    public form: UntypedFormGroup;
    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    protected createForm(): FormGroup<any> {
        return this.form = new UntypedFormGroup({
            nameI18n: new UntypedFormControl((this.value || {}).nameI18n, this.createNameValidator()),
        }, { updateOn: 'change' });
    }

    protected configureForm(value: ConstructCategoryBO<AnyModelType>, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: ConstructCategoryBO<AnyModelType>): ConstructCategoryBO<AnyModelType> {
        return value;
    }

    ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.supportedLanguages && this.form) {
            const defaultLanguage = this.supportedLanguages?.[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
        }
    }

    createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator(() => (this.supportedLanguages || []).map(l => l.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }
}
