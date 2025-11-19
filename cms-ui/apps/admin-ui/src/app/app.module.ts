import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { inject, NgModule, provideAppInitializer } from '@angular/core';
import { KeycloakService } from '@gentics/cms-components';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { TranslateService } from '@ngx-translate/core';
import * as DE_TRANSLATIONS from '../../public/i18n/de.json';
import * as EN_TRANSLATIONS from '../../public/i18n/en.json';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core/core.module';
import { DashboardModule } from './dashboard/dashboard.module';
import { AppStateService } from './state';

const PROVIDERS: any[] = [
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    KeycloakService,
    provideAppInitializer(() => {
        const appState = inject(AppStateService);
        const client = inject(GCMSRestClientService);
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
    id: 'admin-ui',
    declarations: [
        AppComponent,
    ],
    imports: [
        CoreModule,
        AppRoutingModule,
        DashboardModule,
    ],
    providers: PROVIDERS,
    bootstrap: [AppComponent],
})
export class AppModule {}
