import {AppModule} from './app/app.module';
import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';

import {environment} from './environments/environment';
import {patchConsoleObject} from './app/development/development-tools';
import {KeycloakService} from './app/login/providers/keycloak/keycloak.service';

// ToDo: Remove this after we have transitioned to RxJS 6.x style operators.
import './app/rxjs-compat';

if (environment.production) {
    enableProdMode();
}
patchConsoleObject();

KeycloakService.checkKeycloakAuth()
    .then(() => platformBrowserDynamic().bootstrapModule(AppModule, { preserveWhitespaces: true }))
    .catch((err: any) => console.error(err));
