import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CreateDataSourceModalComponent, DataSourceEditorComponent, DataSourceMasterComponent, DataSourcePropertiesComponent } from './components';
import { DATA_SOURCE_ROUTES } from './data-source.routes';

@NgModule({
    declarations: [
        DataSourceMasterComponent,
        DataSourceEditorComponent,
        DataSourcePropertiesComponent,
        CreateDataSourceModalComponent,
    ],
    providers: [
        provideRouter(DATA_SOURCE_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(DATA_SOURCE_ROUTES),
    ],
})
export class DataSourceModule {}
