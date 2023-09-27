import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ModalService } from '@gentics/ui-core';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import { CombinedPropertiesEditorComponent } from './components/combined-properties-editor/combined-properties-editor.component';
import { ConfirmApplyToSubitemsModalComponent } from './components/confirm-apply-to-subitems-modal/confirm-apply-to-subitems-modal.component';
import { ConfirmNavigationModal } from './components/confirm-navigation-modal/confirm-navigation-modal.component';
import { ConstructControlsComponent } from './components/construct-controls/construct-controls.component';
import { ContentFrame } from './components/content-frame/content-frame.component';
import { DescriptionTooltipComponent } from './components/description-tooltip/description-tooltip.component';
import { EditorToolbarComponent } from './components/editor-toolbar/editor-toolbar.component';
import { FilePreview } from './components/file-preview/file-preview.component';
import { FormReportsListComponent } from './components/form-reports-list/form-reports-list.component';
import { FormattingControlsComponent } from './components/formatting-controls/formatting-controls.component';
import { LinkControlsComponent } from './components/link-controls/link-controls.component';
import { ListControlsComponent } from './components/list-controls/list-controls.component';
import { NodePropertiesForm } from './components/node-properties-form/node-properties-form.component';
import { PageEditorControlsComponent } from './components/page-editor-controls/page-editor-controls.component';
import { PropertiesEditor } from './components/properties-editor/properties-editor.component';
import { SimpleDeleteModalComponent } from './components/simple-delete-modal/simple-delete-modal.component';
import { TableControlsComponent } from './components/table-controls/table-controls.component';
import { contentFrameRoutes } from './content-frame.routes';
import { CommandToIconPipe } from './pipes/command-to-icon/command-to-icon.pipe';
import { TagTypeIconPipe } from './pipes/tag-type-icon/tag-type-icon.pipe';
import { AlohaIntegrationService } from './providers/aloha-integration/aloha-integration.service';
import { CustomerScriptService } from './providers/customer-script/customer-script.service';
import { ContentFrameGuard } from './providers/guards/content-frame-guard';
import { IFrameCollectionService } from './providers/iframe/iframe-collection.service';

const COMPONENTS = [
    CombinedPropertiesEditorComponent,
    ContentFrame,
    ConstructControlsComponent,
    CommandToIconPipe,
    DescriptionTooltipComponent,
    EditorToolbarComponent,
    FilePreview,
    FormReportsListComponent,
    FormattingControlsComponent,
    LinkControlsComponent,
    ListControlsComponent,
    NodePropertiesForm,
    PageEditorControlsComponent,
    PropertiesEditor,
    TableControlsComponent,
    TagTypeIconPipe,
];

const ENTRY_COMPONENTS = [
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    SimpleDeleteModalComponent,
];

const PROVIDERS = [
    AlohaIntegrationService,
    IFrameCollectionService,
    ContentFrameGuard,
    CustomerScriptService,
    ModalService,
];

@NgModule({
    imports: [
        SharedModule,
        TagEditorModule,
        EditorOverlayModule,
        RouterModule.forChild(contentFrameRoutes),
    ],
    exports: [],
    declarations: [...COMPONENTS, ...ENTRY_COMPONENTS],
    providers: PROVIDERS,
})
export class ContentFrameModule {
    constructor(private customScriptService: CustomerScriptService) {
        this.customScriptService.loadCustomerScript();
    }
}
