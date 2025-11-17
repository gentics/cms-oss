import { enableProdMode } from '@angular/core';
import { platformBrowser } from '@angular/platform-browser';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
    enableProdMode();
}

// Integration of Keycloak copied from editor UI.
// Keycloak is checked before initializing Angular to prevent unnecessary initialization
// if user needs to be redirected to the Keycloak login page.
//
// ToDo: If we ever have time, we can refactor the login using the strategy pattern
// to make it more flexible and to allow further authentication methods besides normal login and Keycloak.

platformBrowser().bootstrapModule(
    AppModule,
    // Enable preservation of whitespaces for default spacing between components.
    { preserveWhitespaces: true },
);
