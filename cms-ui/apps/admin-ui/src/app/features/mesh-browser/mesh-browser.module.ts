import { MeshModule } from '@admin-ui/mesh';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import {
    RouterModule,
    provideRouter,
    withComponentInputBinding,
} from '@angular/router';
import { NgxPaginationModule } from 'ngx-pagination';
import { SharedModule } from '../../shared/shared.module';
import {
    ContentRepositoryTableComponent,
    MeshBrowserBreadcrumbComponent,
    MeshBrowserContentVersionComponent,
    MeshBrowserEditorComponent,
    MeshBrowserLanguageActivationComponent,
    MeshBrowserLanguageSwitcherComponent,
    MeshBrowserMasterComponent,
    MeshBrowserModuleMasterComponent,
    MeshBrowserProjectSwitcherComponent,
    MeshBrowserSchemaItemsComponent,
    MeshBrowserSchemaListComponent,
} from './components';
import { MESH_BROWSER_ROUTES } from './mesh-browser.routes';
import {
    MeshBrowserCanActivateGuard,
    MeshBrowserContentRepositoryTableLoaderService,
    MeshBrowserImageService,
    MeshBrowserLoaderService,
    MeshBrowserNavigatorService,
} from './providers';

@NgModule({
    declarations: [
        MeshBrowserModuleMasterComponent,
        MeshBrowserMasterComponent,
        MeshBrowserEditorComponent,
        ContentRepositoryTableComponent,
        MeshBrowserSchemaListComponent,
        MeshBrowserSchemaItemsComponent,
        MeshBrowserProjectSwitcherComponent,
        MeshBrowserLanguageActivationComponent,
        MeshBrowserLanguageSwitcherComponent,
        MeshBrowserBreadcrumbComponent,
        MeshBrowserContentVersionComponent,
    ],
    providers: [
        MeshBrowserContentRepositoryTableLoaderService,
        MeshBrowserLoaderService,
        MeshBrowserNavigatorService,
        MeshBrowserImageService,
        MeshBrowserCanActivateGuard,
        provideRouter(MESH_BROWSER_ROUTES, withComponentInputBinding()),
    ],
    imports: [
        SharedModule,
        CommonModule,
        NgxPaginationModule,
        RouterModule.forChild(MESH_BROWSER_ROUTES),
        MeshModule,
    ],
})
export class MeshBrowserModule {}
