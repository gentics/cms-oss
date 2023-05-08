import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { EditorOverlayModule } from './editor-overlay.module';
import { editorOverlayRoutes } from './editor-overlay.routes';

@NgModule({
    imports: [
        EditorOverlayModule,
        RouterModule.forChild(editorOverlayRoutes)
    ],
    exports: [],
    declarations: [],
    providers: []
})
export class EditorOverlayRoutingModule { }
