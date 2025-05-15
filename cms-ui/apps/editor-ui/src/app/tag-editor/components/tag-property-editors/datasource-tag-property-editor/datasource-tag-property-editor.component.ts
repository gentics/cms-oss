import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    DataSourceTagPartProperty,
    EditableTag,
    SelectOption,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';

/**
 * Used to edit DataSource TagParts.
 */
@Component({
    selector: 'datasource-tag-property-editor',
    templateUrl: './datasource-tag-property-editor.component.html',
    styleUrls: ['./datasource-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DataSourceTagPropertyEditor implements TagPropertyEditor {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: DataSourceTagPartProperty;

    /** The key/value bound to the input fields for new options. */
    newOptionKey: string;
    newOptionValue: string | number;

    /** IDs for new options are always incremented. They are not changed when the list is reordered or when something is deleted. */
    newOptionId: number;

    /** The current options in the dataSource. */
    currentOptions: SelectOption[];

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

    /**
     * Handles clicks on the Add Option button.
     */
    onAddOptionClick(): void {
        const newOption: SelectOption = {
            id: this.newOptionId,
            key: this.newOptionKey,
            value: this.newOptionValue,
        };
        ++this.newOptionId;
        this.currentOptions = [ ...this.currentOptions, newOption ];
        this.newOptionKey = '';
        this.newOptionValue = '';

        this.onUserChange();
        this.changeDetector.markForCheck();
    }

    /**
     * Called by the blur event of the inputs for editing existing options.
     */
    onModifyOption(modifiedOption: SelectOption, index: number): void {
        const origOption = this.tagProperty.options[index];

        // If any of the changes is invalid, revert it.
        if (!modifiedOption.key) {
            modifiedOption.key = origOption.key;
        }
        if (!modifiedOption.value) {
            modifiedOption.value = origOption.value;
        }

        if (modifiedOption.key !== origOption.key || modifiedOption.value !== origOption.value) {
            this.onUserChange();
        }
        this.changeDetector.markForCheck();
    }

    /**
     * Updates the tagProperty with changes made by the user and calls onChangeFn().
     */
    onUserChange(): void {
        this.tagProperty.options = cloneDeep(this.currentOptions);

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
        if (newValue.type !== TagPropertyType.DATASOURCE) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by DataSourceTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        this.currentOptions = cloneDeep(this.tagProperty.options) || [];
        this.newOptionId = this.findHighestId(this.currentOptions) + 1;
        this.changeDetector.markForCheck();
    }

    /** Finds the highest ID in the array of options. */
    private findHighestId(options: SelectOption[]): number {
        if (options.some(o => typeof o.id === 'string')) {
            throw new Error('Property "id" is of type string and therefore cannot be compared.');
        }
        if (options && options.length > 0) {
            const optionWithHighestId = options.reduce((opWithHighestId, currOption) => {
                if (!opWithHighestId || currOption.id > opWithHighestId.id) {
                    return currOption;
                } else {
                    return opWithHighestId;
                }
            });
            return optionWithHighestId.id as number;
        }
        return -1;
    }

}
