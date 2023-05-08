import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { SharedModule } from '../../shared/shared.module';
import { TestingOverviewComponentComponent } from './testing-overview/testing-overview.component';
import { testingRoutes } from './testing.routes';

@NgModule({
    declarations: [
        TestingOverviewComponentComponent,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(testingRoutes),
    ],
})
export class TestingDoNotReleaseModule {}
