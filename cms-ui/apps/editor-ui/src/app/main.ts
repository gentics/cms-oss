import {AppModule} from './app.module';
import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';

import {environment} from '../environments/environment';
import {patchConsoleObject} from './development/development-tools';
import {KeycloakService} from './login/providers/keycloak/keycloak.service';

// ToDo: Remove this after we have transitioned to RxJS 6.x style operators.
import './rxjs-compat';

if (environment.production) {
    enableProdMode();
}
patchConsoleObject();

KeycloakService.checkKeycloakAuth()
    .then(() => platformBrowserDynamic().bootstrapModule(AppModule, { preserveWhitespaces: true }))
    .catch((err: any) => console.error(err));
