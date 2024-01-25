import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import {
    AlohaAttributeButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaInputRendererComponent,
    AlohaSelectMenuRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    DynamicDropdownComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
} from './components';
import { contentFrameRoutes } from './content-frame.routes';
import { ContentFrameGuard } from './guards';
import { AlohaCompatIconPipe, CommandToIconPipe, TagTypeIconPipe } from './pipes';
import {
    AlohaIntegrationService,
    CustomerScriptService,
    DynamicOverlayService,
    IFrameCollectionService,
} from './providers';

const COMPONENTS = [
    AlohaAttributeButtonRendererComponent,
    AlohaButtonRendererComponent,
    AlohaComponentRendererComponent,
    AlohaContextButtonRendererComponent,
    AlohaInputRendererComponent,
    AlohaSelectMenuRendererComponent,
    AlohaSplitButtonRendererComponent,
    AlohaTableSizeSelectRendererComponent,
    AlohaToggleButtonRendererComponent,
    AlohaToggleSplitButtonRendererComponent,
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ContentFrameComponent,
    DescriptionTooltipComponent,
    DynamicDropdownComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PageEditorTabsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    TableSizeInputComponent,
    TableSizeSelectComponent,
];

const PIPES = [
    AlohaCompatIconPipe,
    CommandToIconPipe,
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

@NgModule({
    imports: [
        SharedModule,
        TagEditorModule,
        EditorOverlayModule,
        RouterModule.forChild(contentFrameRoutes),
    ],
    exports: [],
    declarations: [...COMPONENTS, ...PIPES],
    providers: [...PROVIDERS, ...GUARDS],
})
export class ContentFrameModule {
    constructor(private customScriptService: CustomerScriptService) {
        this.customScriptService.loadCustomerScript();
    }
}
