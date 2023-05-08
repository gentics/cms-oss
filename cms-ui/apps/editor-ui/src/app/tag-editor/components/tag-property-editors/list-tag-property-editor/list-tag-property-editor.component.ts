import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import {
    EditableTag,
    ListTagPartProperty,
    OrderedUnorderedListTagPartProperty,
    TagEditorContext,
    TagEditorError,
    TagPart,
    TagPartProperty,
    TagPropertiesChangedFn,
    TagPropertyEditor,
    TagPropertyMap,
    TagPropertyType
} from '@gentics/cms-models';
import { Observable, Subject, Subscription } from 'rxjs';

/**
 * Used to edit the following TagParts:
 * - List
 * - ListOrdered
 * - ListUnordered
 */
@Component({
    selector: 'list-tag-property-editor',
    templateUrl: './list-tag-property-editor.component.html',
    styleUrls: ['./list-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ListTagPropertyEditor implements OnInit, OnDestroy, TagPropertyEditor {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: ListTagPartProperty | OrderedUnorderedListTagPartProperty;

    /**
     * The list items joined with \n to one string.
     */
    stringifiedList = '';

    /**
     * Only applies to TagPropertyType.LIST.
     * If true, the list should be numbered, otherwise bulleted.
     */
    isNumberedListUserSelection: boolean;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** Used to check the TagPropertyType in the template. */
    tagPropertyTypeEnum = TagPropertyType;

    /** Used to debounce the textarea changes. */
    listChange = new Subject<void>();
    listBlur = new Subject<void>();
    private subscriptions = new Subscription();

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    constructor(private changeDetector: ChangeDetectorRef) { }

    ngOnInit(): void {
        const debouncer = this.listChange.debounceTime(100);
        const blurOrDebouncedChange = Observable.merge(this.listBlur, debouncer);
        this.subscriptions.add(
            blurOrDebouncedChange.subscribe(() => this.onUserChange())
        );
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

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
     * Updates the tagProperty with changes made by the user and calls onChangeFn().
     */
    onUserChange(): void {
        // This is necessary, because many GUIC components update their ngModels after firing their change event.
        setTimeout(() => {
            if (this.stringifiedList) {
                this.tagProperty.stringValues = this.stringifiedList.split('\n');
            } else {
                this.tagProperty.stringValues = [];
            }

            if (this.tagProperty.type === TagPropertyType.LIST) {
                this.tagProperty.booleanValue = this.isNumberedListUserSelection;
            }

            if (this.onChangeFn) {
                const changes: Partial<TagPropertyMap> = {};
                changes[this.tagPart.keyword] = this.tagProperty;
                this.onChangeFn(changes);
            }
        });
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.LIST && newValue.type !== TagPropertyType.ORDEREDLIST && newValue.type !== TagPropertyType.UNORDEREDLIST) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by ListTagPropertyEditor.`);
        }

        this.tagProperty = newValue as (ListTagPartProperty | OrderedUnorderedListTagPartProperty);
        this.stringifiedList = this.tagProperty.stringValues.join('\n');
        if (this.tagProperty.type === TagPropertyType.LIST) {
            this.isNumberedListUserSelection = this.tagProperty.booleanValue;
        }
        this.changeDetector.markForCheck();
    }

}
