import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { PreloadAllModules, RouterModule } from '@angular/router';
import { KeycloakService } from '@gentics/cms-components';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { TranslateService } from '@ngx-translate/core';
import * as DE_TRANSLATIONS from '../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../public/i18n/en.json';
import { AppComponent } from './app.component';
import { APP_ROUTES } from './app.routes';
import { CoreModule } from './core/core.module';
import { ApplicationStateService } from './state';

const PROVIDERS: any[] = [
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    KeycloakService,
    provideAppInitializer(() => {
        const client = inject(GCMSRestClientService);
        const appState = inject(ApplicationStateService);
        const keycloak = inject(KeycloakService);
        const translations = inject(TranslateService);

        translations.setTranslation('de', DE_TRANSLATIONS, true);
        translations.setTranslation('en', EN_TRANSLATIONS, true);

        client.init({
            connection: {
                absolute: false,
                basePath: '/rest',
            },
        });

        appState.select((state) => state.auth.sid).subscribe((sid) => {
            client.setSessionId(sid);
        });

        return keycloak.checkKeycloakAuth();
    }),
];

@NgModule({
    imports: [
        CoreModule,
        RouterModule.forRoot(APP_ROUTES, {
            preloadingStrategy: PreloadAllModules,
            enableTracing: false,
        }),
    ],
    declarations: [AppComponent],
    providers: PROVIDERS,
    bootstrap: [AppComponent],
})
export class AppModule {}
