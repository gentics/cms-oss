import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import {
    CreateDataSourceEntryModalComponent,
    CreateDataSourceModalComponent,
    DataSourceEditorComponent,
    DataSourceEntryPropertiesComponent,
    DataSourceEntryTableComponent,
    DataSourceMasterComponent,
    DataSourcePropertiesComponent,
} from './components';
import { DATA_SOURCE_ROUTES } from './data-source.routes';
import { DataSourceEntryTableLoaderService } from './providers';

@NgModule({
    declarations: [
        CreateDataSourceEntryModalComponent,
        CreateDataSourceModalComponent,
        DataSourceEditorComponent,
        DataSourceMasterComponent,
        DataSourceEntryPropertiesComponent,
        DataSourceEntryTableComponent,
        DataSourcePropertiesComponent,
    ],
    providers: [
        DataSourceEntryTableLoaderService,
        provideRouter(DATA_SOURCE_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(DATA_SOURCE_ROUTES),
    ],
})
export class DataSourceModule {}
