import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChange } from '@angular/core';
import { FormControl, FormGroup, ValidatorFn } from '@angular/forms';
import { AnyModelType, Language, Role } from '@gentics/cms-models';
import { BaseFormPropertiesComponent, generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';
import { createI18nRequiredValidator } from '../../../../common';

@Component({
    selector: 'gtx-role-properties',
    templateUrl: './role-properties.component.html',
    styleUrls: ['./role-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(RolePropertiesComponent),
        generateValidatorProvider(RolePropertiesComponent),
    ],
    standalone: false
})
export class RolePropertiesComponent extends BaseFormPropertiesComponent<Role> implements OnChanges {

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
            nameI18n: new FormControl(this.safeValue('nameI18n') || {}, this.createNameValidator()),
            descriptionI18n: new FormControl(this.safeValue('descriptionI18n') || {}),
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
