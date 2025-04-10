import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    SelectOption,
    SelectTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';

/**
 * Used to edit Select TagParts.
 */
@Component({
    selector: 'select-tag-property-editor',
    templateUrl: './select-tag-property-editor.component.html',
    styleUrls: ['./select-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectTagPropertyEditor implements TagPropertyEditor {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: SelectTagPartProperty;

    selectedSingleOption: SelectOption;
    selectedSingleOptionId: number | string;
    selectedMultipleOptions: SelectOption[] = [];
    selectedMultipleOptionsIds: (number | string)[] = [];

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

    onSingleSelectChange(newValue: number): void {
        this.updateSelection([newValue]);
    }

    onMultipleSelectChange(newValue: number[]): void {
        this.updateSelection(newValue);
    }

    updateSelection(newValue: number[]): void {
        this.tagProperty.selectedOptions = (newValue || [])
            .sort()
            .map(id => this.tagProperty.options.find(option => option.id === id))
            .filter(val => val != null);

        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.SELECT && newValue.type !== TagPropertyType.MULTISELECT) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by SelectTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;

        if (!this.tagProperty.selectedOptions) {
            this.tagProperty.selectedOptions = [];
        }

        if (newValue.type === TagPropertyType.SELECT) {
            if (this.tagProperty.selectedOptions.length > 0) {
                this.selectedSingleOption = this.tagProperty.selectedOptions[0];
                this.selectedSingleOptionId = this.tagProperty.selectedOptions[0].id;
            } else {
                this.selectedSingleOption = null;
                this.selectedSingleOptionId = null;
            }
        }
        if (newValue.type === TagPropertyType.MULTISELECT) {
            this.selectedMultipleOptions = [];
            this.selectedMultipleOptionsIds = [];

            this.tagProperty.selectedOptions.forEach((value) => {
                this.selectedMultipleOptions.push(value);
                this.selectedMultipleOptionsIds.push(value.id);
            });
        }

        this.changeDetector.markForCheck();
    }
}
