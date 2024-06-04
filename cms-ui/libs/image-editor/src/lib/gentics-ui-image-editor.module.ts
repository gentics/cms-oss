import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GenticsUICoreModule } from '@gentics/ui-core';
import * as Cropper from 'cropperjs';
import {
    ControlPanelComponent,
    FocalPointSelectorComponent,
    GenticsImageEditorComponent,
    GenticsImagePreviewComponent,
    ImageCropperComponent,
    ImagePreviewWithScalesComponent,
} from './components';
import { FocalPointTargetDirective } from './directives';
import { CropperConstructor } from './models';
import { TranslatePipe } from './pipes';
import { LanguageService } from './providers';

/**
 * The export behaviour of the Cropper lib leads to some issues when compiling
 * in AoT mode. This factory is designed to ensure that the correct value is
 * defined for the CropperConstuctor token.
 */
export function getCropperConstructor(): Cropper {
    return (Cropper && Cropper.default) || Cropper as any;
}

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        GenticsUICoreModule,
    ],
    declarations: [
        ControlPanelComponent,
        FocalPointSelectorComponent,
        FocalPointTargetDirective,
        GenticsImageEditorComponent,
        GenticsImagePreviewComponent,
        ImagePreviewWithScalesComponent,
        ImageCropperComponent,
        TranslatePipe,
    ],
    providers: [
        LanguageService,
        TranslatePipe,
        { provide: CropperConstructor, useFactory: getCropperConstructor },
    ],
    exports: [
        GenticsImageEditorComponent,
        GenticsImagePreviewComponent,
    ],
})
export class GenticsUIImageEditorModule {}
