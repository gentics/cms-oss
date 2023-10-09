import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import {
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    CustomEditorControlComponent,
    DescriptionTooltipComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    FormattingControlsComponent,
    LinkControlsComponent,
    ListControlsComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    TableControlsComponent,
    TableSizeSelectComponent,
} from './components';
import { contentFrameRoutes } from './content-frame.routes';
import { ContentFrameGuard } from './guards';
import { CommandToIconPipe, TagTypeIconPipe } from './pipes';
import {
    AlohaIntegrationService,
    CustomerScriptService,
    IFrameCollectionService,
} from './providers';

const COMPONENTS = [
    CombinedPropertiesEditorComponent,
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    ConstructControlsComponent,
    ContentFrameComponent,
    CustomEditorControlComponent,
    DescriptionTooltipComponent,
    EditorToolbarComponent,
    FilePreviewComponent,
    FormReportsListComponent,
    FormattingControlsComponent,
    LinkControlsComponent,
    ListControlsComponent,
    NodePropertiesFormComponent,
    PageEditorControlsComponent,
    PropertiesEditor,
    SimpleDeleteModalComponent,
    TableControlsComponent,
    TableSizeSelectComponent,
];

const PIPES = [
    CommandToIconPipe,
    TagTypeIconPipe,
];

const PROVIDERS = [
    AlohaIntegrationService,
    IFrameCollectionService,
    CustomerScriptService,
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
