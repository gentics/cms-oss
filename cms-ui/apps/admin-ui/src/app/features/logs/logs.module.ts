import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ActionLogEntryMasterComponent, LogsTableComponent } from './components';
import { LOGS_ROUTES } from './logs.routes';
import { ActionLogEntryLoaderService } from './providers';

@NgModule({
    declarations: [
        ActionLogEntryMasterComponent,
        LogsTableComponent,
    ],
    providers: [
        ActionLogEntryLoaderService,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(LOGS_ROUTES),
    ],
})
export class LogsModule {}
