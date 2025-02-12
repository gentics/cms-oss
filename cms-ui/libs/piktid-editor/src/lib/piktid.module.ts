import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { PiktidEditorComponent } from './components';
import { PiktidAPIService } from './providers';

@NgModule({
    imports: [CommonModule, GenticsUICoreModule],
    declarations: [PiktidEditorComponent],
    exports: [PiktidEditorComponent],
    providers: [PiktidAPIService],
})
export class PiktidModule {}
