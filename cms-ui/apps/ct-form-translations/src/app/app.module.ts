import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { CmsComponentsModule, I18nService } from '@gentics/cms-components';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as DE_TRANSLATIONS from '../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../public/i18n/en.json';
import { AppComponent } from './app.component';
import { SaveBarComponent } from './components/save-bar/save-bar.component';
import { ScopeTabsComponent } from './components/scope-tabs/scope-tabs.component';
import { TranslationsTableComponent } from './components/translations-table/translations-table.component';
import { TranslationsToolbarComponent } from './components/translations-toolbar/translations-toolbar.component';

@NgModule({
    declarations: [
        AppComponent,
        ScopeTabsComponent,
        TranslationsToolbarComponent,
        TranslationsTableComponent,
        SaveBarComponent,
    ],
    imports: [
        BrowserModule,
        FormsModule,
        GenticsUICoreModule.forRoot(),
        CmsComponentsModule.forRoot(),
        GCMSRestClientModule,
        TranslateModule.forRoot({ fallbackLang: 'en' }),
    ],
    providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideAppInitializer(() => {
            const i18n = inject(I18nService);
            const translate = inject(TranslateService);
            const client = inject(GCMSRestClientService);

            translate.setTranslation('de', DE_TRANSLATIONS, true);
            translate.setTranslation('en', EN_TRANSLATIONS, true);

            let savedLang = localStorage.getItem('GCMSUI_uiLanguage');

            // For whatever reason, some strings are *sometimes* written with, and sometimes without quotes.
            const matches = /^['|"]([a-zA-Z-]+)['|"]$/.exec(savedLang);
            if (matches != null && matches.length >= 2) {
                savedLang = matches[1];
            }

            if (savedLang === 'de' || savedLang === 'en') {
                i18n.setLanguage(savedLang);
            } else {
                const inferred = i18n.inferUserLanguage();
                if (inferred === 'de' || savedLang === 'en') {
                    i18n.setLanguage(inferred);
                } else {
                    // If nothing matches, we fall back to english just in case
                    i18n.setLanguage('en');
                }
            }

            client.init({
                connection: { absolute: false, basePath: '/rest' },
            });
        }),
    ],
    bootstrap: [AppComponent],
})
export class AppModule {}
