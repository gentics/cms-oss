import { APP_INITIALIZER, NgModule, Provider } from '@angular/core';
import { RouterModule } from '@angular/router';
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
    LinkCheckerControlsComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
} from './components';
import { contentFrameRoutes } from './content-frame.routes';
import { ContentFrameGuard } from './guards';
import { TagTypeIconPipe } from './pipes';
import {
    AlohaIntegrationService,
    CustomerScriptService,
    DynamicOverlayService,
    IFrameCollectionService,
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
    LinkCheckerControlsComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    SymbolGridComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
];

const PIPES = [
    TagTypeIconPipe,
];

const PROVIDERS = [
    AlohaIntegrationService,
    CustomerScriptService,
    DynamicOverlayService,
    IFrameCollectionService,
];

const GUARDS = [
    ContentFrameGuard,
];

const MODULE_INITIALIZER: Provider = {
    provide: APP_INITIALIZER,
    multi: true,
    deps: [CustomerScriptService],
    useFactory: (customScriptService: CustomerScriptService) => {
        return customScriptService.loadCustomerScript();
    },
};

@NgModule({
    imports: [
        SharedModule,
        TagEditorModule,
        EditorOverlayModule,
        ColorSliderModule,
        ColorAlphaModule,
        RouterModule.forChild(contentFrameRoutes),
    ],
    exports: [],
    declarations: [...COMPONENTS, ...PIPES],
    providers: [...PROVIDERS, ...GUARDS, MODULE_INITIALIZER],
})
export class ContentFrameModule {}
