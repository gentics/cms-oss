import { CommonModule } from '@angular/common';
import { inject, ModuleWithProviders, NgModule, provideAppInitializer } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as DE_TRANSLATIONS from '../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../public/i18n/en.json';
import {
    DynamicFormSettingsComponent,
    DynamicFormTranslationsComponent,
    FormConditionInputComponent,
    FormElementDefinitionComponent,
    FormElementSettingsComponent,
    FormElementTranslationComponent,
    FormGridComponent,
    FormGridElementsContainerComponent,
    FormOptionsInputComponent,
    FormPageManagerComponent,
} from './components';

const COMPONENTS = [
    DynamicFormSettingsComponent,
    DynamicFormTranslationsComponent,
    FormConditionInputComponent,
    FormElementDefinitionComponent,
    FormElementSettingsComponent,
    FormElementTranslationComponent,
    FormGridComponent,
    FormGridElementsContainerComponent,
    FormOptionsInputComponent,
    FormPageManagerComponent,
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        GenticsUICoreModule,
        CmsComponentsModule,
        TranslateModule,
    ],
    declarations: [...COMPONENTS],
    providers: [],
    exports: [...COMPONENTS],
})
export class FormGridModule {
    static forRoot(): ModuleWithProviders<FormGridModule> {
        return {
            ngModule: FormGridModule,
            providers: [
                provideAppInitializer(() => {
                    const translations = inject(TranslateService);
                    translations.setTranslation('de', DE_TRANSLATIONS, true);
                    translations.setTranslation('en', EN_TRANSLATIONS, true);
                }),
            ],
        };
    }
}
