import { createI18nRequiredValidator } from '@admin-ui/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { FormControl, FormGroup, ValidatorFn } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { AnyModelType, Language, Role } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-role-properties',
    templateUrl: './role-properties.component.html',
    styleUrls: ['./role-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(RolePropertiesComponent),
        generateValidatorProvider(RolePropertiesComponent),
    ],
})
export class RolePropertiesComponent extends BasePropertiesComponent<Role> implements OnChanges {

    @Input()
    public supportedLanguages: Language[] = [];

    public activeTabI18nLanguage: Language;
    public invalidLanguages: string[] = [];

    ngOnChanges(changes: Record<keyof RolePropertiesComponent, SimpleChange>): void {
        super.ngOnChanges(changes);

        if (changes.supportedLanguages) {
            const defaultLanguage = this.supportedLanguages?.[0];
            if (defaultLanguage) {
                this.activeTabI18nLanguage = defaultLanguage;
            }
            if (this.form) {
                const ctl = this.form.get('nameI18n');
                ctl.setValidators(this.createNameValidator());
                ctl.updateValueAndValidity();
            }
        }
    }

    protected createForm(): FormGroup<any> {
        return new FormGroup({
            nameI18n: new FormControl(this.value?.nameI18n || {}, this.createNameValidator()),
            descriptionI18n: new FormControl(this.value?.descriptionI18n || {}),
        });
    }

    protected configureForm(value: Role<AnyModelType>, loud?: boolean): void {
        // Nothing to do
    }

    protected assembleValue(value: Role<AnyModelType>): Role<AnyModelType> {
        return value;
    }

    createNameValidator(): ValidatorFn {
        const validator = createI18nRequiredValidator((this.supportedLanguages || []).map(l => l.code), langs => {
            this.invalidLanguages = langs;
            this.changeDetector.markForCheck();
        });

        return validator;
    }
}
