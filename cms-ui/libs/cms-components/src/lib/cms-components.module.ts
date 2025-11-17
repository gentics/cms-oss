import { inject, ModuleWithProviders, NgModule, provideAppInitializer } from '@angular/core';
import DE_TRANSLATIONS from '../../public/i18n/de.json';
import EN_TRANSLATIONS from '../../public/i18n/en.json';
import { CoreModule } from './core/core.module';
import { TranslateService } from '@ngx-translate/core';

@NgModule({
    imports: [
        CoreModule,
    ],
    exports: [
        CoreModule,
    ],
})
export class CmsComponentsModule {
    static forRoot(): ModuleWithProviders<CmsComponentsModule> {
        return {
            ngModule: CmsComponentsModule,
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
