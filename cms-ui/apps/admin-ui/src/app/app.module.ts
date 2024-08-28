import { CoreModule } from '@admin-ui/core/core.module';
import { DashboardModule } from '@admin-ui/dashboard/dashboard.module';
import { HashLocationStrategy, LocationStrategy } from '@angular/common';
import { NgModule } from '@angular/core';
import { AngularSvgIconModule } from 'angular-svg-icon';
import { KeycloakService } from '../../../../libs/cms-components/src/lib/core/providers/keycloak/keycloak.service';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

const PROVIDERS: any[] = [
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    KeycloakService,
];

@NgModule({
    id: 'admin-ui',
    declarations: [
        AppComponent,
    ],
    imports: [
        AngularSvgIconModule.forRoot(),
        CoreModule,
        AppRoutingModule,
        DashboardModule,
    ],
    providers: PROVIDERS,
    bootstrap: [AppComponent],
})
export class AppModule {}
