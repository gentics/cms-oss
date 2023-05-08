import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ContentRepositoryFragmentDetailComponent, ContentRepositoryFragmentMasterComponent } from './components';
import { CR_FRAGMENT_ROUTES } from './cr-fragment.routes';
import { CanActivateCRFragmentGuard } from './providers';

@NgModule({
    declarations: [
        ContentRepositoryFragmentDetailComponent,
        ContentRepositoryFragmentMasterComponent,
    ],
    providers: [
        CanActivateCRFragmentGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(CR_FRAGMENT_ROUTES),
    ],
})
export class ContentRepositoryFragmentModule {}
