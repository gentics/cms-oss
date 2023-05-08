import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ModalService } from '@gentics/ui-core';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { TagEditorModule } from '../tag-editor';
import { CombinedPropertiesEditorComponent } from './components/combined-properties-editor/combined-properties-editor.component';
import { ConfirmApplyToSubitemsModalComponent } from './components/confirm-apply-to-subitems-modal/confirm-apply-to-subitems-modal.component';
import { ConfirmNavigationModal } from './components/confirm-navigation-modal/confirm-navigation-modal.component';
import { ContentFrame } from './components/content-frame/content-frame.component';
import { DescriptionTooltipComponent } from './components/description-tooltip/description-tooltip.component';
import { FilePreview } from './components/file-preview/file-preview.component';
import { SimpleDeleteModalComponent } from './components/simple-delete-modal/simple-delete-modal.component';
import { FormReportsListComponent } from './components/form-reports-list/form-reports-list.component';
import { NodePropertiesForm } from './components/node-properties-form/node-properties-form.component';
import { PropertiesEditor } from './components/properties-editor/properties-editor.component';
import { contentFrameRoutes } from './content-frame.routes';
import { TagTypeIconPipe } from './pipes/tag-type-icon/tag-type-icon.pipe';
import { CustomerScriptService } from './providers/customer-script/customer-script.service';
import { ContentFrameGuard } from './providers/guards/content-frame-guard';
import { IFrameCollectionService } from './providers/iframe/iframe-collection.service';

const COMPONENTS = [
    CombinedPropertiesEditorComponent,
    ContentFrame,
    DescriptionTooltipComponent,
    FilePreview,
    FormReportsListComponent,
    NodePropertiesForm,
    PropertiesEditor,
    TagTypeIconPipe,
];

const ENTRY_COMPONENTS = [
    ConfirmApplyToSubitemsModalComponent,
    ConfirmNavigationModal,
    SimpleDeleteModalComponent,
];

const PROVIDERS = [
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
    providers: PROVIDERS
})
export class ContentFrameModule {
    constructor(private customScriptService: CustomerScriptService) {
        this.customScriptService.loadCustomerScript();
    }
}
