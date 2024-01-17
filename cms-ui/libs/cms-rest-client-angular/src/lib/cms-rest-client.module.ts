import { NgModule } from '@angular/core';
import { GCMSRestClientService } from './cms-rest-client.service';

@NgModule({
    providers: [
        GCMSRestClientService,
    ],
})
export class GCMSRestClientModule {}
