import { Component, ChangeDetectionStrategy } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BasePropertiesComponent } from '@gentics/cms-components';
import { Language } from '@gentics/cms-models';
import { generateFormProvider, generateValidatorProvider } from '@gentics/ui-core';

@Component({
    selector: 'gtx-language-properties',
    templateUrl: './language-properties.component.html',
    styleUrls: ['./language-properties.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        generateFormProvider(LanguagePropertiesComponent),
        generateValidatorProvider(LanguagePropertiesComponent),
    ],
})
export class LanguagePropertiesComponent extends BasePropertiesComponent<Language> {

    protected createForm(): FormGroup<any> {
        return new FormGroup<any>({
            name: new FormControl(this.safeValue('name'), [Validators.required, Validators.maxLength(50)]),
            code: new FormControl(this.safeValue('code'), [Validators.required, Validators.maxLength(5)]),
        });
    }

    protected configureForm(value: Language, loud?: boolean): void {
        // no-op
    }

    protected assembleValue(value: Language): Language {
        return value;
    }

}
