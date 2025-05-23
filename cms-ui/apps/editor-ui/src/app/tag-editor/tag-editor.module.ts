import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EditorOverlayModule } from '../editor-overlay/editor-overlay.module';
import { SharedModule } from '../shared/shared.module';
import { CustomTagEditorHostComponent } from './components/custom-tag-editor-host/custom-tag-editor-host.component';
import { CustomTagPropertyEditorHostComponent } from './components/custom-tag-property-editor-host/custom-tag-property-editor-host.component';
import { GenticsTagEditorComponent } from './components/gentics-tag-editor/gentics-tag-editor.component';
import { IFrameWrapperComponent } from './components/iframe-wrapper/iframe-wrapper.component';
import { ExpansionButtonComponent } from './components/shared/expansion-button/expansion-button.component';
import { ImagePreviewComponent } from './components/shared/image-preview/image-preview.component';
import { SortableArrayListComponent } from './components/shared/sortable-array-list/sortable-array-list.component';
import { UploadWithPropertiesModalComponent } from './components/shared/upload-with-properties-modal/upload-with-properties-modal.component';
import { UploadWithPropertiesComponent } from './components/shared/upload-with-properties/upload-with-properties.component';
import { ValidationErrorInfoComponent } from './components/shared/validation-error-info/validation-error-info.component';
import { TagEditorHostComponent } from './components/tag-editor-host/tag-editor-host.component';
import { TagEditorModal } from './components/tag-editor-modal/tag-editor-modal.component';
import { TagPropertyEditorHostComponent } from './components/tag-property-editor-host/tag-property-editor-host.component';
import { CheckboxTagPropertyEditor } from './components/tag-property-editors/checkbox-tag-property-editor/checkbox-tag-property-editor.component';
import { DataSourceTagPropertyEditor } from './components/tag-property-editors/datasource-tag-property-editor/datasource-tag-property-editor.component';
import { FileOrImageUrlTagPropertyEditor } from './components/tag-property-editors/file-or-image-url-tag-property-editor/file-or-image-url-tag-property-editor.component';
import { FolderUrlTagPropertyEditor } from './components/tag-property-editors/folder-url-tag-property-editor/folder-url-tag-property-editor.component';
import { FormTagPropertyEditorComponent } from './components/tag-property-editors/form-tag-property-editor/form-tag-property-editor.component';
import { FormlistTagPropertyEditor } from './components/tag-property-editors/formlist-tag-property-editor/formlist-tag-property-editor.component';
import { ListTagPropertyEditor } from './components/tag-property-editors/list-tag-property-editor/list-tag-property-editor.component';
import { NodeSelectorTagPropertyEditor } from './components/tag-property-editors/node-selector-tag-property-editor/node-selector-tag-property-editor.component';
import { OverviewTagPropertyEditor } from './components/tag-property-editors/overview-tag-property-editor/overview-tag-property-editor.component';
import { PageUrlTagPropertyEditor } from './components/tag-property-editors/page-url-tag-property-editor/page-url-tag-property-editor.component';
import { SelectTagPropertyEditor } from './components/tag-property-editors/select-tag-property-editor/select-tag-property-editor.component';
import { TagRefTagPropertyEditor } from './components/tag-property-editors/tagref-tag-property-editor/tagref-tag-property-editor.component';
import { TextTagPropertyEditor } from './components/tag-property-editors/text-tag-property-editor/text-tag-property-editor.component';
import { ObjectTagNamePipe } from './pipes/object-tag-name/object-tag-name.pipe';
import { TagPropertyLabelPipe } from './pipes/tag-property-label/tag-property-label.pipe';
import { FormgeneratorApiService } from './providers/formgenerator-api/formgenerator-api.service';
import { IFrameStylesService } from './providers/iframe-styles/iframe-styles.service';
import { TagEditorService } from './providers/tag-editor/tag-editor.service';
import { TagPropertyEditorResolverService } from './providers/tag-property-editor-resolver/tag-property-editor-resolver.service';

export const COMPONENTS: any[] = [
    CheckboxTagPropertyEditor,
    CustomTagEditorHostComponent,
    CustomTagPropertyEditorHostComponent,
    DataSourceTagPropertyEditor,
    ExpansionButtonComponent,
    FileOrImageUrlTagPropertyEditor,
    FolderUrlTagPropertyEditor,
    FormlistTagPropertyEditor,
    FormTagPropertyEditorComponent,
    GenticsTagEditorComponent,
    IFrameWrapperComponent,
    ImagePreviewComponent,
    ListTagPropertyEditor,
    NodeSelectorTagPropertyEditor,
    OverviewTagPropertyEditor,
    PageUrlTagPropertyEditor,
    SelectTagPropertyEditor,
    SortableArrayListComponent,
    TagEditorHostComponent,
    TagEditorModal,
    TagPropertyEditorHostComponent,
    TagRefTagPropertyEditor,
    TextTagPropertyEditor,
    UploadWithPropertiesComponent,
    UploadWithPropertiesModalComponent,
    ValidationErrorInfoComponent,
];

export const PIPES: any[] = [
    ObjectTagNamePipe,
    TagPropertyLabelPipe,
];

export const PROVIDERS: any[] = [
    FormgeneratorApiService,
    IFrameStylesService,
    TagEditorService,
    TagPropertyEditorResolverService,
];

export const IMPORTS: any[] = [
    FormsModule,
    SharedModule,
    EditorOverlayModule,
];

export const DECLARATIONS: any[] = [
    ...COMPONENTS,
    ...PIPES,
];

export const EXPORTS: any[] = [
    IFrameWrapperComponent,
    ObjectTagNamePipe,
    TagEditorHostComponent,
];

export const ENTRY_COMPONENTS: any[] = [
    CheckboxTagPropertyEditor,
    CustomTagEditorHostComponent,
    CustomTagPropertyEditorHostComponent,
    DataSourceTagPropertyEditor,
    FileOrImageUrlTagPropertyEditor,
    FolderUrlTagPropertyEditor,
    FormTagPropertyEditorComponent,
    GenticsTagEditorComponent,
    ListTagPropertyEditor,
    NodeSelectorTagPropertyEditor,
    OverviewTagPropertyEditor,
    PageUrlTagPropertyEditor,
    SelectTagPropertyEditor,
    TagRefTagPropertyEditor,
    TextTagPropertyEditor,
    UploadWithPropertiesModalComponent,
];

@NgModule({
    imports: IMPORTS,
    exports: EXPORTS,
    declarations: DECLARATIONS,
    providers: PROVIDERS,
})
export class TagEditorModule { }
