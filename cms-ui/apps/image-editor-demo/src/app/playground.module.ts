import { NgModule } from '@angular/core';
import { FormsModule } from "@angular/forms";
import { BrowserModule } from '@angular/platform-browser';
import { GenticsUIImageEditorModule } from '@gentics/image-editor';
import { GenticsUICoreModule, OverlayHostService } from "@gentics/ui-core";
import { PlaygroundAppComponent } from "./playground.component";

@NgModule({
    bootstrap: [PlaygroundAppComponent],
    declarations: [PlaygroundAppComponent],
    imports: [BrowserModule, FormsModule, GenticsUIImageEditorModule, GenticsUICoreModule.forRoot()],
    providers: [OverlayHostService]
})
export class PlaygroundModule {}
