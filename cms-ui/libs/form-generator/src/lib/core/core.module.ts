import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CmsComponentsModule } from '@gentics/cms-components';
import { GenticsUICoreModule } from '@gentics/ui-core';
import {
    FormEditorComponent,
    FormEditorElementComponent,
    FormEditorElementListComponent,
    FormEditorMenuComponent,
    FormElementDropZoneComponent,
    FormElementPreviewComponent,
    FormElementPropertiesEditorComponent,
} from './components';

import { ElementPropertyPipe, I18nFgPipe } from './pipes';
import {
    FormEditorConfigurationService,
    FormEditorMappingService,
    FormEditorService,
    FormReportService,
} from './providers';

const COMPONENTS: any[] = [
    FormEditorComponent,
    FormEditorElementComponent,
    FormEditorElementListComponent,
    FormEditorMenuComponent,
    FormElementPreviewComponent,
    FormElementDropZoneComponent,
    FormElementPropertiesEditorComponent,
];

const DIRECTIVES = [
];

const PIPES: any[] = [
    ElementPropertyPipe,
    I18nFgPipe,
];

const DECLARATIONS: any[] = [
    ...COMPONENTS,
    ...DIRECTIVES,
    ...PIPES,
];

const PROVIDERS: any[] = [
    FormEditorConfigurationService,
    FormEditorMappingService,
    FormEditorService,
    FormReportService,
];

@NgModule({
    declarations: DECLARATIONS,
    imports: [
        CmsComponentsModule,
        CommonModule,
        FormsModule,
        GenticsUICoreModule,
        ReactiveFormsModule,

    ],
    exports: [
        ...DECLARATIONS,
    ],
    providers: PROVIDERS
})
export class CoreModule { }
