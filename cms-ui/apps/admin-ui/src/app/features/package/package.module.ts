import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CreatePackageModalComponent, PackageCheckWrapperComponent, PackageDetailComponent, PackageMasterComponent } from './components';
import { PACKAGE_ROUTES } from './package.routes';
import { CanActivatePackageGuard } from './providers';

@NgModule({
    declarations: [
        CreatePackageModalComponent,
        PackageMasterComponent,
        PackageDetailComponent,
        PackageCheckWrapperComponent,
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
