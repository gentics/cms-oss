import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { EditorOverlayModule } from './editor-overlay.module';
import { EDITOR_OVERLAY_ROUTES } from './editor-overlay.routes';

@NgModule({
    imports: [
        EditorOverlayModule,
        RouterModule.forChild(EDITOR_OVERLAY_ROUTES),
    ],
    exports: [],
    declarations: [],
    providers: [],
})
export class EditorOverlayRoutingModule { }
