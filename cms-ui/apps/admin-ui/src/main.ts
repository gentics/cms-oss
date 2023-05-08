import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';

import {AppModule} from './app/app.module';
import {KeycloakService} from './app/login/providers/keycloak/keycloak.service';
import {environment} from './environments/environment';

if (environment.production) {
    enableProdMode();
}

// Integration of Keycloak copied from editor UI.
// Keycloak is checked before initializing Angular to prevent unnecessary initialization
// if user needs to be redirected to the Keycloak login page.
//
// ToDo: If we ever have time, we can refactor the login using the strategy pattern
// to make it more flexible and to allow further authentication methods besides normal login and Keycloak.
KeycloakService.checkKeycloakAuth()
    .then(() =>
        platformBrowserDynamic().bootstrapModule(
            AppModule,
            // Enable preservation of whitespaces for default spacing between components.
            { preserveWhitespaces: true }
        )
    ).catch(err => console.error(err));
