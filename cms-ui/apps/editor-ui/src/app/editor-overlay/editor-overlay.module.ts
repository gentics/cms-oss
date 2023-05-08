import { NgModule } from '@angular/core';
import { GenticsUIImageEditorModule } from '@gentics/image-editor';
import { ModalService } from '@gentics/ui-core';
import { SharedModule } from '../shared/shared.module';
import { ConfirmCloseModal } from './components/confirm-close-modal/confirm-close-modal.component';
import { EditorOverlay } from './components/editor-overlay/editor-overlay.component';
import { ImageEditorModalComponent } from './components/image-editor-modal/image-editor-modal.component';
import { EditorOverlayService } from './providers/editor-overlay.service';

const COMPONENTS = [
    EditorOverlay,
];

const ENTRY_COMPONENTS = [
    ImageEditorModalComponent,
    ConfirmCloseModal,
];

const PROVIDERS = [
    EditorOverlayService,
    ModalService,
];

@NgModule({
    imports: [
        SharedModule,
        GenticsUIImageEditorModule
    ],
    exports: [],
    declarations: [...COMPONENTS, ...ENTRY_COMPONENTS],
    providers: PROVIDERS,
})
export class EditorOverlayModule { }
