import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import {
    AnonymizationEditorComponent,
    FaceManipulationComponent,
    ImagePreviewComponent,
    EditorComponent,
    LoginGateComponent,
} from './components';
import { PiktidAPIService } from './providers';

const COMPONENTS = [
    AnonymizationEditorComponent,
    FaceManipulationComponent,
    ImagePreviewComponent,
    EditorComponent,
    LoginGateComponent,
];

@NgModule({
    imports: [CommonModule, GenticsUICoreModule, ReactiveFormsModule, FormsModule],
    declarations: [
        ...COMPONENTS,
    ],
    exports: [
        ...COMPONENTS,
    ],
    providers: [PiktidAPIService],
})
export class PiktidModule {}
