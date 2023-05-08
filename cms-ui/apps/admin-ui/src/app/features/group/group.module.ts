import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { GroupDetailComponent, GroupMasterComponent } from './components';
import { GROUP_ROUTES } from './group.routes';
import { CanActivateGroupGuard } from './providers';

@NgModule({
    declarations: [
        GroupMasterComponent,
        GroupDetailComponent,
    ],
    providers: [
        CanActivateGroupGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(GROUP_ROUTES),
    ],
})
export class GroupModule {}
