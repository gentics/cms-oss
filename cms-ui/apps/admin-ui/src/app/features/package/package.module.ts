import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CreatePackageModalComponent, PackageDetailComponent, PackageMasterComponent } from './components';
import { PACKAGE_ROUTES } from './package.routes';
import { CanActivatePackageGuard } from './providers';

@NgModule({
    declarations: [
        CreatePackageModalComponent,
        PackageMasterComponent,
        PackageDetailComponent,
    ],
    providers: [
        CanActivatePackageGuard,
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(PACKAGE_ROUTES),
    ],
})
export class PackageModule {}
