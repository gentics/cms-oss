import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { DataSourceDetailComponent, DataSourceMasterComponent } from './components';
import { DATA_SOURCE_ROUTES } from './data-source.routes';
import { CanActivateDataSourceGuard } from './providers';

@NgModule({
    declarations: [
        DataSourceMasterComponent,
        DataSourceDetailComponent,
    ],
    providers: [
        CanActivateDataSourceGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(DATA_SOURCE_ROUTES),
    ],
})
export class DataSourceModule {}
