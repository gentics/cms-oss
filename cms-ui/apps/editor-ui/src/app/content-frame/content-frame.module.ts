import { NgModule, inject, provideAppInitializer } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AlohaModule } from '@gentics/cms-components/aloha';
import { FormGridModule } from '@gentics/form-grid';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { ColorAlphaModule } from 'ngx-color/alpha';
import { ColorSliderModule } from 'ngx-color/slider';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import {
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    ImagePropertiesModalComponent,
    InheritanceEditorComponent,
    LinkCheckerControlsComponent,
    NodePropertiesComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,
    PropertiesEditorComponent,
    SimpleDeleteModalComponent,
} from './components';
import { CONTENT_FRAME_ROUTES } from './content-frame.routes';
import { ContentFrameGuard } from './guards';
import {
    CustomerScriptService,
} from './providers';

const COMPONENTS = [
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    ImagePropertiesModalComponent,
    InheritanceEditorComponent,
    LinkCheckerControlsComponent,
    NodePropertiesComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,

    PropertiesEditorComponent,
    SimpleDeleteModalComponent,
];

const PROVIDERS = [
    CustomerScriptService,
];

const GUARDS = [
    ContentFrameGuard,
];

const MODULE_INITIALIZER = provideAppInitializer(() => {
    const customScriptService = inject(CustomerScriptService);
    return customScriptService.loadCustomerScript();
});

@NgModule({
    imports: [
        SharedModule,
        TagEditorModule,
        EditorOverlayModule,
        ColorSliderModule,
        ColorAlphaModule,
        FormGridModule,
        AlohaModule.forRoot(),
        RouterModule.forChild(CONTENT_FRAME_ROUTES),
        GenticsUICoreModule,
    ],
    exports: [],
    declarations: [...COMPONENTS],
    providers: [...PROVIDERS, ...GUARDS, MODULE_INITIALIZER],
})
export class ContentFrameModule {}
