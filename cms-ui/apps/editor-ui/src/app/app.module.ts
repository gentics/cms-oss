/* eslint-disable @typescript-eslint/indent, @typescript-eslint/no-unused-vars */
import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule } from '@angular/router';
import { KeycloakService } from '@gentics/cms-components';
import { PiktidModule } from '@gentics/picktid-editor';
import { AppComponent } from './app.component';
import { APP_ROUTES } from './app.routes';
import { CoreModule } from './core/core.module';

const PROVIDERS: any[] = [
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    KeycloakService,
];

@NgModule({
    imports: [
        CoreModule,
        PiktidModule,
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
