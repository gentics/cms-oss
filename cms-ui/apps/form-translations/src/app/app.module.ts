import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrowserModule } from '@angular/platform-browser';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import * as DE_TRANSLATIONS from '../assets/i18n/de.json';
import * as EN_TRANSLATIONS from '../assets/i18n/en.json';
import { AppComponent } from './app.component';
import { SaveBarComponent } from './components/save-bar/save-bar.component';
import { ScopeTabsComponent } from './components/scope-tabs/scope-tabs.component';
import { ShellComponent } from './components/shell/shell.component';
import { TranslationsTableComponent } from './components/translations-table/translations-table.component';
import { TranslationsToolbarComponent } from './components/translations-toolbar/translations-toolbar.component';
import { AuthenticationService } from './core/services/authentication.service';

@NgModule({
    declarations: [
        AppComponent,
        ShellComponent,
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
            const translate = inject(TranslateService);
            const auth = inject(AuthenticationService);
            const client = inject(GCMSRestClientService);

            translate.setTranslation('de', DE_TRANSLATIONS, true);
            translate.setTranslation('en', EN_TRANSLATIONS, true);

            auth.init();

            client.init({
                connection: { absolute: false, basePath: '/rest' },
            });
            if (auth.sid) {
                client.setSessionId(auth.sid);
            }
        }),
    ],
    bootstrap: [AppComponent],
})
export class AppModule {}
