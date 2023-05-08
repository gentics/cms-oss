import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { DashboardItemGroupComponent } from './components/dashboard-item-group/dashboard-item-group.component';
import { DashboardItemComponent } from './components/dashboard-item/dashboard-item.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { WidgetCmsStatusComponent } from './components/widget-cms-status/widget-cms-status.component';
import { DASHBOARD_ROUTES } from './dashboard.routes';

@NgModule({
    declarations: [
        DashboardComponent,
        DashboardItemGroupComponent,
        DashboardItemComponent,
        WidgetCmsStatusComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(DASHBOARD_ROUTES),
    ],
})
export class DashboardModule {}
