import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import {
    AnonymizationEditorComponent,
    FaceManipulationComponent,
    ImagePreviewComponent,
    PiktidEditorComponent,
} from './components';
import { PiktidAPIService } from './providers';

@NgModule({
    imports: [CommonModule, GenticsUICoreModule, ReactiveFormsModule, FormsModule],
    declarations: [
        PiktidEditorComponent,
        ImagePreviewComponent,
        FaceManipulationComponent,
        AnonymizationEditorComponent,
    ],
    exports: [PiktidEditorComponent],
    providers: [PiktidAPIService],
})
export class PiktidModule {}
