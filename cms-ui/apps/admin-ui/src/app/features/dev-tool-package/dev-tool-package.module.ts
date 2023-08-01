import { NgModule } from '@angular/core';
import { RouterModule, provideRouter, withComponentInputBinding } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CreateDevToolPackageModalComponent, DevToolPackageEditorComponent, DevToolPackageMasterComponent } from './components';
import { DEV_TOOL_PACKAGE_ROUTES } from './dev-tool-package.routes';

@NgModule({
    declarations: [
        CreateDevToolPackageModalComponent,
        DevToolPackageMasterComponent,
        DevToolPackageEditorComponent,
    ],
    providers: [
        provideRouter(DEV_TOOL_PACKAGE_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        RouterModule.forChild(DEV_TOOL_PACKAGE_ROUTES),
    ],
})
export class DevToolPackageModule {}
