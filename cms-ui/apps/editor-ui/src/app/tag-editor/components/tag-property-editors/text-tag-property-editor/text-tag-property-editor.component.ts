import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor, ValidationResult } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    StringTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPartType,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import { BehaviorSubject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, skip } from 'rxjs/operators';

/**
 * Used to edit Text TagParts.
 */
@Component({
    selector: 'text-tag-property-editor',
    templateUrl: './text-tag-property-editor.component.html',
    styleUrls: ['./text-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TextTagPropertyEditor implements TagPropertyEditor, OnInit, OnDestroy {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: StringTagPartProperty;

    /** Used to determine if we need to show a one line input or a multiline text area. */
    isTextShort: boolean;

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** Used for displaying a validation error to the user. */
    displayedValidationResult: ValidationResult;

    /** Used to debounce the input/textarea changes. */
    private inputChange = new BehaviorSubject<string>('');
    private blur = new BehaviorSubject<void>(null);
    private subscriptions: Subscription[] = [];

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;
    private lastValidationResult: ValidationResult;

    constructor(
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnInit(): void {
        const tmp$ = new BehaviorSubject<void>(null);

        this.subscriptions.push(this.inputChange.asObservable().pipe(
            skip(1),
            distinctUntilChanged(isEqual),
        ).subscribe(newValue => {
            this.tagProperty.stringValue = newValue;
            tmp$.next();
        }));

        this.subscriptions.push(this.blur.asObservable().pipe(
            skip(1),
        ).subscribe(() => {
            tmp$.next();
        }));

        this.subscriptions.push(tmp$.asObservable().pipe(
            skip(1),
            debounceTime(100),
        ).subscribe(() => {
            this.processChange();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.isTextShort = tagPart.typeId === TagPartType.TextShort;
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

    onChange(newValue: string): void {
        if (typeof newValue === 'string') {
            this.inputChange.next(newValue);
        }
    }

    onBlur(): void {
        this.blur.next();
        this.displayedValidationResult = this.lastValidationResult;
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.STRING && newValue.type !== TagPropertyType.RICHTEXT) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by TextTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        this.changeDetector.markForCheck();
    }

    /**
     * Signals the change to the TagEditor and also validates the change if the
     * last validation was unsuccessful.
     */
    private processChange(): void {
        if (this.onChangeFn) {
            // Signal only the changed tag properties.
            const changes: Partial<TagPropertyMap> = {};
            changes[this.tagPart.keyword] = this.tagProperty;
            const validationResults = this.onChangeFn(changes);
            this.lastValidationResult = validationResults[this.tagPart.keyword];
        }

        if (this.displayedValidationResult && !this.displayedValidationResult.success && this.lastValidationResult.success) {
            this.displayedValidationResult = this.lastValidationResult;
            this.changeDetector.markForCheck();
        }
    }

}
