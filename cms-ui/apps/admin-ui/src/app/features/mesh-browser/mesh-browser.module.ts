import { MeshModule } from '@admin-ui/mesh';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ContentRepositoryTableComponent, MeshBrowserEditorComponent, MeshBrowserMasterComponent } from './components';
import { MESH_BROWSER_ROUTES } from './mesh-browser.routes';
import { MeshBrowserContentRepositoryTableLoaderService } from './providers/mesh-browser-repository-table-loader.service';


@NgModule({
    declarations: [
        MeshBrowserMasterComponent,
        MeshBrowserEditorComponent,
        ContentRepositoryTableComponent,
    ],
    providers: [
        MeshBrowserContentRepositoryTableLoaderService,
    ],
    imports: [
        SharedModule,
        CommonModule,
        RouterModule.forChild(MESH_BROWSER_ROUTES),
        MeshModule,
    ],
})
export class MeshBrowserModule {}
