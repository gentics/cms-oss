import { NgModule, inject, provideAppInitializer } from '@angular/core';
import { RouterModule } from '@angular/router';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { ColorAlphaModule } from 'ngx-color/alpha';
import { ColorSliderModule } from 'ngx-color/slider';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import {
    AlohaAttributeButtonRendererComponent,
    AlohaAttributeToggleButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaCheckboxRendererComponent,
    AlohaColorPickerRendererComponent,
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectMenuRendererComponent,
    AlohaSelectRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaSymbolGridRendererComponent,
    AlohaSymbolSearchGridRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    DynamicDropdownComponent,
    DynamicFormModal,
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
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
} from './components';
import { CONTENT_FRAME_ROUTES } from './content-frame.routes';
import { ContentFrameGuard } from './guards';
import {
    CustomerScriptService,
    DynamicOverlayService,
} from './providers';

const COMPONENTS = [
    AlohaAttributeButtonRendererComponent,
    AlohaAttributeToggleButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaCheckboxRendererComponent,
    AlohaColorPickerRendererComponent,
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaContextToggleButtonRendererComponent,
    AlohaDateTimePickerRendererComponent,
    AlohaIFrameRendererComponent,
    AlohaInputRendererComponent,
    AlohaLinkTargetRendererComponent,
    AlohaSelectMenuRendererComponent,
    AlohaSelectRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaSymbolGridRendererComponent,
    AlohaSymbolSearchGridRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,

    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    DynamicDropdownComponent,
    DynamicFormModal,
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
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
];

const PROVIDERS = [
    CustomerScriptService,
    DynamicOverlayService,
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
        RouterModule.forChild(CONTENT_FRAME_ROUTES),
        GenticsUICoreModule,
    ],
    exports: [],
    declarations: [...COMPONENTS],
    providers: [...PROVIDERS, ...GUARDS, MODULE_INITIALIZER],
})
export class ContentFrameModule {}
