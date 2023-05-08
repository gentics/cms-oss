import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { ErrorHandler, NgModule, Optional, SkipSelf } from '@angular/core';

import { GCMS_API_BASE_URL, GCMS_API_ERROR_HANDLER, GCMS_API_SID, GcmsRestClientsAngularModule } from '@gentics/cms-rest-clients-angular';

import { TranslateModule } from '@ngx-translate/core';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../common/utils/base-urls';
import { GcmsAuthenticationService } from './services/authentication/gcms-authentication.service';

export function createSidObservable(gcmsAuthenticationService: GcmsAuthenticationService): Observable<string> {
    return gcmsAuthenticationService.getSid();
}

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        HttpClientModule,
        GenticsUICoreModule,
        TranslateModule,
        GcmsRestClientsAngularModule
    ],
    providers: [
        // @gentics/cms-rest-clients-angular configuration
        { provide: GCMS_API_BASE_URL, useValue: API_BASE_URL },
        { provide: GCMS_API_ERROR_HANDLER, useClass: ErrorHandler },
        {
            provide: GCMS_API_SID,
            useFactory: createSidObservable,
            deps: [ GcmsAuthenticationService ],
        },
        GcmsAuthenticationService
    ]
})
/** Provides core functionality, such as GCMS Authentication service. */
export class CoreModule {

    constructor( @Optional() @SkipSelf() parentModule: CoreModule) {
        if (parentModule) {
            throw new Error('The CoreModule should only be loaded by the AppModule, not by a submodule.');
        }
    }

}
