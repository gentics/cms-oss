import { MeshModule } from '@admin-ui/mesh';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NgxPaginationModule } from 'ngx-pagination';
import { SharedModule } from '../../shared/shared.module';
import { ContentRepositoryTableComponent, MeshBrowserEditorComponent, MeshBrowserMasterComponent, MeshBrowserSchemaListComponent } from './components';
import { MESH_BROWSER_ROUTES } from './mesh-browser.routes';
import { MeshBrowserContentRepositoryTableLoaderService, MeshBrowserLoaderService } from './providers';


@NgModule({
    declarations: [
        MeshBrowserMasterComponent,
        MeshBrowserEditorComponent,
        ContentRepositoryTableComponent,
        MeshBrowserSchemaListComponent,
    ],
    providers: [
        MeshBrowserContentRepositoryTableLoaderService,
        MeshBrowserLoaderService,
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
