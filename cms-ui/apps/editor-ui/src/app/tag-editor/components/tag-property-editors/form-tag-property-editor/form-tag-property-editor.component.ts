import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    CmsFormTagPartProperty,
    EditableTag,
    FormInNode,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { SelectedItemHelper } from '../../../../shared/util/selected-item-helper/selected-item-helper';

/**
 * Used to insert forms created with Editor UI Form Generator.
 */
@Component({
    selector: 'form-tag-property-editor',
    templateUrl: './form-tag-property-editor.component.html',
    styleUrls: ['./form-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class FormTagPropertyEditorComponent implements TagPropertyEditor {

    /** The helper for managing and loading the selected internal form. */
    public selectedInternalForm: SelectedItemHelper<FormInNode>;

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: CmsFormTagPartProperty;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    constructor(
        private client: GCMSRestClientService,
        private changeDetector: ChangeDetectorRef,
    ) { }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.selectedInternalForm = new SelectedItemHelper('form', context.node.id, this.client);
        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.updateTagProperty(tagProperty);
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    /**
     * Changes the values of this.tagProperty and this.selectedInternalForm$ according
     * to newSelectedForm. This method must only be called in response to
     * user input.
     */
    changeSelectedForm(newSelectedForm: FormInNode): void {
        let selectedInternalForm: FormInNode;

        if (typeof newSelectedForm === 'string') {
            // Invalid, shouldn't happen
            this.tagProperty.formId = 0;
            selectedInternalForm = null;
        } else {
            if (newSelectedForm) {
                this.tagProperty.formId = newSelectedForm.id;
            } else {
                this.tagProperty.formId = 0;
            }

            selectedInternalForm = newSelectedForm;
        }

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.selectedInternalForm.setSelectedItem(selectedInternalForm);
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.CMSFORM) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by FormUrlTagPropertyEditor.`);
        }
        this.tagProperty = newValue;

        this.selectedInternalForm.setSelectedItem(this.tagProperty.formId || null);

        // This seems to be necessary to make the radio buttons react correctly.
        this.changeDetector.detectChanges();
        setTimeout(() => this.changeDetector.detectChanges());
    }

}
