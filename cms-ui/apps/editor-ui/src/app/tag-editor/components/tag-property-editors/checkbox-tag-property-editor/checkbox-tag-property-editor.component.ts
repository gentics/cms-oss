import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import {
    BooleanTagPartProperty,
    EditableTag,
    TagEditorContext,
    TagEditorError,
    TagPart,
    TagPartProperty,
    TagPropertiesChangedFn,
    TagPropertyEditor,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';

/**
 * Used to edit Checkbox TagParts.
 */
@Component({
    selector: 'checkbox-tag-property-editor',
    templateUrl: './checkbox-tag-property-editor.component.html',
    styleUrls: ['./checkbox-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckboxTagPropertyEditor implements TagPropertyEditor {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: BooleanTagPartProperty;

    /** Stores tagProperty.booleanValue for ngModel binding. */
    isChecked: boolean;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    constructor(private changeDetector: ChangeDetectorRef) { }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
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

    onCheckboxChange(): void {
        if (this.onChangeFn && this.tagProperty.booleanValue !== this.isChecked) {
            const changes: Partial<TagPropertyMap> = {};
            this.tagProperty.booleanValue = this.isChecked;
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.BOOLEAN) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by CheckboxTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        this.isChecked = this.tagProperty.booleanValue;
        this.changeDetector.markForCheck();
    }

}
