import { ComponentFactory, ComponentFactoryResolver, Injectable, Type } from '@angular/core';
import { TagPropertyEditor } from '@gentics/cms-integration-api-models';
import { TagPart, TagPartType } from '@gentics/cms-models';
import { CustomTagPropertyEditorHostComponent } from '../../components/custom-tag-property-editor-host/custom-tag-property-editor-host.component';
import { CheckboxTagPropertyEditor } from '../../components/tag-property-editors/checkbox-tag-property-editor/checkbox-tag-property-editor.component';
import { DataSourceTagPropertyEditor } from '../../components/tag-property-editors/datasource-tag-property-editor/datasource-tag-property-editor.component';
import {
    FileOrImageUrlTagPropertyEditor,
} from '../../components/tag-property-editors/file-or-image-url-tag-property-editor/file-or-image-url-tag-property-editor.component';
import { FolderUrlTagPropertyEditor } from '../../components/tag-property-editors/folder-url-tag-property-editor/folder-url-tag-property-editor.component';
import { FormTagPropertyEditorComponent } from '../../components/tag-property-editors/form-tag-property-editor/form-tag-property-editor.component';
import { ListTagPropertyEditor } from '../../components/tag-property-editors/list-tag-property-editor/list-tag-property-editor.component';
import {
    NodeSelectorTagPropertyEditor,
} from '../../components/tag-property-editors/node-selector-tag-property-editor/node-selector-tag-property-editor.component';
import { OverviewTagPropertyEditor } from '../../components/tag-property-editors/overview-tag-property-editor/overview-tag-property-editor.component';
import { PageUrlTagPropertyEditor } from '../../components/tag-property-editors/page-url-tag-property-editor/page-url-tag-property-editor.component';
import { SelectTagPropertyEditor } from '../../components/tag-property-editors/select-tag-property-editor/select-tag-property-editor.component';
import { TagRefTagPropertyEditor } from '../../components/tag-property-editors/tagref-tag-property-editor/tagref-tag-property-editor.component';
import { TextTagPropertyEditor } from '../../components/tag-property-editors/text-tag-property-editor/text-tag-property-editor.component';

// Maps the TagPartTypes to their TagPropertyEditor components.
const DEFAULT_EDITORS = new Map<TagPartType, Type<TagPropertyEditor>>();

DEFAULT_EDITORS.set(TagPartType.Checkbox, CheckboxTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.DataSource, DataSourceTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.FileUpload, FileOrImageUrlTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.UrlFile, FileOrImageUrlTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.UrlImage, FileOrImageUrlTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.FolderUpload, FolderUrlTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.UrlFolder, FolderUrlTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.List, ListTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.ListOrdered, ListTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.ListUnordered, ListTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.Node, NodeSelectorTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.Overview, OverviewTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.UrlPage, PageUrlTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.SelectMultiple, SelectTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.SelectSingle, SelectTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.Form, FormTagPropertyEditorComponent);
DEFAULT_EDITORS.set(TagPartType.CmsForm, FormTagPropertyEditorComponent);

DEFAULT_EDITORS.set(TagPartType.TagPage, TagRefTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.TagTemplate, TagRefTagPropertyEditor);

DEFAULT_EDITORS.set(TagPartType.Text, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.TextHtml, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.TextHtmlLong, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.TextShort, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.Html, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.HtmlLong, TextTagPropertyEditor);
DEFAULT_EDITORS.set(TagPartType.Handlebars, TextTagPropertyEditor);

/**
 * Looks up the ComponentFactory for the TagPropertyEditor that is configured for
 * a particular TagPartType.
 */
@Injectable()
export class TagPropertyEditorResolverService {

    private componentFactories = new Map<Type<TagPropertyEditor>, ComponentFactory<TagPropertyEditor>>();

    constructor(private componentFactoryResolver: ComponentFactoryResolver) { }

    resolveTagPropertyEditorFactory(tagPart: TagPart): ComponentFactory<TagPropertyEditor> {
        let componentType: Type<TagPropertyEditor>;
        if (!tagPart.externalEditorUrl) {
            componentType = DEFAULT_EDITORS.get(tagPart.typeId);
            if (!componentType) {
                console.error(`No TagPropertyEditor defined for TagPartType ${tagPart.typeId}`);
                return null;
            }
        } else {
            componentType = CustomTagPropertyEditorHostComponent;
        }

        let componentFactory = this.componentFactories.get(componentType);
        if (!componentFactory) {
            componentFactory = this.componentFactoryResolver.resolveComponentFactory(componentType);
            this.componentFactories.set(componentType, componentFactory);
        }
        return componentFactory;
    }

}
