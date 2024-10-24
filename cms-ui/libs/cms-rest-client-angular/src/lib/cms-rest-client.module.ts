import { NgModule, Optional, SkipSelf } from '@angular/core';
import { GCMSRestClientService } from './cms-rest-client.service';

@NgModule({
    providers: [GCMSRestClientService],
})
export class GCMSRestClientModule {
    constructor(@Optional() @SkipSelf() parentModule?: GCMSRestClientModule) {
        if (parentModule) {
            throw new Error(
                'GCMSRestClientModule is already loaded. Import it in the AppModule only',
            );
        }
    }
}
