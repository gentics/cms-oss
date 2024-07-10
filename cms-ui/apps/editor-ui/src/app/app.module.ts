/* eslint-disable @typescript-eslint/indent, @typescript-eslint/no-unused-vars */
import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule } from '@angular/router';
import { AppComponent } from './app.component';
import { APP_ROUTES } from './app.routes';
import { CoreModule } from './core/core.module';
import { KeycloakService } from './login/providers/keycloak/keycloak.service';

const PROVIDERS: any[] = [
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    KeycloakService,
];

@NgModule({
    imports: [
        CoreModule,
        RouterModule.forRoot(APP_ROUTES, { preloadingStrategy: PreloadAllModules }),
    ],
    declarations: [AppComponent],
    providers: PROVIDERS,
    bootstrap: [AppComponent],
})
export class AppModule {}
