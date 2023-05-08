import { ModuleWithProviders, NgModule } from '@angular/core';
import { GTX_TOKEN_EXTENDED_TRANSLATIONS } from '@gentics/cms-components';
import { CoreModule } from './core/core.module';
import * as FORM_TRANSLATIONS from './core/providers/i18n/translations/form.translations.json';

const MODULE_TRANSLATIONS = {
    gtxFormGenerator: (FORM_TRANSLATIONS as any).default,
};

@NgModule({
    imports: [
        CoreModule,
    ],
    exports: [
        CoreModule,
    ],
})
export class FormGeneratorModule {
    static forRoot(): ModuleWithProviders<FormGeneratorModule> {
        return {
            ngModule: FormGeneratorModule,
            providers: [
                // provide this module's translations to the i18n solution in parent module
                { provide: GTX_TOKEN_EXTENDED_TRANSLATIONS, useValue: MODULE_TRANSLATIONS, multi: true },
            ],
        }
    }
}
